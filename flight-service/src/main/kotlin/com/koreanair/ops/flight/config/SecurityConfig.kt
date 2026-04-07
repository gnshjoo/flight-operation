package com.koreanair.ops.flight.config

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.filter.OncePerRequestFilter
import java.util.*
import javax.crypto.SecretKey

@Configuration
class SecurityConfig(
    @Value("\${app.jwt.secret:koreanair-ops-dashboard-secret-key-for-demo-2024}")
    private val jwtSecret: String
) {
    private val key: SecretKey = Keys.hmacShaKeyFor(jwtSecret.toByteArray())

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .cors { cors ->
                cors.configurationSource {
                    CorsConfiguration().apply {
                        allowedOrigins = listOf("*")
                        allowedMethods = listOf("*")
                        allowedHeaders = listOf("*")
                    }
                }
            }
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers("/actuator/**").permitAll()
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/flights").authenticated()
                    .requestMatchers(HttpMethod.PUT, "/api/flights/*/status").authenticated()
                    .requestMatchers(HttpMethod.PUT, "/api/flights/*/gate").authenticated()
                    .anyRequest().permitAll()
            }
            .addFilterBefore(JwtAuthFilter(key), UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun jwtKey(): SecretKey = key
}

class JwtAuthFilter(private val key: SecretKey) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val header = request.getHeader("Authorization")
        if (header != null && header.startsWith("Bearer ")) {
            try {
                val token = header.substring(7)
                val claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload
                val auth = UsernamePasswordAuthenticationToken(
                    claims.subject,
                    null,
                    listOf(SimpleGrantedAuthority("ROLE_ADMIN"))
                )
                SecurityContextHolder.getContext().authentication = auth
            } catch (_: Exception) {
                // Invalid token, continue without auth
            }
        }
        filterChain.doFilter(request, response)
    }
}
