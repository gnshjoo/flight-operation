package com.koreanair.ops.flight.service

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Component
class OpenSkyClient(
    @Value("\${app.opensky.username:}") private val username: String,
    @Value("\${app.opensky.password:}") private val password: String,
    @Value("\${app.opensky.base-url:https://opensky-network.org}") private val baseUrl: String,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val cache = ConcurrentHashMap<String, OpenSkyState>()
    private var lastFetchTime: Instant = Instant.EPOCH
    private val cacheTtl = Duration.ofMinutes(5)

    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    private val regions = listOf(
        "lamin=10&lamax=55&lomin=90&lomax=155",
        "lamin=20&lamax=65&lomin=-140&lomax=-50",
        "lamin=30&lamax=65&lomin=-15&lomax=55",
    )

    fun isEnabled(): Boolean = true

    fun getAll(): List<OpenSkyState> {
        fetchIfStale()
        return cache.values.toList()
    }

    fun getInFlight(): List<OpenSkyState> {
        fetchIfStale()
        return cache.values.filter { !it.onGround }
    }

    fun getOnGround(): List<OpenSkyState> {
        fetchIfStale()
        return cache.values.filter { it.onGround }
    }

    fun getStats(): OpenSkyStats {
        fetchIfStale()
        val all = cache.values.toList()
        return OpenSkyStats(
            total = all.size,
            inFlight = all.count { !it.onGround },
            onGround = all.count { it.onGround },
            source = if (isEnabled() && all.isNotEmpty()) "opensky" else "unavailable"
        )
    }

    @Synchronized
    private fun fetchIfStale() {
        if (!isEnabled()) return
        if (Duration.between(lastFetchTime, Instant.now()) < cacheTtl) return
        lastFetchTime = Instant.now()

        val credentials = Base64.getEncoder().encodeToString("$username:$password".toByteArray())
        val found = ConcurrentHashMap<String, OpenSkyState>()

        for ((index, bbox) in regions.withIndex()) {
            try {
                val url = "$baseUrl/api/states/all?$bbox"
                val request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Basic $credentials")
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build()

                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

                if (response.statusCode() != 200) {
                    log.warn("OpenSky region {} returned HTTP {}", index, response.statusCode())
                    continue
                }

                val body = response.body()
                if (body.isNullOrBlank()) continue

                val parsed = objectMapper.readValue(body, OpenSkyResponse::class.java)
                val states = parsed.states ?: emptyList()

                states.forEach { arr ->
                    if (arr.size >= 14) {
                        val icao24 = (arr[0] as? String)?.trim() ?: ""
                        val callsign = (arr[1] as? String)?.trim() ?: return@forEach
                        if (callsign.startsWith("KAL")) {
                            val lat = toDouble(arr[6])
                            val lon = toDouble(arr[5])
                            if (lat != null && lon != null) {
                                val route = KE_ROUTES[callsign]
                                found[callsign] = OpenSkyState(
                                    icao24 = icao24,
                                    callsign = callsign,
                                    latitude = lat,
                                    longitude = lon,
                                    altitude = toDouble(arr[7]) ?: 0.0,
                                    velocity = toDouble(arr[9]) ?: 0.0,
                                    heading = toDouble(arr[10]) ?: 0.0,
                                    onGround = arr[8] as? Boolean ?: false,
                                    departureAirport = route?.first,
                                    arrivalAirport = route?.second
                                )
                            }
                        }
                    }
                }

                log.debug("OpenSky region {}: {} total, {} KAL", index, states.size, found.size)
            } catch (e: Exception) {
                log.warn("OpenSky region {} failed: {} ({})", index, e.message, e.cause?.message ?: "no cause")
            }
        }

        if (found.isNotEmpty()) {
            cache.clear()
            cache.putAll(found)
            log.info("OpenSky: {} Korean Air aircraft tracked ({} in flight, {} on ground)",
                cache.size, cache.values.count { !it.onGround }, cache.values.count { it.onGround })
        } else if (cache.isEmpty()) {
            log.warn("OpenSky: no Korean Air aircraft found in any region")
        }
    }

    private fun toDouble(value: Any?): Double? = when (value) {
        is Number -> value.toDouble()
        is String -> value.toDoubleOrNull()
        else -> null
    }

    companion object {
        val KE_ROUTES: Map<String, Pair<String, String>> = mapOf(
            "KAL1" to ("ICN" to "LAX"), "KAL2" to ("LAX" to "ICN"),
            "KAL5" to ("ICN" to "LAX"), "KAL6" to ("LAX" to "ICN"),
            "KAL11" to ("ICN" to "HND"), "KAL12" to ("HND" to "ICN"),
            "KAL17" to ("ICN" to "NRT"), "KAL18" to ("NRT" to "ICN"),
            "KAL23" to ("ICN" to "CDG"), "KAL24" to ("CDG" to "ICN"),
            "KAL25" to ("ICN" to "CDG"), "KAL26" to ("CDG" to "ICN"),
            "KAL31" to ("ICN" to "JFK"), "KAL32" to ("JFK" to "ICN"),
            "KAL33" to ("ICN" to "JFK"), "KAL34" to ("JFK" to "ICN"),
            "KAL37" to ("ICN" to "SFO"), "KAL38" to ("SFO" to "ICN"),
            "KAL41" to ("ICN" to "ORD"), "KAL42" to ("ORD" to "ICN"),
            "KAL51" to ("ICN" to "NRT"), "KAL52" to ("NRT" to "ICN"),
            "KAL61" to ("ICN" to "HNL"), "KAL62" to ("HNL" to "ICN"),
            "KAL73" to ("ICN" to "SIN"), "KAL74" to ("SIN" to "ICN"),
            "KAL85" to ("ICN" to "FRA"), "KAL86" to ("FRA" to "ICN"),
            "KAL91" to ("ICN" to "SYD"), "KAL92" to ("SYD" to "ICN"),
            "KAL101" to ("ICN" to "SEA"), "KAL102" to ("SEA" to "ICN"),
            "KAL107" to ("ICN" to "IAD"), "KAL108" to ("IAD" to "ICN"),
            "KAL131" to ("ICN" to "BKK"), "KAL132" to ("BKK" to "ICN"),
            "KAL161" to ("ICN" to "PVG"), "KAL162" to ("PVG" to "ICN"),
            "KAL163" to ("ICN" to "PEK"), "KAL164" to ("PEK" to "ICN"),
            "KAL166" to ("ICN" to "TPE"), "KAL167" to ("TPE" to "ICN"),
            "KAL181" to ("ICN" to "CAN"), "KAL182" to ("CAN" to "ICN"),
            "KAL189" to ("ICN" to "HKG"), "KAL190" to ("HKG" to "ICN"),
            "KAL301" to ("ICN" to "MNL"), "KAL302" to ("MNL" to "ICN"),
            "KAL351" to ("ICN" to "SGN"), "KAL352" to ("SGN" to "ICN"),
            "KAL361" to ("ICN" to "KUL"), "KAL362" to ("KUL" to "ICN"),
            "KAL364" to ("ICN" to "HAN"), "KAL365" to ("HAN" to "ICN"),
            "KAL601" to ("ICN" to "LHR"), "KAL602" to ("LHR" to "ICN"),
            "KAL621" to ("ICN" to "FCO"), "KAL622" to ("FCO" to "ICN"),
            "KAL631" to ("ICN" to "MAD"), "KAL632" to ("MAD" to "ICN"),
            "KAL641" to ("ICN" to "AMS"), "KAL642" to ("AMS" to "ICN"),
            "KAL643" to ("ICN" to "BCN"), "KAL644" to ("BCN" to "ICN"),
            "KAL651" to ("ICN" to "IST"), "KAL652" to ("IST" to "ICN"),
            "KAL701" to ("ICN" to "GUM"), "KAL702" to ("GUM" to "ICN"),
            "KAL801" to ("ICN" to "CTS"), "KAL802" to ("CTS" to "ICN"),
            "KAL811" to ("ICN" to "KIX"), "KAL812" to ("KIX" to "ICN"),
            "KAL831" to ("ICN" to "FUK"), "KAL832" to ("FUK" to "ICN"),
            "KAL851" to ("ICN" to "NGO"), "KAL852" to ("NGO" to "ICN"),
            "KAL901" to ("GMP" to "CJU"), "KAL902" to ("CJU" to "GMP"),
        )
    }
}

data class OpenSkyState(
    val icao24: String = "",
    val callsign: String,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val velocity: Double,
    val heading: Double,
    val onGround: Boolean,
    val departureAirport: String? = null,
    val arrivalAirport: String? = null
)

data class OpenSkyStats(
    val total: Int,
    val inFlight: Int,
    val onGround: Int,
    val source: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenSkyResponse(
    val time: Long? = null,
    val states: List<List<Any?>>? = null
)
