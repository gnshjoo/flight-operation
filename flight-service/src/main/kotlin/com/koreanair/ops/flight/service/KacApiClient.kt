package com.koreanair.ops.flight.service

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

@Component
class KacApiClient(
    @Value("\${app.kac.service-key:}") private val serviceKey: String,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val client = RestClient.create()
    private val cache = ConcurrentHashMap<String, KacFlightStatus>()
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

    companion object {
        const val BASE_URL = "https://api.odcloud.kr/api/FlightStatusListDTL/v1/getFlightStatusListDetail"
        const val PER_PAGE = 100
    }

    fun isEnabled(): Boolean = serviceKey.isNotBlank()

    fun getAll(): List<KacFlightStatus> = cache.values.toList()

    fun getKoreanAirOnly(): List<KacFlightStatus> =
        cache.values.filter { it.airFln?.startsWith("KE") == true }
            .sortedBy { it.std }

    fun getStats(): KacStats {
        val keFlights = getKoreanAirOnly()
        return KacStats(
            total = cache.size,
            koreanAir = keFlights.size,
            departed = keFlights.count { it.rmkEng.orEmpty().contains("DEPART", true) },
            arrived = keFlights.count { it.rmkEng.orEmpty().contains("ARRIV", true) },
            delayed = keFlights.count { it.rmkEng.orEmpty().contains("DELAY", true) },
            cancelled = keFlights.count { it.rmkEng.orEmpty().contains("CANCEL", true) },
            boarding = keFlights.count {
                val rmk = it.rmkKor ?: ""
                rmk.contains("수속") || rmk.contains("탑승")
            },
            source = if (isEnabled() && keFlights.isNotEmpty()) "kac" else "unavailable"
        )
    }

    @Scheduled(initialDelay = 3000, fixedRate = 300_000)
    fun pollFlightStatus() {
        if (!isEnabled()) return

        val today = LocalDate.now().format(dateFormatter)
        log.info("KAC: Polling flights for {}...", today)

        try {
            // 1. Get total count
            val first = fetchPage(1) ?: return
            val totalPages = ((first.totalCount ?: 0) + PER_PAGE - 1) / PER_PAGE
            if (totalPages == 0) return

            // 2. Binary search for the first page containing today's date
            val startPage = binarySearchDate(today, 1, totalPages)
            if (startPage == -1) {
                log.warn("KAC: No data found for {}", today)
                return
            }

            // 3. Collect all today's flights from startPage forward
            val todayFlights = mutableListOf<KacFlightStatus>()
            for (page in startPage..minOf(startPage + 30, totalPages)) {
                val result = fetchPage(page) ?: break
                val flights = result.data ?: break
                val todayOnPage = flights.filter { it.flightDate == today }
                todayFlights.addAll(todayOnPage)
                // Stop if we've moved past today
                if (flights.any { (it.flightDate ?: "") > today } && todayOnPage.isEmpty()) break
            }

            cache.clear()
            todayFlights.forEach { f ->
                cache[f.ufid ?: "${f.airFln}-${f.std}-${f.io}"] = f
            }

            val keCount = cache.values.count { it.airFln?.startsWith("KE") == true }
            log.info("KAC: {} flights today, {} Korean Air", cache.size, keCount)
        } catch (e: Exception) {
            log.warn("KAC: Poll failed: {}", e.message)
        }
    }

    /**
     * Binary search for the first page containing the target date.
     * Data is chronologically ordered.
     */
    private fun binarySearchDate(targetDate: String, low: Int, high: Int): Int {
        var lo = low
        var hi = high
        var result = -1

        while (lo <= hi) {
            val mid = lo + (hi - lo) / 2
            val page = fetchPage(mid) ?: break
            val dates = page.data?.mapNotNull { it.flightDate }?.toSet() ?: break

            when {
                dates.contains(targetDate) -> {
                    result = mid
                    hi = mid - 1 // Keep searching left for the first page
                }
                dates.all { it < targetDate } -> lo = mid + 1
                else -> hi = mid - 1
            }
        }
        return result
    }

    private fun fetchPage(page: Int): KacApiResponse? {
        return try {
            val url = "$BASE_URL?page=$page&perPage=$PER_PAGE&serviceKey=$serviceKey"
            val body = client.get()
                .uri(url)
                .header("Authorization", serviceKey)
                .retrieve()
                .body(String::class.java) ?: return null
            objectMapper.readValue(body, KacApiResponse::class.java)
        } catch (e: Exception) {
            null
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class KacApiResponse(
    val currentCount: Int? = null,
    val totalCount: Int? = null,
    val data: List<KacFlightStatus>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class KacFlightStatus(
    @JsonProperty("AIR_FLN") val airFln: String? = null,
    @JsonProperty("AIRLINE_KOREAN") val airlineKorean: String? = null,
    @JsonProperty("AIRLINE_ENGLISH") val airlineEnglish: String? = null,
    @JsonProperty("AIRPORT") val airport: String? = null,
    @JsonProperty("BOARDING_KOR") val boardingKor: String? = null,
    @JsonProperty("BOARDING_ENG") val boardingEng: String? = null,
    @JsonProperty("ARRIVED_KOR") val arrivedKor: String? = null,
    @JsonProperty("ARRIVED_ENG") val arrivedEng: String? = null,
    @JsonProperty("CITY") val city: String? = null,
    @JsonProperty("STD") val std: String? = null,
    @JsonProperty("ETD") val etd: String? = null,
    @JsonProperty("IO") val io: String? = null,
    @JsonProperty("LINE") val line: String? = null,
    @JsonProperty("LINE_CODE") val lineCode: String? = null,
    @JsonProperty("RMK_KOR") val rmkKor: String? = null,
    @JsonProperty("RMK_ENG") val rmkEng: String? = null,
    @JsonProperty("GATE") val gate: String? = null,
    @JsonProperty("BAGGAGE_CLAIM") val baggageClaim: String? = null,
    @JsonProperty("FLIGHT_DATE") val flightDate: String? = null,
    @JsonProperty("UFID") val ufid: String? = null
)

data class KacStats(
    val total: Int,
    val koreanAir: Int,
    val departed: Int,
    val arrived: Int,
    val delayed: Int,
    val cancelled: Int,
    val boarding: Int = 0,
    val source: String
)
