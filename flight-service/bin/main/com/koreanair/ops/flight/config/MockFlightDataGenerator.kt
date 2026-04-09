package com.koreanair.ops.flight.config

import com.koreanair.ops.flight.dto.UpdateStatusRequest
import com.koreanair.ops.flight.model.Flight
import com.koreanair.ops.flight.model.FlightStatus
import com.koreanair.ops.flight.service.FlightRepository
import com.koreanair.ops.flight.service.FlightService
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.temporal.ChronoUnit

@Component
@Profile("demo")
class MockFlightDataGenerator(
    private val flightRepository: FlightRepository,
    private val flightService: FlightService
) : CommandLineRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    private val routes = listOf(
        Triple("KE001", "ICN" to "LAX", "America/Los_Angeles"),
        Triple("KE023", "ICN" to "CDG", "Europe/Paris"),
        Triple("KE051", "ICN" to "NRT", "Asia/Tokyo"),
        Triple("KE061", "ICN" to "JFK", "America/New_York"),
        Triple("KE073", "ICN" to "SIN", "Asia/Singapore"),
        Triple("KE089", "ICN" to "SYD", "Australia/Sydney"),
        Triple("KE011", "ICN" to "HND", "Asia/Tokyo"),
        Triple("KE035", "ICN" to "PEK", "Asia/Shanghai"),
        Triple("KE012", "LAX" to "ICN", "Asia/Seoul"),
        Triple("KE024", "CDG" to "ICN", "Asia/Seoul"),
        Triple("KE052", "NRT" to "ICN", "Asia/Seoul"),
        Triple("KE062", "JFK" to "ICN", "Asia/Seoul"),
        Triple("KE074", "SIN" to "ICN", "Asia/Seoul"),
        Triple("KE090", "SYD" to "ICN", "Asia/Seoul"),
    )

    private val aircraft = listOf("B777-300ER", "A380-800", "B787-9", "A330-300", "B747-8")
    private val gates = listOf("A01", "A05", "A12", "B03", "B07", "C02", "C08", "D01")

    override fun run(vararg args: String?) {
        if (flightRepository.count() > 0) {
            log.info("Demo data already exists, skipping seed")
            return
        }

        log.info("Seeding demo flight data...")
        val now = Instant.now()

        val statuses = listOf(
            FlightStatus.SCHEDULED, FlightStatus.SCHEDULED, FlightStatus.BOARDING,
            FlightStatus.DEPARTED, FlightStatus.IN_FLIGHT, FlightStatus.IN_FLIGHT,
            FlightStatus.IN_FLIGHT, FlightStatus.ARRIVED, FlightStatus.ARRIVED,
            FlightStatus.DELAYED, FlightStatus.DELAYED, FlightStatus.CANCELLED,
            FlightStatus.SCHEDULED, FlightStatus.BOARDING
        )

        routes.forEachIndexed { index, (flightNum, route, arrTz) ->
            val depHoursOffset = (index * 2L) - 10
            val flight = Flight(
                flightNumber = flightNum,
                departureAirport = route.first,
                arrivalAirport = route.second,
                scheduledDeparture = now.plus(depHoursOffset, ChronoUnit.HOURS),
                scheduledArrival = now.plus(depHoursOffset + 10, ChronoUnit.HOURS),
                status = statuses[index % statuses.size],
                gate = gates[index % gates.size],
                aircraft = aircraft[index % aircraft.size],
                departureTimezone = if (route.first == "ICN") "Asia/Seoul" else arrTz,
                arrivalTimezone = arrTz,
                actualDeparture = if (statuses[index % statuses.size] in listOf(
                        FlightStatus.DEPARTED, FlightStatus.IN_FLIGHT, FlightStatus.ARRIVED
                    )
                ) now.plus(depHoursOffset + 1, ChronoUnit.HOURS) else null,
                actualArrival = if (statuses[index % statuses.size] == FlightStatus.ARRIVED)
                    now.plus(depHoursOffset + 11, ChronoUnit.HOURS) else null
            )
            flightRepository.save(flight)
        }

        log.info("Seeded {} demo flights", routes.size)
    }

    @Scheduled(fixedDelay = 30000) // Every 30 seconds
    fun simulateStatusChanges() {
        val candidates = flightRepository.findByStatusIn(
            listOf(FlightStatus.SCHEDULED, FlightStatus.BOARDING, FlightStatus.DEPARTED, FlightStatus.IN_FLIGHT, FlightStatus.DELAYED)
        )

        if (candidates.isEmpty()) return

        val flight = candidates.random()
        val validNextStatuses = FlightStatus.entries.filter {
            FlightStatus.isValidTransition(flight.status, it)
        }

        if (validNextStatuses.isEmpty()) return

        val nextStatus = validNextStatuses.random()
        val delayMinutes = if (nextStatus == FlightStatus.DELAYED) (15..120).random() else null
        val reason = if (nextStatus == FlightStatus.DELAYED) {
            listOf("Weather conditions", "Aircraft maintenance", "Air traffic control", "Crew scheduling").random()
        } else if (nextStatus == FlightStatus.CANCELLED) {
            listOf("Mechanical issue", "Severe weather", "Operational decision").random()
        } else null

        try {
            flightService.updateStatus(
                flight.id,
                UpdateStatusRequest(status = nextStatus, delayMinutes = delayMinutes, reason = reason)
            )
            log.info("Simulator: {} {} → {}", flight.flightNumber, flight.status, nextStatus)
        } catch (e: Exception) {
            log.warn("Simulator: failed to update {} — {}", flight.flightNumber, e.message)
        }
    }
}
