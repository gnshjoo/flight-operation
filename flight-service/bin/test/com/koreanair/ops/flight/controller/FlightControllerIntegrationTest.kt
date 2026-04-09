package com.koreanair.ops.flight.controller

import com.koreanair.ops.flight.model.Flight
import com.koreanair.ops.flight.model.FlightStatus
import com.koreanair.ops.flight.service.FlightRepository
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
import org.springframework.test.web.servlet.put
import java.time.Instant

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FlightControllerIntegrationTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var flightRepository: FlightRepository

    private var token: String = ""

    @BeforeEach
    fun setup() {
        flightRepository.deleteAll()

        // Get JWT token
        val loginResult = mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"username":"admin","password":"admin"}"""
        }.andReturn()
        token = loginResult.response.contentAsString
            .substringAfter("\"token\":\"")
            .substringBefore("\"")

        // Seed test flight
        flightRepository.save(
            Flight(
                flightNumber = "KE001",
                departureAirport = "ICN",
                arrivalAirport = "LAX",
                scheduledDeparture = Instant.now(),
                scheduledArrival = Instant.now().plusSeconds(36000),
                status = FlightStatus.SCHEDULED,
                gate = "A12"
            )
        )
    }

    @Test
    fun `GET flights returns list`() {
        mockMvc.get("/api/flights")
            .andExpect {
                status { isOk() }
                jsonPath("$[0].flightNumber") { value("KE001") }
                jsonPath("$[0].status") { value("SCHEDULED") }
            }
    }

    @Test
    fun `PUT status with valid transition returns updated flight`() {
        val flight = flightRepository.findAll().first()

        mockMvc.put("/api/flights/${flight.id}/status") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"status":"BOARDING"}"""
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value("BOARDING") }
        }
    }

    @Test
    fun `PUT status with invalid transition returns 400`() {
        val flight = flightRepository.findAll().first()

        // First move to ARRIVED via valid path
        mockMvc.put("/api/flights/${flight.id}/status") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"status":"BOARDING"}"""
            header("Authorization", "Bearer $token")
        }
        mockMvc.put("/api/flights/${flight.id}/status") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"status":"DEPARTED"}"""
            header("Authorization", "Bearer $token")
        }
        mockMvc.put("/api/flights/${flight.id}/status") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"status":"IN_FLIGHT"}"""
            header("Authorization", "Bearer $token")
        }
        mockMvc.put("/api/flights/${flight.id}/status") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"status":"ARRIVED"}"""
            header("Authorization", "Bearer $token")
        }

        // Now try invalid: ARRIVED → BOARDING
        mockMvc.put("/api/flights/${flight.id}/status") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"status":"BOARDING"}"""
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `PUT status without auth returns 403`() {
        val flight = flightRepository.findAll().first()

        mockMvc.put("/api/flights/${flight.id}/status") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"status":"BOARDING"}"""
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `GET stats daily returns counts`() {
        mockMvc.get("/api/stats/daily")
            .andExpect {
                status { isOk() }
                jsonPath("$.totalFlights") { value(1) }
            }
    }

    @Test
    fun `POST login with valid creds returns token`() {
        mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"username":"admin","password":"admin"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.token") { isNotEmpty() }
            jsonPath("$.username") { value("admin") }
        }
    }

    @Test
    fun `POST login with bad creds returns 401`() {
        mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"username":"admin","password":"wrong"}"""
        }.andExpect {
            status { isUnauthorized() }
        }
    }
}
