package com.koreanair.ops.alert.controller

import com.koreanair.ops.alert.dto.AlertResponse
import com.koreanair.ops.alert.service.AlertService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/alerts")
@Tag(name = "Alerts", description = "Alert management and WebSocket push")
class AlertController(private val alertService: AlertService) {

    @GetMapping
    @Operation(summary = "List all alerts (newest first)")
    fun getAllAlerts(): List<AlertResponse> = alertService.getAllAlerts()

    @GetMapping("/active")
    @Operation(summary = "List unacknowledged alerts")
    fun getActiveAlerts(): List<AlertResponse> = alertService.getActiveAlerts()

    @PutMapping("/{id}/acknowledge")
    @Operation(summary = "Acknowledge an alert")
    fun acknowledgeAlert(@PathVariable id: Long): AlertResponse =
        alertService.acknowledgeAlert(id)
}
