package com.koreanair.ops.alert.config

import com.koreanair.ops.alert.service.AlertNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(AlertNotFoundException::class)
    fun handleNotFound(ex: AlertNotFoundException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(mapOf("code" to "NOT_FOUND", "message" to (ex.message ?: "Not found")))
}
