package com.koreanair.ops.flight.controller

import com.koreanair.ops.flight.dto.*
import com.koreanair.ops.flight.model.FlightStatus
import com.koreanair.ops.flight.service.FlightService
import com.koreanair.ops.flight.service.KacApiClient
import com.koreanair.ops.flight.service.OpenSkyClient
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/flights")
@Tag(name = "Flights", description = "Flight management operations")
class FlightController(
    private val flightService: FlightService,
    private val openSkyClient: OpenSkyClient,
    private val kacApiClient: KacApiClient
) {

    @GetMapping
    @Operation(summary = "List all flights with optional filters")
    fun getAllFlights(
        @RequestParam(required = false) airport: String?,
        @RequestParam(required = false) status: FlightStatus?
    ): List<FlightResponse> = flightService.getAllFlights(airport, status)

    @GetMapping("/{id}")
    @Operation(summary = "Get flight details by ID")
    fun getFlightById(@PathVariable id: Long): FlightResponse =
        flightService.getFlightById(id)

    @GetMapping("/live-tracking")
    @Operation(summary = "Get in-flight aircraft with positions (mock data)")
    fun getLiveTracking(): List<FlightWithPositionResponse> =
        flightService.getLiveTracking()

    @GetMapping("/opensky")
    @Operation(summary = "All Korean Air aircraft from OpenSky (real data, 5min poll)")
    fun getOpenSkyAll(): Map<String, Any> {
        val stats = openSkyClient.getStats()
        return mapOf(
            "source" to stats.source,
            "stats" to stats,
            "aircraft" to openSkyClient.getAll()
        )
    }

    @GetMapping("/opensky/stats")
    @Operation(summary = "Korean Air fleet stats from OpenSky")
    fun getOpenSkyStats() = openSkyClient.getStats()

    @GetMapping("/kac")
    @Operation(summary = "Korean Air flights from KAC API (한국공항공사 실시간 현황)")
    fun getKacFlights(): Map<String, Any> {
        val stats = kacApiClient.getStats()
        return mapOf(
            "source" to stats.source,
            "stats" to stats,
            "flights" to kacApiClient.getKoreanAirOnly()
        )
    }

    @GetMapping("/kac/stats")
    @Operation(summary = "KAC flight stats")
    fun getKacStats() = kacApiClient.getStats()

    @GetMapping("/combined")
    @Operation(summary = "Combined view: KAC schedule + OpenSky positions for Korean Air")
    fun getCombinedFlights(): Map<String, Any> {
        val kacFlights = kacApiClient.getKoreanAirOnly()
        val openskyPositions = openSkyClient.getAll().associateBy {
            it.callsign.replace("KAL", "KE")
        }

        val combined = kacFlights.map { kac ->
            val flightNum = kac.airFln ?: ""
            val position = openskyPositions[flightNum]
            mapOf(
                "flightNumber" to flightNum,
                "airline" to (kac.airlineEnglish ?: "Korean Air"),
                "departureAirport" to (kac.boardingEng ?: "-"),
                "arrivalAirport" to (kac.arrivedEng ?: "-"),
                "departureKor" to (kac.boardingKor ?: "-"),
                "arrivalKor" to (kac.arrivedKor ?: "-"),
                "scheduledTime" to (kac.std ?: "-"),
                "estimatedTime" to (kac.etd ?: kac.std ?: "-"),
                "status" to (kac.rmkEng ?: "-"),
                "statusKor" to (kac.rmkKor ?: "-"),
                "direction" to if (kac.io == "O") "DEPARTURE" else "ARRIVAL",
                "lineType" to (kac.line ?: "-"),
                "hasPosition" to (position != null),
                "latitude" to position?.latitude,
                "longitude" to position?.longitude,
                "altitude" to position?.altitude,
                "velocity" to position?.velocity,
                "heading" to position?.heading
            )
        }

        return mapOf(
            "kacSource" to kacApiClient.getStats().source,
            "openskySource" to openSkyClient.getStats().source,
            "totalFlights" to combined.size,
            "withPosition" to combined.count { it["hasPosition"] == true },
            "flights" to combined
        )
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new flight (admin)")
    fun createFlight(@RequestBody request: CreateFlightRequest): FlightResponse =
        flightService.createFlight(request)

    @PutMapping("/{id}/status")
    @Operation(summary = "Update flight status (state machine validated)")
    fun updateStatus(
        @PathVariable id: Long,
        @RequestBody request: UpdateStatusRequest
    ): FlightResponse = flightService.updateStatus(id, request)

    @PutMapping("/{id}/gate")
    @Operation(summary = "Update flight gate")
    fun updateGate(
        @PathVariable id: Long,
        @RequestBody request: UpdateGateRequest
    ): FlightResponse = flightService.updateGate(id, request)
}

@RestController
@RequestMapping("/api/airports")
@Tag(name = "Airports", description = "Airport departure/arrival queries")
class AirportController(private val flightService: FlightService) {

    @GetMapping("/{code}/departures")
    @Operation(summary = "Get departures for an airport")
    fun getDepartures(@PathVariable code: String): List<FlightResponse> =
        flightService.getDepartures(code.uppercase())

    @GetMapping("/{code}/arrivals")
    @Operation(summary = "Get arrivals for an airport")
    fun getArrivals(@PathVariable code: String): List<FlightResponse> =
        flightService.getArrivals(code.uppercase())
}

@RestController
@RequestMapping("/api/stats")
@Tag(name = "Statistics", description = "Flight statistics (cached 60s)")
class StatsController(private val flightService: FlightService) {

    @GetMapping("/daily")
    @Operation(summary = "Daily flight statistics")
    fun getDailyStats(): Map<String, Any> = flightService.getDailyStats()

    @GetMapping("/airport/{code}")
    @Operation(summary = "Statistics for a specific airport")
    fun getAirportStats(@PathVariable code: String): Map<String, Any> =
        flightService.getAirportStats(code.uppercase())

    @GetMapping("/delay-rate")
    @Operation(summary = "Current delay rate")
    fun getDelayRate(): Map<String, Any> = flightService.getDelayRate()
}
