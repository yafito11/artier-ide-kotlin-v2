package com.artier.ide.data.model

import java.util.UUID

/**
 * Chat message model
 */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: MessageRole,
    val content: String,
    val agentName: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false,
    val toolCalls: List<ToolCallInfo> = emptyList(),
    val isError: Boolean = false
)

enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM,
    TOOL
}

data class ToolCallInfo(
    val id: String,
    val name: String,
    val arguments: String,
    val result: String? = null
)

/**
 * Chat session model
 */
data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val agentName: String,
    val messages: List<ChatMessage> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

/**
 * Agent status
 */
data class AgentStatus(
    val name: String,
    val isAvailable: Boolean,
    val isActive: Boolean = false,
    val currentSessionId: String? = null
)

/**
 * AI Assistant state
 */
data class AiState(
    val sessions: List<ChatSession> = emptyList(),
    val activeSessionId: String? = null,
    val availableAgents: List<AgentStatus> = emptyList(),
    val selectedAgent: String? = null,
    val isProcessing: Boolean = false,
    val error: String? = null
) {
    val activeSession: ChatSession?
        get() = sessions.find { it.id == activeSessionId }
    
    val messages: List<ChatMessage>
        get() = activeSession?.messages ?: emptyList()
}

sealed class AiEvent {
    data class MessageAdded(val message: ChatMessage) : AiEvent()
    data class MessageUpdated(val message: ChatMessage) : AiEvent()
    data class SessionCreated(val session: ChatSession) : AiEvent()
    data class SessionDeleted(val sessionId: String) : AiEvent()
    data class AgentChanged(val agentName: String) : AiEvent()
    data class Error(val message: String) : AiEvent()
    data class ProcessingStarted(val sessionId: String) : AiEvent()
    data class ProcessingCompleted(val sessionId: String) : AiEvent()
}