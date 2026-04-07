package com.koreanair.ops.flight.service

import com.koreanair.ops.flight.dto.FlightPosition
import com.koreanair.ops.flight.dto.FlightWithPositionResponse
import com.koreanair.ops.flight.dto.FlightResponse
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.util.concurrent.ConcurrentHashMap

@Component
class OpenSkyClient(
    @Value("\${app.opensky.username:}") private val username: String,
    @Value("\${app.opensky.password:}") private val password: String,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val client = RestClient.create()
    private val cache = ConcurrentHashMap<String, OpenSkyState>()

    fun isEnabled(): Boolean = username.isNotBlank() && password.isNotBlank()

    fun getKoreanAirPositions(): Map<String, OpenSkyState> = cache.toMap()

    @Scheduled(fixedRate = 300_000) // 5 minutes
    fun pollOpenSky() {
        if (!isEnabled()) return

        try {
            val url = "https://opensky-network.org/api/states/all?operator_icao=KAL"
            val response = client.get()
                .uri(url)
                .headers { it.setBasicAuth(username, password) }
                .retrieve()
                .body(String::class.java)

            if (response.isNullOrBlank()) {
                log.warn("OpenSky returned empty response")
                return
            }

            val parsed = objectMapper.readValue(response, OpenSkyResponse::class.java)
            val states = parsed.states ?: emptyList()

            cache.clear()
            states.forEach { arr ->
                if (arr.size >= 14) {
                    val callsign = (arr[1] as? String)?.trim() ?: return@forEach
                    if (callsign.startsWith("KAL")) {
                        val lat = toDouble(arr[6])
                        val lon = toDouble(arr[5])
                        if (lat != null && lon != null) {
                            cache[callsign] = OpenSkyState(
                                callsign = callsign,
                                latitude = lat,
                                longitude = lon,
                                altitude = toDouble(arr[7]) ?: 0.0,
                                velocity = toDouble(arr[9]) ?: 0.0,
                                heading = toDouble(arr[10]) ?: 0.0,
                                onGround = arr[8] as? Boolean ?: false
                            )
                        }
                    }
                }
            }

            log.info("OpenSky: {} Korean Air aircraft tracked", cache.size)
        } catch (e: Exception) {
            log.warn("OpenSky poll failed: {}. Using cached data ({} entries)", e.message, cache.size)
        }
    }

    private fun toDouble(value: Any?): Double? = when (value) {
        is Number -> value.toDouble()
        is String -> value.toDoubleOrNull()
        else -> null
    }
}

data class OpenSkyState(
    val callsign: String,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val velocity: Double,
    val heading: Double,
    val onGround: Boolean
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenSkyResponse(
    val time: Long? = null,
    val states: List<List<Any?>>? = null
)
