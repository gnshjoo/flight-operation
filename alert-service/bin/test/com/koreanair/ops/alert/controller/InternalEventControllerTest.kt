package com.koreanair.ops.alert.controller

import com.koreanair.ops.alert.service.AlertRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InternalEventControllerTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var alertRepository: AlertRepository

    @BeforeEach
    fun cleanup() {
        alertRepository.deleteAll()
    }

    @Test
    fun `status-changed DELAYED creates CRITICAL alert`() {
        mockMvc.post("/api/internal/events/status-changed") {
            contentType = MediaType.APPLICATION_JSON
            content = """{
                "flightId": 1,
                "flightNumber": "KE051",
                "oldStatus": "SCHEDULED",
                "newStatus": "DELAYED",
                "delayMinutes": 40,
                "reason": "Weather conditions"
            }"""
        }.andExpect { status { isOk() } }

        // Verify alert was created
        mockMvc.get("/api/alerts/active")
            .andExpect {
                status { isOk() }
                jsonPath("$[0].flightNumber") { value("KE051") }
                jsonPath("$[0].type") { value("DELAY") }
                jsonPath("$[0].severity") { value("CRITICAL") }
            }
    }

    @Test
    fun `status-changed CANCELLED creates CRITICAL alert`() {
        mockMvc.post("/api/internal/events/status-changed") {
            contentType = MediaType.APPLICATION_JSON
            content = """{
                "flightId": 2,
                "flightNumber": "KE089",
                "oldStatus": "SCHEDULED",
                "newStatus": "CANCELLED",
                "reason": "Aircraft maintenance"
            }"""
        }.andExpect { status { isOk() } }

        mockMvc.get("/api/alerts")
            .andExpect {
                status { isOk() }
                jsonPath("$[?(@.flightNumber == 'KE089')].type") { value("CANCELLATION") }
            }
    }

    @Test
    fun `status-changed BOARDING creates INFO alert`() {
        mockMvc.post("/api/internal/events/status-changed") {
            contentType = MediaType.APPLICATION_JSON
            content = """{
                "flightId": 3,
                "flightNumber": "KE001",
                "oldStatus": "SCHEDULED",
                "newStatus": "BOARDING"
            }"""
        }.andExpect { status { isOk() } }

        mockMvc.get("/api/alerts")
            .andExpect {
                status { isOk() }
                jsonPath("$[?(@.flightNumber == 'KE001')].severity") { value("INFO") }
            }
    }

    @Test
    fun `gate-changed creates WARNING alert`() {
        mockMvc.post("/api/internal/events/gate-changed") {
            contentType = MediaType.APPLICATION_JSON
            content = """{
                "flightId": 1,
                "flightNumber": "KE001",
                "oldGate": "A10",
                "newGate": "A12"
            }"""
        }.andExpect { status { isOk() } }

        mockMvc.get("/api/alerts")
            .andExpect {
                status { isOk() }
                jsonPath("$[?(@.flightNumber == 'KE001' && @.type == 'GATE_CHANGE')].severity") { value("WARNING") }
            }
    }
}
