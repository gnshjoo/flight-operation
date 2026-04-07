package com.koreanair.ops.flight.model

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "flights")
class Flight(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, length = 10)
    val flightNumber: String,

    @Column(nullable = false, length = 50)
    val airline: String = "Korean Air",

    @Column(nullable = false, length = 3)
    val departureAirport: String,

    @Column(nullable = false, length = 3)
    val arrivalAirport: String,

    @Column(nullable = false)
    val scheduledDeparture: Instant,

    @Column(nullable = false)
    val scheduledArrival: Instant,

    var actualDeparture: Instant? = null,

    var actualArrival: Instant? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: FlightStatus = FlightStatus.SCHEDULED,

    @Column(length = 10)
    var gate: String? = null,

    @Column(length = 30)
    val aircraft: String? = null,

    @Column(nullable = false, length = 50)
    val departureTimezone: String = "Asia/Seoul",

    @Column(nullable = false, length = 50)
    val arrivalTimezone: String = "Asia/Seoul"
)

enum class FlightStatus {
    SCHEDULED,
    BOARDING,
    DEPARTED,
    IN_FLIGHT,
    ARRIVED,
    DELAYED,
    CANCELLED;

    companion object {
        private val validTransitions: Map<FlightStatus, Set<FlightStatus>> = mapOf(
            SCHEDULED to setOf(BOARDING, DELAYED, CANCELLED),
            BOARDING to setOf(DEPARTED, DELAYED),
            DEPARTED to setOf(IN_FLIGHT, DELAYED),
            IN_FLIGHT to setOf(ARRIVED, DELAYED),
            ARRIVED to emptySet(),
            DELAYED to setOf(BOARDING, CANCELLED),
            CANCELLED to emptySet()
        )

        fun isValidTransition(from: FlightStatus, to: FlightStatus): Boolean {
            return validTransitions[from]?.contains(to) ?: false
        }
    }
}
