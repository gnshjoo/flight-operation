package com.koreanair.ops.flight.service

import com.koreanair.ops.flight.dto.*
import com.koreanair.ops.flight.event.FlightStatusChangedEvent
import com.koreanair.ops.flight.event.GateChangedEvent
import com.koreanair.ops.flight.model.Flight
import com.koreanair.ops.flight.model.FlightStatus
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FlightService(
    private val flightRepository: FlightRepository,
    private val eventPublisher: ApplicationEventPublisher
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun getAllFlights(
        airport: String? = null,
        status: FlightStatus? = null
    ): List<FlightResponse> {
        val flights = when {
            status != null -> flightRepository.findByStatus(status)
            airport != null -> flightRepository.findByDepartureAirport(airport) +
                    flightRepository.findByArrivalAirport(airport)
            else -> flightRepository.findAll()
        }
        return flights.map { FlightResponse.from(it) }
    }

    fun getFlightById(id: Long): FlightResponse {
        val flight = flightRepository.findById(id)
            .orElseThrow { FlightNotFoundException(id) }
        return FlightResponse.from(flight)
    }

    fun getLiveTracking(): List<FlightWithPositionResponse> {
        val flights = flightRepository.findInFlightFlights()
        return flights.map { flight ->
            FlightWithPositionResponse(
                flight = FlightResponse.from(flight),
                position = generateMockPosition(flight)
            )
        }
    }

    fun getDepartures(airportCode: String): List<FlightResponse> =
        flightRepository.findByDepartureAirport(airportCode).map { FlightResponse.from(it) }

    fun getArrivals(airportCode: String): List<FlightResponse> =
        flightRepository.findByArrivalAirport(airportCode).map { FlightResponse.from(it) }

    @Transactional
    fun createFlight(request: CreateFlightRequest): FlightResponse {
        val flight = Flight(
            flightNumber = request.flightNumber,
            airline = request.airline,
            departureAirport = request.departureAirport,
            arrivalAirport = request.arrivalAirport,
            scheduledDeparture = request.scheduledDeparture,
            scheduledArrival = request.scheduledArrival,
            gate = request.gate,
            aircraft = request.aircraft,
            departureTimezone = request.departureTimezone,
            arrivalTimezone = request.arrivalTimezone
        )
        val saved = flightRepository.save(flight)
        log.info("Flight created: {} ({}→{})", saved.flightNumber, saved.departureAirport, saved.arrivalAirport)
        return FlightResponse.from(saved)
    }

    @Transactional
    fun updateStatus(id: Long, request: UpdateStatusRequest): FlightResponse {
        val flight = flightRepository.findById(id)
            .orElseThrow { FlightNotFoundException(id) }

        val oldStatus = flight.status
        if (!FlightStatus.isValidTransition(oldStatus, request.status)) {
            throw InvalidStatusTransitionException(oldStatus, request.status)
        }

        flight.status = request.status

        if (request.status == FlightStatus.DEPARTED) {
            flight.actualDeparture = java.time.Instant.now()
        }
        if (request.status == FlightStatus.ARRIVED) {
            flight.actualArrival = java.time.Instant.now()
        }

        val saved = flightRepository.save(flight)
        log.info("Flight {} status: {} → {}", saved.flightNumber, oldStatus, request.status)

        eventPublisher.publishEvent(
            FlightStatusChangedEvent(
                flightId = saved.id,
                flightNumber = saved.flightNumber,
                oldStatus = oldStatus,
                newStatus = request.status,
                delayMinutes = request.delayMinutes,
                reason = request.reason
            )
        )

        return FlightResponse.from(saved)
    }

    @Transactional
    fun updateGate(id: Long, request: UpdateGateRequest): FlightResponse {
        val flight = flightRepository.findById(id)
            .orElseThrow { FlightNotFoundException(id) }

        val oldGate = flight.gate
        flight.gate = request.gate
        val saved = flightRepository.save(flight)
        log.info("Flight {} gate: {} → {}", saved.flightNumber, oldGate, request.gate)

        eventPublisher.publishEvent(
            GateChangedEvent(
                flightId = saved.id,
                flightNumber = saved.flightNumber,
                oldGate = oldGate,
                newGate = request.gate
            )
        )

        return FlightResponse.from(saved)
    }

    @Cacheable("statsDaily")
    fun getDailyStats(): Map<String, Any> {
        val total = flightRepository.countAll()
        val delayed = flightRepository.countDelayed()
        val cancelled = flightRepository.countCancelled()
        val onTime = total - delayed - cancelled
        val onTimeRate = if (total > 0) (onTime.toDouble() / total * 100) else 0.0

        return mapOf(
            "totalFlights" to total,
            "onTime" to onTime,
            "onTimeRate" to "%.1f".format(onTimeRate),
            "delayed" to delayed,
            "cancelled" to cancelled
        )
    }

    @Cacheable("statsAirport")
    fun getAirportStats(airportCode: String): Map<String, Any> {
        val departures = flightRepository.findByDepartureAirport(airportCode).size.toLong()
        val arrivals = flightRepository.findByArrivalAirport(airportCode).size.toLong()
        return mapOf(
            "airport" to airportCode,
            "departures" to departures,
            "arrivals" to arrivals,
            "total" to (departures + arrivals)
        )
    }

    @Cacheable("statsDelayRate")
    fun getDelayRate(): Map<String, Any> {
        val total = flightRepository.countAll()
        val delayed = flightRepository.countDelayed()
        val rate = if (total > 0) (delayed.toDouble() / total * 100) else 0.0
        return mapOf("delayRate" to "%.1f".format(rate), "delayed" to delayed, "total" to total)
    }

    private fun generateMockPosition(flight: Flight): FlightPosition {
        // Simple mock: random position along a great circle approximation
        val hash = flight.id.hashCode()
        val lat = 20.0 + (hash % 40)
        val lon = 100.0 + (hash % 80)
        return FlightPosition(
            latitude = lat,
            longitude = lon,
            altitude = 35000.0 + (hash % 6000),
            velocity = 450.0 + (hash % 100).toDouble(),
            heading = (hash % 360).toDouble()
        )
    }
}

class FlightNotFoundException(id: Long) :
    RuntimeException("Flight not found: $id")

class InvalidStatusTransitionException(from: FlightStatus, to: FlightStatus) :
    RuntimeException("Invalid status transition: $from → $to")
