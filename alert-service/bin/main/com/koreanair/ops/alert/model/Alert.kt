package com.koreanair.ops.alert.model

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "alerts")
class Alert(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val flightId: Long,

    @Column(nullable = false, length = 10)
    val flightNumber: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val type: AlertType,

    @Column(nullable = false, length = 500)
    val message: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    val severity: Severity,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(nullable = false)
    var acknowledged: Boolean = false
)

enum class AlertType {
    DELAY, CANCELLATION, GATE_CHANGE, STATUS_CHANGE
}

enum class Severity {
    INFO, WARNING, CRITICAL
}
