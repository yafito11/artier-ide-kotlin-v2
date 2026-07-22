package com.artier.ide.data.remote

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class WebSocketClient @Inject constructor() {
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var reconnectJob: Job? = null

    private val _events = Channel<WebSocketEvent>(Channel.BUFFERED)
    val events: Flow<WebSocketEvent> = _events.receiveAsFlow()

    private val messageHandlers = ConcurrentHashMap<String, CopyOnWriteArrayList<(JSONObject) -> Unit>>()

    private var isConnected = false
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5
    private var currentUrl: String? = null

    open fun connect(url: String) {
        currentUrl = url
        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected = true
                reconnectAttempts = 0
                _events.trySend(WebSocketEvent.Connected)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val orgJson = JSONObject(text)
                    val type = orgJson.optString("type")
                    val payload = orgJson.optJSONObject("payload")

                    if (type.isNotBlank() && payload != null) {
                        val handlers = messageHandlers[type]
                        if (handlers != null && handlers.isNotEmpty()) {
                            for (handler in handlers) {
                                try {
                                    handler.invoke(payload)
                                } catch (e: Exception) {
                                    android.util.Log.w("WebSocketClient", "Handler error for $type", e)
                                }
                            }
                        }
                    }

                    val gsonJson = JsonParser.parseString(text).asJsonObject
                    val event = parseEvent(gsonJson)
                    _events.trySend(event)
                } catch (e: Exception) {
                    _events.trySend(WebSocketEvent.Error("Failed to parse message: ${e.message}"))
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
                isConnected = false
                _events.trySend(WebSocketEvent.Disconnected)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isConnected = false
                _events.trySend(WebSocketEvent.Disconnected)
                attemptReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                _events.trySend(WebSocketEvent.Error(t.message ?: "Unknown error"))
                attemptReconnect()
            }
        })
    }

    private fun attemptReconnect() {
        val url = currentUrl ?: return
        if (reconnectAttempts < maxReconnectAttempts) {
            reconnectAttempts++
            val delayMs = (reconnectAttempts * 1000).toLong()
            reconnectJob?.cancel()
            reconnectJob = scope.launch {
                delay(delayMs)
                if (!isConnected) {
                    connect(url)
                }
            }
        }
    }

    fun addMessageHandler(type: String, handler: (JSONObject) -> Unit) {
        messageHandlers.getOrPut(type) { CopyOnWriteArrayList() }.add(handler)
    }

    fun removeMessageHandler(type: String, handler: (JSONObject) -> Unit) {
        messageHandlers[type]?.remove(handler)
    }

    fun removeMessageHandlers(type: String) {
        messageHandlers.remove(type)
    }

    fun send(message: JSONObject) {
        webSocket?.send(message.toString())
    }

    fun send(type: String, data: JsonObject) {
        val message = JsonObject().apply {
            addProperty("type", type)
            add("payload", data)
        }
        webSocket?.send(gson.toJson(message))
    }

    fun sendRaw(message: String) {
        webSocket?.send(message)
    }

    open fun disconnect() {
        reconnectJob?.cancel()
        reconnectJob = null
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        isConnected = false
        currentUrl = null
        reconnectAttempts = 0
    }

    fun isConnected(): Boolean = isConnected

    private fun parseEvent(json: JsonObject): WebSocketEvent {
        val type = json.get("type")?.asString ?: return WebSocketEvent.Unknown
        val payload = json.get("payload")?.asJsonObject ?: json.get("data")?.asJsonObject

        return when (type) {
            "daemon_ready" -> WebSocketEvent.DaemonReady(
                version = payload?.get("version")?.asString ?: "",
                features = payload?.get("features")?.asJsonArray?.map { it.asString } ?: emptyList(),
                cloudflaredAvailable = payload?.get("cloudflaredAvailable")?.asBoolean ?: false
            )
            "terminal_output" -> WebSocketEvent.TerminalOutput(
                sessionId = payload?.get("sessionId")?.asString ?: "",
                data = payload?.get("data")?.asString ?: ""
            )
            "terminal_exit" -> WebSocketEvent.TerminalExit(
                sessionId = payload?.get("sessionId")?.asString ?: "",
                exitCode = payload?.get("exitCode")?.asInt ?: -1
            )
            "terminal_created" -> WebSocketEvent.TerminalCreated(
                sessionId = payload?.get("sessionId")?.asString ?: "",
                shell = payload?.get("shell")?.asString ?: ""
            )
            "file_content" -> WebSocketEvent.FileContent(
                path = payload?.get("path")?.asString ?: "",
                content = payload?.get("content")?.asString ?: ""
            )
            "file_saved" -> WebSocketEvent.FileSaved(
                path = payload?.get("path")?.asString ?: ""
            )
            "tunnel_created" -> WebSocketEvent.TunnelCreated(
                tunnelId = payload?.get("tunnelId")?.asString ?: "",
                url = payload?.get("url")?.asString ?: "",
                port = payload?.get("port")?.asInt ?: 0
            )
            "tunnel_closed" -> WebSocketEvent.TunnelClosed(
                tunnelId = payload?.get("tunnelId")?.asString ?: "",
                exitCode = payload?.get("exitCode")?.asInt
            )
            "tunnel_status" -> WebSocketEvent.TunnelStatus(
                tunnelId = payload?.get("tunnelId")?.asString ?: "",
                status = payload?.get("status")?.asString ?: "",
                url = payload?.get("url")?.asString,
                port = payload?.get("port")?.asInt ?: 0,
                service = payload?.get("service")?.asString ?: "",
                uptime = payload?.get("uptime")?.asInt ?: 0
            )
            "tunnel_download_progress" -> WebSocketEvent.TunnelDownloadProgress(
                status = payload?.get("status")?.asString ?: "",
                downloaded = payload?.get("downloaded")?.asLong ?: 0,
                total = payload?.get("total")?.asLong ?: 0,
                percent = payload?.get("percent")?.asInt ?: 0
            )
            "tunnel_download_complete" -> WebSocketEvent.TunnelDownloadComplete(
                path = payload?.get("path")?.asString ?: ""
            )
            "ports_detected" -> {
                val portsArray = payload?.get("ports")?.asJsonArray
                val ports = portsArray?.map { portJson ->
                    val portObj = portJson.asJsonObject
                    com.artier.ide.data.model.DetectedPort(
                        port = portObj.get("port")?.asInt ?: 0,
                        service = portObj.get("service")?.asString ?: ""
                    )
                } ?: emptyList()
                WebSocketEvent.PortsDetected(ports)
            }
            "db_chat_session_created" -> WebSocketEvent.DbChatSessionCreated(
                sessionId = payload?.get("session")?.asJsonObject?.get("id")?.asString ?: ""
            )
            "db_chat_sessions" -> WebSocketEvent.DbChatSessions(
                sessions = payload?.get("sessions")?.asJsonArray?.map { it.asJsonObject } ?: emptyList()
            )
            "db_chat_message_added" -> WebSocketEvent.DbChatMessageAdded(
                messageId = payload?.get("message")?.asJsonObject?.get("id")?.asString ?: ""
            )
            "db_chat_messages" -> WebSocketEvent.DbChatMessages(
                sessionId = payload?.get("sessionId")?.asString ?: "",
                messages = payload?.get("messages")?.asJsonArray?.map { it.asJsonObject } ?: emptyList()
            )
            "db_connected" -> WebSocketEvent.DbConnected(
                connectionId = payload?.get("connectionId")?.asString ?: "",
                connectionType = payload?.get("type")?.asString ?: ""
            )
            "db_disconnected" -> WebSocketEvent.DbDisconnected(
                connectionId = payload?.get("connectionId")?.asString ?: ""
            )
            "db_query_result" -> WebSocketEvent.DbQueryResult(
                connectionId = payload?.get("connectionId")?.asString ?: "",
                rows = payload?.get("rows")?.asJsonArray?.map { row ->
                    val rowObj = row.asJsonObject
                    val map = mutableMapOf<String, Any?>()
                    for (key in rowObj.keySet()) {
                        map[key] = rowObj.get(key)
                    }
                    map
                } ?: emptyList(),
                rowCount = payload?.get("rowCount")?.asInt ?: 0,
                fields = payload?.get("fields")?.asJsonArray?.map { it.asString } ?: emptyList(),
                duration = payload?.get("duration")?.asLong ?: 0,
                error = payload?.get("error")?.asString
            )
            "db_tables" -> WebSocketEvent.DbTables(
                connectionId = payload?.get("connectionId")?.asString ?: "",
                tables = payload?.get("tables")?.asJsonArray?.map { it.asString } ?: emptyList()
            )
            "db_schema" -> WebSocketEvent.DbSchema(
                connectionId = payload?.get("connectionId")?.asString ?: "",
                tableName = payload?.get("tableName")?.asString ?: "",
                columns = payload?.get("columns")?.asJsonArray?.map { col ->
                    val colObj = col.asJsonObject
                    com.artier.ide.data.model.ColumnInfo(
                        name = colObj.get("name")?.asString ?: "",
                        type = colObj.get("type")?.asString ?: "",
                        nullable = colObj.get("nullable")?.asBoolean ?: true,
                        isPrimaryKey = colObj.get("isPrimaryKey")?.asBoolean ?: false
                    )
                } ?: emptyList()
            )
            "skill_list_result" -> WebSocketEvent.SkillListResult(
                skills = payload?.get("skills")?.asJsonArray?.map { skillJson ->
                    val skillObj = skillJson.asJsonObject
                    com.artier.ide.data.model.SkillInfo(
                        name = skillObj.get("name")?.asString ?: "",
                        description = skillObj.get("description")?.asString ?: "",
                        path = skillObj.get("path")?.asString ?: ""
                    )
                } ?: emptyList()
            )
            "skill_detail_result" -> WebSocketEvent.SkillDetailResult(
                name = payload?.get("name")?.asString ?: "",
                content = payload?.get("content")?.asString ?: "",
                metadata = payload?.get("metadata")?.asJsonObject
            )
            "agent_status" -> WebSocketEvent.AgentStatusUpdate(
                sessionId = payload?.get("sessionId")?.asString ?: "",
                status = payload?.get("status")?.asString ?: "",
                agentType = payload?.get("agentType")?.asString
            )
            "agent_output" -> WebSocketEvent.AgentOutput(
                sessionId = payload?.get("sessionId")?.asString ?: "",
                content = payload?.get("content")?.asString ?: "",
                toolCalls = payload?.get("toolCalls")?.asJsonArray?.map { tc ->
                    val tcObj = tc.asJsonObject
                    com.artier.ide.data.model.ToolCallInfo(
                        id = tcObj.get("id")?.asString ?: "",
                        name = tcObj.get("name")?.asString ?: "",
                        arguments = tcObj.get("input")?.asString ?: tcObj.get("arguments")?.asString ?: "",
                        result = tcObj.get("output")?.asString ?: tcObj.get("result")?.asString
                    )
                } ?: emptyList()
            )
            "agent_error" -> WebSocketEvent.AgentError(
                sessionId = payload?.get("sessionId")?.asString ?: "",
                message = payload?.get("message")?.asString ?: "Unknown error"
            )
            "router_status" -> WebSocketEvent.RouterStatusEvent(
                isRunning = payload?.get("isRunning")?.asBoolean ?: false,
                port = payload?.get("port")?.asInt ?: 0
            )
            "router_config" -> WebSocketEvent.RouterConfigEvent(
                config = payload
            )
            "error" -> WebSocketEvent.Error(
                message = payload?.get("message")?.asString ?: "Unknown error"
            )
            else -> WebSocketEvent.Unknown
        }
    }
}

