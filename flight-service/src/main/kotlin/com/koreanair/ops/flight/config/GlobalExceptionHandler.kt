package com.koreanair.ops.flight.config

import com.koreanair.ops.flight.controller.UnauthorizedException
import com.koreanair.ops.flight.service.FlightNotFoundException
import com.koreanair.ops.flight.service.InvalidStatusTransitionException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(FlightNotFoundException::class)
    fun handleNotFound(ex: FlightNotFoundException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse("NOT_FOUND", ex.message ?: "Not found"))

    @ExceptionHandler(InvalidStatusTransitionException::class)
    fun handleInvalidTransition(ex: InvalidStatusTransitionException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse("INVALID_TRANSITION", ex.message ?: "Invalid status transition"))

    @ExceptionHandler(UnauthorizedException::class)
    fun handleUnauthorized(ex: UnauthorizedException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ErrorResponse("UNAUTHORIZED", ex.message ?: "Unauthorized"))
}

data class ErrorResponse(val code: String, val message: String)
