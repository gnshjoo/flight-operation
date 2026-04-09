package com.koreanair.ops.alert.service

import com.koreanair.ops.alert.model.Alert
import org.springframework.data.jpa.repository.JpaRepository

interface AlertRepository : JpaRepository<Alert, Long> {
    fun findByAcknowledgedFalseOrderByCreatedAtDesc(): List<Alert>
    fun findAllByOrderByCreatedAtDesc(): List<Alert>
}
