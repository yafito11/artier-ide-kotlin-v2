package com.artier.ide.data.repository

import com.artier.ide.data.model.ChatMessage
import com.artier.ide.data.model.ChatSession
import com.artier.ide.data.model.MessageRole
import com.artier.ide.data.remote.WebSocketClient
import com.artier.ide.data.remote.WebSocketEvent
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists chat sessions/messages via daemon SQLite (WebSocket + REST fallback).
 */
@Singleton
class ChatRepository @Inject constructor(
    private val webSocketClient: WebSocketClient
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _sessions = MutableStateFlow<List<ChatSession>>(emptyList())
    val sessions: StateFlow<List<ChatSession>> = _sessions.asStateFlow()

    init {
        scope.launch {
            webSocketClient.events.collect { event ->
                when (event) {
                    is WebSocketEvent.DbChatSessions -> {
                        _sessions.value = event.sessions.mapNotNull { parseSession(it) }
                    }
                    is WebSocketEvent.DbChatMessages -> {
                        val msgs = event.messages.mapNotNull { parseMessage(it) }
                        _sessions.value = _sessions.value.map { session ->
                            if (session.id == event.sessionId) {
                                session.copy(messages = msgs)
                            } else session
                        }
                    }
                    is WebSocketEvent.DbChatSessionCreated -> {
                        // refresh list
                        loadSessions()
                    }
                    is WebSocketEvent.DbChatMessageAdded -> {
                        // no-op; local state already updated optimistically
                    }
                    else -> Unit
                }
            }
        }
    }

    fun loadSessions() {
        webSocketClient.send("db_chat_get_sessions", JsonObject())
    }

    fun loadMessages(sessionId: String) {
        val data = JsonObject().apply {
            addProperty("sessionId", sessionId)
            addProperty("limit", 200)
        }
        webSocketClient.send("db_chat_get_messages", data)
    }

    fun createSession(session: ChatSession) {
        val data = JsonObject().apply {
            addProperty("id", session.id)
            addProperty("title", session.title)
            addProperty("agentName", session.agentName)
        }
        webSocketClient.send("db_chat_create_session", data)

        // Optimistic local update
        _sessions.value = listOf(session) + _sessions.value.filter { it.id != session.id }
    }

    fun addMessage(sessionId: String, message: ChatMessage) {
        val data = JsonObject().apply {
            addProperty("id", message.id)
            addProperty("sessionId", sessionId)
            addProperty("role", message.role.name.lowercase())
            addProperty("content", message.content)
            if (message.agentName != null) {
                addProperty("agentName", message.agentName)
            }
        }
        webSocketClient.send("db_chat_add_message", data)

        _sessions.value = _sessions.value.map { session ->
            if (session.id == sessionId) {
                session.copy(
                    messages = session.messages + message,
                    updatedAt = System.currentTimeMillis()
                )
            } else session
        }
    }

    fun deleteSession(sessionId: String) {
        val data = JsonObject().apply {
            addProperty("sessionId", sessionId)
        }
        webSocketClient.send("db_chat_delete_session", data)
        _sessions.value = _sessions.value.filter { it.id != sessionId }
    }

    fun upsertLocalSession(session: ChatSession) {
        _sessions.value = _sessions.value
            .filter { it.id != session.id } + session
    }

    fun updateLocalMessages(sessionId: String, messages: List<ChatMessage>) {
        _sessions.value = _sessions.value.map { session ->
            if (session.id == sessionId) {
                session.copy(messages = messages, updatedAt = System.currentTimeMillis())
            } else session
        }
    }

    private fun parseSession(obj: JsonObject): ChatSession? {
        return try {
            ChatSession(
                id = obj.get("id")?.asString ?: return null,
                title = obj.get("title")?.asString ?: "Chat",
                agentName = obj.get("agentName")?.asString
                    ?: obj.get("agent_name")?.asString
                    ?: "tool-loop",
                createdAt = obj.get("createdAt")?.asLong
                    ?: obj.get("created_at")?.asLong
                    ?: System.currentTimeMillis(),
                updatedAt = obj.get("updatedAt")?.asLong
                    ?: obj.get("updated_at")?.asLong
                    ?: System.currentTimeMillis(),
                isActive = when {
                    obj.has("isActive") -> obj.get("isActive").asBoolean
                    obj.has("is_active") -> obj.get("is_active").asInt == 1
                    else -> true
                }
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun parseMessage(obj: JsonObject): ChatMessage? {
        return try {
            val roleStr = obj.get("role")?.asString?.uppercase() ?: "ASSISTANT"
            val role = try {
                MessageRole.valueOf(roleStr)
            } catch (_: Exception) {
                MessageRole.ASSISTANT
            }
            ChatMessage(
                id = obj.get("id")?.asString ?: return null,
                role = role,
                content = obj.get("content")?.asString ?: "",
                agentName = obj.get("agentName")?.asString ?: obj.get("agent_name")?.asString,
                timestamp = obj.get("timestamp")?.asLong ?: System.currentTimeMillis()
            )
        } catch (_: Exception) {
            null
        }
    }
}
