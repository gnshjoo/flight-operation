package com.koreanair.ops.alert.service

import com.koreanair.ops.alert.model.Alert
import com.koreanair.ops.alert.model.AlertType
import com.koreanair.ops.alert.model.Severity
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.messaging.simp.SimpMessagingTemplate
import java.util.*

class AlertServiceTest {

    private val repository = mockk<AlertRepository>()
    private val messagingTemplate = mockk<SimpMessagingTemplate>(relaxed = true)
    private lateinit var service: AlertService

    @BeforeEach
    fun setup() {
        service = AlertService(repository, messagingTemplate)
    }

    @Test
    fun `createAlert saves and pushes via WebSocket`() {
        val savedAlert = Alert(
            id = 1,
            flightId = 10,
            flightNumber = "KE051",
            type = AlertType.DELAY,
            message = "KE051 delayed by 40 minutes",
            severity = Severity.CRITICAL
        )
        every { repository.save(any()) } returns savedAlert

        val result = service.createAlert(
            flightId = 10,
            flightNumber = "KE051",
            type = AlertType.DELAY,
            message = "KE051 delayed by 40 minutes",
            severity = Severity.CRITICAL
        )

        assertEquals("KE051", result.flightNumber)
        assertEquals(AlertType.DELAY, result.type)
        assertEquals(Severity.CRITICAL, result.severity)
        verify { messagingTemplate.convertAndSend(eq("/topic/alerts"), any<Any>()) }
    }

    @Test
    fun `acknowledgeAlert sets acknowledged to true`() {
        val alert = Alert(
            id = 1, flightId = 10, flightNumber = "KE051",
            type = AlertType.DELAY, message = "delayed",
            severity = Severity.CRITICAL, acknowledged = false
        )
        every { repository.findById(1L) } returns Optional.of(alert)
        every { repository.save(any()) } answers { firstArg() }

        val result = service.acknowledgeAlert(1)

        assertTrue(result.acknowledged)
        verify { repository.save(match { it.acknowledged }) }
    }

    @Test
    fun `acknowledgeAlert for nonexistent throws AlertNotFoundException`() {
        every { repository.findById(999L) } returns Optional.empty()

        assertThrows<AlertNotFoundException> {
            service.acknowledgeAlert(999)
        }
    }

    @Test
    fun `getActiveAlerts returns only unacknowledged`() {
        val alerts = listOf(
            Alert(id = 1, flightId = 1, flightNumber = "KE001",
                type = AlertType.DELAY, message = "delayed", severity = Severity.CRITICAL)
        )
        every { repository.findByAcknowledgedFalseOrderByCreatedAtDesc() } returns alerts

        val result = service.getActiveAlerts()

        assertEquals(1, result.size)
        assertFalse(result[0].acknowledged)
    }
}
