package com.koreanair.ops.flight.service

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.util.concurrent.ConcurrentHashMap

/**
 * 한국공항공사 항공기운항정보 OpenAPI 클라이언트
 *
 * APIs:
 *   - getFlightStatusList: 실시간 운항 현황 (편명, 출발/도착, 상태, 시간)
 *   - getIflightScheduleList: 국제선 스케줄
 *   - getDflightScheduleList: 국내선 스케줄
 *   - getAirportCodeList: 공항코드
 *
 * 관리 공항: GMP(김포), PUS(부산), CJU(제주), TAE(대구), KWJ(광주), RSU(여수), USN(울산), MWX(무안), HIN(사천), WJU(원주), YNY(양양), CJJ(청주), KUV(군산), KPO(포항)
 * 주의: ICN(인천)은 인천국제공항공사 관할, 이 API에 없음
 */
@Component
class KacApiClient(
    @Value("\${app.kac.service-key:}") private val serviceKey: String
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val client = RestClient.create()
    private val xmlMapper = XmlMapper().registerKotlinModule()
    private val cache = ConcurrentHashMap<String, KacFlightStatus>()

    companion object {
        const val BASE_URL = "http://openapi.airport.co.kr/service/rest"
        val KAC_AIRPORTS = listOf("GMP", "PUS", "CJU", "TAE", "KWJ", "RSU", "USN", "MWX", "HIN", "WJU", "YNY", "CJJ")
    }

    fun isEnabled(): Boolean = serviceKey.isNotBlank()

    fun getAll(): List<KacFlightStatus> = cache.values.toList()

    fun getKoreanAirOnly(): List<KacFlightStatus> =
        cache.values.filter { it.airFln?.startsWith("KE") == true }

    fun getStats(): KacStats {
        val keFlights = getKoreanAirOnly()
        return KacStats(
            total = cache.size,
            koreanAir = keFlights.size,
            departed = keFlights.count { it.rmkEng?.contains("DEPART", true) == true },
            arrived = keFlights.count { it.rmkEng?.contains("ARRIV", true) == true },
            delayed = keFlights.count { it.rmkEng?.contains("DELAY", true) == true },
            cancelled = keFlights.count { it.rmkEng?.contains("CANCEL", true) == true },
            source = if (isEnabled() && cache.isNotEmpty()) "kac" else "unavailable"
        )
    }

    @Scheduled(initialDelay = 5000, fixedRate = 300_000) // 5 sec delay, then every 5 min
    fun pollFlightStatus() {
        if (!isEnabled()) return

        log.info("KAC: Polling flight status for Korean Air...")
        var totalLoaded = 0

        // Poll major airports for both departures and arrivals
        for (airport in listOf("GMP", "PUS", "CJU")) {
            for (ioType in listOf("O", "I")) { // O=departure, I=arrival
                for (lineType in listOf("D", "I")) { // D=domestic, I=international
                    try {
                        val flights = fetchFlightStatus(airport, ioType, lineType)
                        flights.forEach { flight ->
                            val key = "${flight.airFln}-${flight.std}-${flight.io}"
                            cache[key] = flight
                        }
                        totalLoaded += flights.size
                    } catch (e: Exception) {
                        log.warn("KAC: Failed to poll {} {} {}: {}", airport, ioType, lineType, e.message)
                    }
                }
            }
        }

        // Remove non-KE flights to keep cache focused
        cache.entries.removeIf { it.value.airFln?.startsWith("KE") != true }

        log.info("KAC: {} total flights loaded, {} Korean Air flights cached", totalLoaded, cache.size)
    }

    private fun fetchFlightStatus(
        airportCode: String,
        ioType: String, // I=arrival, O=departure
        lineType: String // D=domestic, I=international
    ): List<KacFlightStatus> {
        val url = "$BASE_URL/FlightStatusList/getFlightStatusList" +
                "?ServiceKey=$serviceKey" +
                "&schAirCode=$airportCode" +
                "&schIOType=$ioType" +
                "&schLineType=$lineType" +
                "&pageNo=1" +
                "&numOfRows=100"

        val xml = client.get()
            .uri(url)
            .retrieve()
            .body(String::class.java) ?: return emptyList()

        return parseFlightStatusXml(xml)
    }

    fun parseFlightStatusXml(xml: String): List<KacFlightStatus> {
        return try {
            val response = xmlMapper.readValue(xml, KacResponse::class.java)
            if (response.header?.resultCode != "00") {
                log.warn("KAC API error: {} {}", response.header?.resultCode, response.header?.resultMsg)
                return emptyList()
            }
            response.body?.items?.item ?: emptyList()
        } catch (e: Exception) {
            log.warn("KAC XML parse error: {}", e.message)
            emptyList()
        }
    }
}

// XML Response models
@JacksonXmlRootElement(localName = "response")
data class KacResponse(
    @JacksonXmlProperty(localName = "header")
    val header: KacHeader? = null,
    @JacksonXmlProperty(localName = "body")
    val body: KacBody? = null
)

data class KacHeader(
    val resultCode: String? = null,
    val resultMsg: String? = null
)

data class KacBody(
    val items: KacItems? = null,
    val numOfRows: Int? = null,
    val pageNo: Int? = null,
    val totalCount: Int? = null
)

data class KacItems(
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "item")
    val item: List<KacFlightStatus>? = null
)

data class KacFlightStatus(
    val airFln: String? = null,         // 항공편명 (KE1131)
    val airlineKorean: String? = null,  // 항공사 국문 (대한항공)
    val airlineEnglish: String? = null, // 항공사 영문 (Korean Air)
    val airport: String? = null,        // 기준공항코드 (GMP)
    val boardingKor: String? = null,    // 출발공항 국문
    val boardingEng: String? = null,    // 출발공항 영문
    val arrivedKor: String? = null,     // 도착공항 국문
    val arrivedEng: String? = null,     // 도착공항 영문
    val city: String? = null,           // 운항구간코드 (PUS)
    val std: String? = null,            // 예정시간 (0625)
    val etd: String? = null,            // 변경시간 (0640)
    val io: String? = null,             // 출/도착 (I=도착, O=출발)
    val line: String? = null,           // 국내/국제
    val rmkKor: String? = null,         // 상태 국문 (출발, 도착, 지연, 결항)
    val rmkEng: String? = null          // 상태 영문 (DEPARTED, ARRIVED, DELAYED)
)

data class KacStats(
    val total: Int,
    val koreanAir: Int,
    val departed: Int,
    val arrived: Int,
    val delayed: Int,
    val cancelled: Int,
    val source: String
)
