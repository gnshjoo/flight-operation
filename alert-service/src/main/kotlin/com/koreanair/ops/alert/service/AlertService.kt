package com.koreanair.ops.alert.service

import com.koreanair.ops.alert.dto.AlertResponse
import com.koreanair.ops.alert.model.Alert
import com.koreanair.ops.alert.model.AlertType
import com.koreanair.ops.alert.model.Severity
import org.slf4j.LoggerFactory
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AlertService(
    private val alertRepository: AlertRepository,
    private val messagingTemplate: SimpMessagingTemplate
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun getAllAlerts(): List<AlertResponse> =
        alertRepository.findAllByOrderByCreatedAtDesc().map { AlertResponse.from(it) }

    fun getActiveAlerts(): List<AlertResponse> =
        alertRepository.findByAcknowledgedFalseOrderByCreatedAtDesc().map { AlertResponse.from(it) }

    @Transactional
    fun acknowledgeAlert(id: Long): AlertResponse {
        val alert = alertRepository.findById(id)
            .orElseThrow { AlertNotFoundException(id) }
        alert.acknowledged = true
        val saved = alertRepository.save(alert)
        log.info("Alert {} acknowledged", id)
        return AlertResponse.from(saved)
    }

    @Transactional
    fun createAlert(
        flightId: Long,
        flightNumber: String,
        type: AlertType,
        message: String,
        severity: Severity
    ): AlertResponse {
        val alert = Alert(
            flightId = flightId,
            flightNumber = flightNumber,
            type = type,
            message = message,
            severity = severity
        )
        val saved = alertRepository.save(alert)
        val response = AlertResponse.from(saved)

        // Push via WebSocket
        messagingTemplate.convertAndSend("/topic/alerts", response)
        log.info("Alert created and pushed: [{}] {} — {}", severity, flightNumber, type)

        return response
    }
}

class AlertNotFoundException(id: Long) : RuntimeException("Alert not found: $id")