sealed class WebSocketEvent {
    object Connected : WebSocketEvent()
    object Disconnected : WebSocketEvent()
    data class DaemonReady(
        val version: String,
        val features: List<String>,
        val cloudflaredAvailable: Boolean
    ) : WebSocketEvent()
    data class TerminalOutput(val sessionId: String, val data: String) : WebSocketEvent()
    data class TerminalExit(val sessionId: String, val exitCode: Int) : WebSocketEvent()
    data class TerminalCreated(val sessionId: String, val shell: String) : WebSocketEvent()
    data class FileContent(val path: String, val content: String) : WebSocketEvent()
    data class FileSaved(val path: String) : WebSocketEvent()
    data class TunnelCreated(val tunnelId: String, val url: String, val port: Int) : WebSocketEvent()
    data class TunnelClosed(val tunnelId: String, val exitCode: Int? = null) : WebSocketEvent()
    data class TunnelStatus(
        val tunnelId: String,
        val status: String,
        val url: String?,
        val port: Int,
        val service: String,
        val uptime: Int
    ) : WebSocketEvent()
    data class TunnelDownloadProgress(
        val status: String,
        val downloaded: Long,
        val total: Long,
        val percent: Int
    ) : WebSocketEvent()
    data class TunnelDownloadComplete(val path: String) : WebSocketEvent()
    data class PortsDetected(val ports: List<com.artier.ide.data.model.DetectedPort>) : WebSocketEvent()
    data class DbChatSessionCreated(val sessionId: String) : WebSocketEvent()
    data class DbChatSessions(val sessions: List<JsonObject>) : WebSocketEvent()
    data class DbChatMessageAdded(val messageId: String) : WebSocketEvent()
    data class DbChatMessages(val sessionId: String, val messages: List<JsonObject>) : WebSocketEvent()
    data class DbConnected(val connectionId: String, val connectionType: String) : WebSocketEvent()
    data class DbDisconnected(val connectionId: String) : WebSocketEvent()
    data class DbQueryResult(
        val connectionId: String,
        val rows: List<Map<String, Any?>>,
        val rowCount: Int,
        val fields: List<String>,
        val duration: Long,
        val error: String? = null
    ) : WebSocketEvent()
    data class DbTables(val connectionId: String, val tables: List<String>) : WebSocketEvent()
    data class DbSchema(
        val connectionId: String,
        val tableName: String,
        val columns: List<com.artier.ide.data.model.ColumnInfo>
    ) : WebSocketEvent()
    data class SkillListResult(val skills: List<com.artier.ide.data.model.SkillInfo>) : WebSocketEvent()
    data class SkillDetailResult(
        val name: String,
        val content: String,
        val metadata: JsonObject? = null
    ) : WebSocketEvent()
    data class AgentStatusUpdate(
        val sessionId: String,
        val status: String,
        val agentType: String? = null
    ) : WebSocketEvent()
    data class AgentOutput(
        val sessionId: String,
        val content: String,
        val toolCalls: List<com.artier.ide.data.model.ToolCallInfo> = emptyList()
    ) : WebSocketEvent()
    data class AgentError(val sessionId: String, val message: String) : WebSocketEvent()
    data class RouterStatusEvent(val isRunning: Boolean, val port: Int) : WebSocketEvent()
    data class RouterConfigEvent(val config: JsonObject? = null) : WebSocketEvent()
    data class Error(val message: String) : WebSocketEvent()
    object Unknown : WebSocketEvent()
}
