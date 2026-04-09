package com.koreanair.ops.flight.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class FlightStatusTest {

    @ParameterizedTest
    @CsvSource(
        "SCHEDULED, BOARDING",
        "SCHEDULED, DELAYED",
        "SCHEDULED, CANCELLED",
        "BOARDING, DEPARTED",
        "BOARDING, DELAYED",
        "DEPARTED, IN_FLIGHT",
        "DEPARTED, DELAYED",
        "IN_FLIGHT, ARRIVED",
        "IN_FLIGHT, DELAYED",
        "DELAYED, BOARDING",
        "DELAYED, CANCELLED"
    )
    fun `valid transitions should be allowed`(from: FlightStatus, to: FlightStatus) {
        assertTrue(FlightStatus.isValidTransition(from, to),
            "$from → $to should be valid")
    }

    @ParameterizedTest
    @CsvSource(
        "ARRIVED, BOARDING",
        "ARRIVED, SCHEDULED",
        "ARRIVED, DELAYED",
        "CANCELLED, SCHEDULED",
        "CANCELLED, BOARDING",
        "BOARDING, SCHEDULED",
        "IN_FLIGHT, BOARDING",
        "DEPARTED, BOARDING",
        "SCHEDULED, IN_FLIGHT",
        "SCHEDULED, ARRIVED"
    )
    fun `invalid transitions should be rejected`(from: FlightStatus, to: FlightStatus) {
        assertFalse(FlightStatus.isValidTransition(from, to),
            "$from → $to should be invalid")
    }

    @Test
    fun `ARRIVED is terminal state`() {
        FlightStatus.entries.forEach { target ->
            if (target != FlightStatus.ARRIVED) {
                assertFalse(FlightStatus.isValidTransition(FlightStatus.ARRIVED, target),
                    "ARRIVED → $target should be invalid")
            }
        }
    }

    @Test
    fun `CANCELLED is terminal state`() {
        FlightStatus.entries.forEach { target ->
            if (target != FlightStatus.CANCELLED) {
                assertFalse(FlightStatus.isValidTransition(FlightStatus.CANCELLED, target),
                    "CANCELLED → $target should be invalid")
            }
        }
    }

    @Test
    fun `DELAYED can recover to BOARDING`() {
        assertTrue(FlightStatus.isValidTransition(FlightStatus.DELAYED, FlightStatus.BOARDING))
    }
}
