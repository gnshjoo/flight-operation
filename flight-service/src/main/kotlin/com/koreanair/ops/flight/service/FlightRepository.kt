package com.koreanair.ops.flight.service

import com.koreanair.ops.flight.model.Flight
import com.koreanair.ops.flight.model.FlightStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface FlightRepository : JpaRepository<Flight, Long> {

    fun findByStatus(status: FlightStatus): List<Flight>

    fun findByStatusIn(statuses: List<FlightStatus>): List<Flight>

    fun findByDepartureAirport(airport: String): List<Flight>

    fun findByArrivalAirport(airport: String): List<Flight>

    @Query("SELECT f FROM Flight f WHERE f.status IN ('DEPARTED', 'IN_FLIGHT')")
    fun findInFlightFlights(): List<Flight>

    @Query("SELECT COUNT(f) FROM Flight f")
    fun countAll(): Long

    @Query("SELECT COUNT(f) FROM Flight f WHERE f.status = 'DELAYED'")
    fun countDelayed(): Long

    @Query("SELECT COUNT(f) FROM Flight f WHERE f.status = 'CANCELLED'")
    fun countCancelled(): Long

    @Query("SELECT COUNT(f) FROM Flight f WHERE f.status IN ('ARRIVED') AND f.actualArrival IS NOT NULL")
    fun countOnTime(): Long
}
