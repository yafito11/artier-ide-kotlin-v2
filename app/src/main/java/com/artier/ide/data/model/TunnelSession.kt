package com.artier.ide.data.model

import java.util.UUID

data class TunnelSession(
    val id: String = UUID.randomUUID().toString(),
    val port: Int,
    val service: String = "http",
    val url: String? = null,
    val status: TunnelStatus = TunnelStatus.Pending,
    val createdAt: Long = System.currentTimeMillis(),
    val lastActivity: Long = System.currentTimeMillis()
)

enum class TunnelStatus {
    Pending,
    Active,
    Error,
    Closed
}

sealed class TunnelEvent {
    data class TunnelCreated(
        val tunnelId: String,
        val url: String,
        val port: Int
    ) : TunnelEvent()

    data class TunnelClosed(
        val tunnelId: String,
        val exitCode: Int? = null
    ) : TunnelEvent()

    data class TunnelStatus(
        val tunnelId: String,
        val status: String,
        val url: String?,
        val port: Int,
        val service: String,
        val uptime: Int
    ) : TunnelEvent()

    data class PortsDetected(
        val ports: List<DetectedPort>
    ) : TunnelEvent()

    data class Error(
        val message: String
    ) : TunnelEvent()
}

data class DetectedPort(
    val port: Int,
    val service: String
)

data class TunnelState(
    val sessions: List<TunnelSession> = emptyList(),
    val activeSessionId: String? = null,
    val detectedPorts: List<DetectedPort> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val activeSession: TunnelSession?
        get() = sessions.find { it.id == activeSessionId }

    val hasActiveTunnel: Boolean
        get() = sessions.any { it.status == TunnelStatus.Active }
}