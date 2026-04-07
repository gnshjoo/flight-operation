package com.koreanair.ops.alert.dto

import com.koreanair.ops.alert.model.Alert
import com.koreanair.ops.alert.model.AlertType
import com.koreanair.ops.alert.model.Severity
import java.time.Instant

data class AlertResponse(
    val id: Long,
    val flightId: Long,
    val flightNumber: String,
    val type: AlertType,
    val message: String,
    val severity: Severity,
    val createdAt: Instant,
    val acknowledged: Boolean
) {
    companion object {
        fun from(alert: Alert) = AlertResponse(
            id = alert.id,
            flightId = alert.flightId,
            flightNumber = alert.flightNumber,
            type = alert.type,
            message = alert.message,
            severity = alert.severity,
            createdAt = alert.createdAt,
            acknowledged = alert.acknowledged
        )
    }
}
