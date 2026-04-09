package com.koreanair.ops.flight.event

import com.koreanair.ops.flight.model.FlightStatus
import java.time.Instant

data class FlightStatusChangedEvent(
    val flightId: Long,
    val flightNumber: String,
    val oldStatus: FlightStatus,
    val newStatus: FlightStatus,
    val delayMinutes: Int? = null,
    val reason: String? = null,
    val timestamp: Instant = Instant.now()
)

data class GateChangedEvent(
    val flightId: Long,
    val flightNumber: String,
    val oldGate: String?,
    val newGate: String,
    val timestamp: Instant = Instant.now()
)
