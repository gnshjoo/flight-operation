package com.koreanair.ops.flight.controller

import io.jsonwebtoken.Jwts
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.crypto.SecretKey

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Authentication (demo: admin/admin)")
class AuthController(private val jwtKey: SecretKey) {

    @PostMapping("/login")
    @Operation(summary = "Login and get JWT token (demo: admin/admin)")
    fun login(@RequestBody request: LoginRequest): LoginResponse {
        if (request.username == "admin" && request.password == "admin") {
            val token = Jwts.builder()
                .subject(request.username)
                .issuedAt(Date())
                .expiration(Date(System.currentTimeMillis() + 86400000)) // 24h
                .signWith(jwtKey)
                .compact()
            return LoginResponse(token = token, username = request.username)
        }
        throw UnauthorizedException()
    }
}

data class LoginRequest(val username: String, val password: String)
data class LoginResponse(val token: String, val username: String)
class UnauthorizedException : RuntimeException("Invalid credentials")
