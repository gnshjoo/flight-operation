package com.koreanair.ops.flight.service

import com.koreanair.ops.flight.dto.CreateFlightRequest
import com.koreanair.ops.flight.dto.UpdateStatusRequest
import com.koreanair.ops.flight.model.Flight
import com.koreanair.ops.flight.model.FlightStatus
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.context.ApplicationEventPublisher
import java.time.Instant
import java.util.*

class FlightServiceTest {

    private val repository = mockk<FlightRepository>()
    private val eventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)
    private lateinit var service: FlightService

    @BeforeEach
    fun setup() {
        service = FlightService(repository, eventPublisher)
    }

    private fun createTestFlight(
        id: Long = 1L,
        status: FlightStatus = FlightStatus.SCHEDULED
    ) = Flight(
        id = id,
        flightNumber = "KE001",
        departureAirport = "ICN",
        arrivalAirport = "LAX",
        scheduledDeparture = Instant.now(),
        scheduledArrival = Instant.now().plusSeconds(36000),
        status = status,
        gate = "A12",
        aircraft = "B777-300ER"
    )

    @Test
    fun `updateStatus with valid transition publishes event`() {
        val flight = createTestFlight(status = FlightStatus.SCHEDULED)
        every { repository.findById(1L) } returns Optional.of(flight)
        every { repository.save(any()) } answers { firstArg() }

        val result = service.updateStatus(1L, UpdateStatusRequest(FlightStatus.BOARDING))

        assertEquals(FlightStatus.BOARDING, result.status)
        verify { eventPublisher.publishEvent(any<Any>()) }
    }

    @Test
    fun `updateStatus with invalid transition throws exception`() {
        val flight = createTestFlight(status = FlightStatus.ARRIVED)
        every { repository.findById(1L) } returns Optional.of(flight)

        assertThrows<InvalidStatusTransitionException> {
            service.updateStatus(1L, UpdateStatusRequest(FlightStatus.BOARDING))
        }
        verify(exactly = 0) { eventPublisher.publishEvent(any<Any>()) }
    }

    @Test
    fun `updateStatus to DEPARTED sets actualDeparture`() {
        val flight = createTestFlight(status = FlightStatus.BOARDING)
        every { repository.findById(1L) } returns Optional.of(flight)
        every { repository.save(any()) } answers { firstArg() }

        val result = service.updateStatus(1L, UpdateStatusRequest(FlightStatus.DEPARTED))

        assertEquals(FlightStatus.DEPARTED, result.status)
        assertNotNull(result.actualDeparture)
    }

    @Test
    fun `updateStatus to ARRIVED sets actualArrival`() {
        val flight = createTestFlight(status = FlightStatus.IN_FLIGHT)
        every { repository.findById(1L) } returns Optional.of(flight)
        every { repository.save(any()) } answers { firstArg() }

        val result = service.updateStatus(1L, UpdateStatusRequest(FlightStatus.ARRIVED))

        assertEquals(FlightStatus.ARRIVED, result.status)
        assertNotNull(result.actualArrival)
    }

    @Test
    fun `updateStatus for nonexistent flight throws FlightNotFoundException`() {
        every { repository.findById(999L) } returns Optional.empty()

        assertThrows<FlightNotFoundException> {
            service.updateStatus(999L, UpdateStatusRequest(FlightStatus.BOARDING))
        }
    }

    @Test
    fun `createFlight saves and returns response`() {
        val request = CreateFlightRequest(
            flightNumber = "KE051",
            departureAirport = "ICN",
            arrivalAirport = "NRT",
            scheduledDeparture = Instant.now(),
            scheduledArrival = Instant.now().plusSeconds(7200)
        )
        every { repository.save(any()) } answers {
            val f = firstArg<Flight>()
            Flight(
                id = 1L,
                flightNumber = f.flightNumber,
                departureAirport = f.departureAirport,
                arrivalAirport = f.arrivalAirport,
                scheduledDeparture = f.scheduledDeparture,
                scheduledArrival = f.scheduledArrival
            )
        }

        val result = service.createFlight(request)

        assertEquals("KE051", result.flightNumber)
        assertEquals("ICN", result.departureAirport)
        verify { repository.save(any()) }
    }

    @Test
    fun `getDailyStats returns correct counts`() {
        every { repository.countAll() } returns 14
        every { repository.countDelayed() } returns 3
        every { repository.countCancelled() } returns 1
        every { repository.countOnTime() } returns 10

        val stats = service.getDailyStats()

        assertEquals(14L, stats["totalFlights"])
        assertEquals(3L, stats["delayed"])
        assertEquals(1L, stats["cancelled"])
    }

    @Test
    fun `getLiveTracking returns flights with positions`() {
        val flights = listOf(
            createTestFlight(id = 1, status = FlightStatus.IN_FLIGHT),
            createTestFlight(id = 2, status = FlightStatus.DEPARTED)
        )
        every { repository.findInFlightFlights() } returns flights

        val result = service.getLiveTracking()

        assertEquals(2, result.size)
        result.forEach { assertNotNull(it.position) }
    }
}
