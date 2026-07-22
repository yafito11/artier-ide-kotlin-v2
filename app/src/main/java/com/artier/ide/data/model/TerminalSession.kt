package com.artier.ide.data.model

import java.util.UUID

data class TerminalSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Terminal",
    val workingDirectory: String = "/",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

sealed class TerminalEvent {
    data class Output(val sessionId: String, val data: String) : TerminalEvent()
    data class Input(val sessionId: String, val data: String) : TerminalEvent()
    data class Resize(val sessionId: String, val cols: Int, val rows: Int) : TerminalEvent()
    data class Exit(val sessionId: String, val exitCode: Int) : TerminalEvent()
    data class Error(val sessionId: String, val message: String) : TerminalEvent()
    data class WorkingDirectoryChanged(val sessionId: String, val path: String) : TerminalEvent()
}

data class TerminalState(
    val sessions: List<TerminalSession> = emptyList(),
    val activeSessionId: String? = null,
    val outputBuffer: Map<String, List<String>> = emptyMap()
) {
    val activeSession: TerminalSession?
        get() = sessions.find { it.id == activeSessionId }

    fun getOutput(sessionId: String): String {
        return outputBuffer[sessionId]?.joinToString("") ?: ""
    }
}