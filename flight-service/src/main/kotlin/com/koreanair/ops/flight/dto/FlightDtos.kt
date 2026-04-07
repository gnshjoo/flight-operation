package com.koreanair.ops.flight.dto

import com.koreanair.ops.flight.model.Flight
import com.koreanair.ops.flight.model.FlightStatus
import java.time.Instant

data class FlightResponse(
    val id: Long,
    val flightNumber: String,
    val airline: String,
    val departureAirport: String,
    val arrivalAirport: String,
    val scheduledDeparture: Instant,
    val scheduledArrival: Instant,
    val actualDeparture: Instant?,
    val actualArrival: Instant?,
    val status: FlightStatus,
    val gate: String?,
    val aircraft: String?,
    val departureTimezone: String,
    val arrivalTimezone: String
) {
    companion object {
        fun from(flight: Flight) = FlightResponse(
            id = flight.id,
            flightNumber = flight.flightNumber,
            airline = flight.airline,
            departureAirport = flight.departureAirport,
            arrivalAirport = flight.arrivalAirport,
            scheduledDeparture = flight.scheduledDeparture,
            scheduledArrival = flight.scheduledArrival,
            actualDeparture = flight.actualDeparture,
            actualArrival = flight.actualArrival,
            status = flight.status,
            gate = flight.gate,
            aircraft = flight.aircraft,
            departureTimezone = flight.departureTimezone,
            arrivalTimezone = flight.arrivalTimezone
        )
    }
}

data class FlightWithPositionResponse(
    val flight: FlightResponse,
    val position: FlightPosition?
)

data class FlightPosition(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val velocity: Double,
    val heading: Double
)

data class CreateFlightRequest(
    val flightNumber: String,
    val airline: String = "Korean Air",
    val departureAirport: String,
    val arrivalAirport: String,
    val scheduledDeparture: Instant,
    val scheduledArrival: Instant,
    val gate: String? = null,
    val aircraft: String? = null,
    val departureTimezone: String = "Asia/Seoul",
    val arrivalTimezone: String = "Asia/Seoul"
)

data class UpdateStatusRequest(
    val status: FlightStatus,
    val delayMinutes: Int? = null,
    val reason: String? = null
)

data class UpdateGateRequest(
    val gate: String
)

data class DailyStats(
    val date: String,
    val totalFlights: Long,
    val onTime: Long,
    val delayed: Long,
    val cancelled: Long
)

data class AirportStats(
    val airport: String,
    val departures: Long,
    val arrivals: Long
)

data class DelayRateStats(
    val date: String,
    val delayRate: Double
)
