package com.koreanair.ops.flight.service

import com.koreanair.ops.flight.event.FlightStatusChangedEvent
import com.koreanair.ops.flight.event.GateChangedEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.event.EventListener
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class AlertNotifier(
    @Value("\${app.alert-service.url:http://localhost:8082}") private val alertServiceUrl: String
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val client = RestClient.create()

    @EventListener
    fun onStatusChanged(event: FlightStatusChangedEvent) {
        try {
            client.post()
                .uri("$alertServiceUrl/api/internal/events/status-changed")
                .contentType(MediaType.APPLICATION_JSON)
                .body(event)
                .retrieve()
                .toBodilessEntity()
            log.info("Notified alert-service: {} {} → {}", event.flightNumber, event.oldStatus, event.newStatus)
        } catch (e: Exception) {
            log.warn("Failed to notify alert-service: {}", e.message)
        }
    }

    @EventListener
    fun onGateChanged(event: GateChangedEvent) {
        try {
            client.post()
                .uri("$alertServiceUrl/api/internal/events/gate-changed")
                .contentType(MediaType.APPLICATION_JSON)
                .body(event)
                .retrieve()
                .toBodilessEntity()
            log.info("Notified alert-service: {} gate → {}", event.flightNumber, event.newGate)
        } catch (e: Exception) {
            log.warn("Failed to notify alert-service: {}", e.message)
        }
    }
}
