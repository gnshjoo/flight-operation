package com.koreanair.ops.alert.controller

import com.koreanair.ops.alert.model.AlertType
import com.koreanair.ops.alert.model.Severity
import com.koreanair.ops.alert.service.AlertService
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/internal/events")
class InternalEventController(private val alertService: AlertService) {

    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping("/status-changed")
    fun onStatusChanged(@RequestBody event: StatusChangedPayload) {
        log.info("Received status change: {} {} → {}", event.flightNumber, event.oldStatus, event.newStatus)

        val (type, severity, message) = when (event.newStatus) {
            "DELAYED" -> Triple(
                AlertType.DELAY,
                Severity.CRITICAL,
                "${event.flightNumber} delayed${event.delayMinutes?.let { " by $it minutes" } ?: ""}. ${event.reason ?: ""}"
            )
            "CANCELLED" -> Triple(
                AlertType.CANCELLATION,
                Severity.CRITICAL,
                "${event.flightNumber} cancelled. ${event.reason ?: ""}"
            )
            else -> Triple(
                AlertType.STATUS_CHANGE,
                Severity.INFO,
                "${event.flightNumber}: ${event.oldStatus} → ${event.newStatus}"
            )
        }

        alertService.createAlert(
            flightId = event.flightId,
            flightNumber = event.flightNumber,
            type = type,
            message = message.trim(),
            severity = severity
        )
    }

    @PostMapping("/gate-changed")
    fun onGateChanged(@RequestBody event: GateChangedPayload) {
        log.info("Received gate change: {} → {}", event.flightNumber, event.newGate)

        alertService.createAlert(
            flightId = event.flightId,
            flightNumber = event.flightNumber,
            type = AlertType.GATE_CHANGE,
            message = "${event.flightNumber} gate changed from ${event.oldGate ?: "unassigned"} to ${event.newGate}",
            severity = Severity.WARNING
        )
    }
}

data class StatusChangedPayload(
    val flightId: Long,
    val flightNumber: String,
    val oldStatus: String,
    val newStatus: String,
    val delayMinutes: Int? = null,
    val reason: String? = null,
    val timestamp: String? = null
)

data class GateChangedPayload(
    val flightId: Long,
    val flightNumber: String,
    val oldGate: String? = null,
    val newGate: String,
    val timestamp: String? = null
)
