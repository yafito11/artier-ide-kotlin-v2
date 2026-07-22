package com.artier.ide.data.remote

import com.artier.ide.data.model.*
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * High-level API for daemon communication.
 * Wraps WebSocketClient with typed methods for all daemon operations.
 */
@Singleton
open class DaemonApi @Inject constructor(
    private val webSocketClient: WebSocketClient
) {
    private val gson = Gson()

    /**
     * Flow of daemon events
     */
    val events: Flow<DaemonEvent> = webSocketClient.events.map { event ->
        when (event) {
            is WebSocketEvent.Connected -> DaemonEvent.Connected
            is WebSocketEvent.Disconnected -> DaemonEvent.Disconnected
            is WebSocketEvent.DaemonReady -> DaemonEvent.DaemonReady(
                version = event.version,
                features = event.features,
                cloudflaredAvailable = event.cloudflaredAvailable
            )
            is WebSocketEvent.TerminalOutput -> DaemonEvent.TerminalOutput(
                sessionId = event.sessionId,
                data = event.data
            )
            is WebSocketEvent.TerminalExit -> DaemonEvent.TerminalExit(
                sessionId = event.sessionId,
                exitCode = event.exitCode
            )
            is WebSocketEvent.TerminalCreated -> DaemonEvent.TerminalCreated(
                sessionId = event.sessionId,
                shell = event.shell
            )
            is WebSocketEvent.FileContent -> DaemonEvent.FileContent(
                path = event.path,
                content = event.content
            )
            is WebSocketEvent.FileSaved -> DaemonEvent.FileSaved(
                path = event.path
            )
            is WebSocketEvent.TunnelCreated -> DaemonEvent.TunnelCreated(
                tunnelId = event.tunnelId,
                url = event.url,
                port = event.port
            )
            is WebSocketEvent.TunnelClosed -> DaemonEvent.TunnelClosed(
                tunnelId = event.tunnelId,
                exitCode = event.exitCode
            )
            is WebSocketEvent.TunnelStatus -> DaemonEvent.TunnelStatus(
                tunnelId = event.tunnelId,
                status = event.status,
                url = event.url,
                port = event.port,
                service = event.service,
                uptime = event.uptime
            )
            is WebSocketEvent.TunnelDownloadProgress -> DaemonEvent.TunnelDownloadProgress(
                status = event.status,
                downloaded = event.downloaded,
                total = event.total,
                percent = event.percent
            )
            is WebSocketEvent.TunnelDownloadComplete -> DaemonEvent.TunnelDownloadComplete(
                path = event.path
            )
            is WebSocketEvent.PortsDetected -> DaemonEvent.PortsDetected(
                ports = event.ports
            )
            is WebSocketEvent.DbConnected -> DaemonEvent.DbConnected(
                connectionId = event.connectionId,
                connectionType = event.connectionType
            )
            is WebSocketEvent.DbDisconnected -> DaemonEvent.DbDisconnected(
                connectionId = event.connectionId
            )
            is WebSocketEvent.DbQueryResult -> DaemonEvent.DbQueryResult(
                connectionId = event.connectionId,
                rows = event.rows,
                rowCount = event.rowCount,
                fields = event.fields,
                duration = event.duration,
                error = event.error
            )
            is WebSocketEvent.DbTables -> DaemonEvent.DbTables(
                connectionId = event.connectionId,
                tables = event.tables
            )
            is WebSocketEvent.DbSchema -> DaemonEvent.DbSchema(
                connectionId = event.connectionId,
                tableName = event.tableName,
                columns = event.columns
            )
            is WebSocketEvent.DbChatSessionCreated -> DaemonEvent.DbChatSessionCreated(
                sessionId = event.sessionId
            )
            is WebSocketEvent.DbChatSessions -> DaemonEvent.DbChatSessions(
                sessions = event.sessions
            )
            is WebSocketEvent.DbChatMessageAdded -> DaemonEvent.DbChatMessageAdded(
                messageId = event.messageId
            )
            is WebSocketEvent.DbChatMessages -> DaemonEvent.DbChatMessages(
                sessionId = event.sessionId,
                messages = event.messages
            )
            is WebSocketEvent.SkillListResult -> DaemonEvent.SkillListResult(
                skills = event.skills
            )
            is WebSocketEvent.SkillDetailResult -> DaemonEvent.SkillDetailResult(
                name = event.name,
                content = event.content,
                metadata = event.metadata
            )
            is WebSocketEvent.AgentStatusUpdate -> DaemonEvent.AgentStatusUpdate(
                sessionId = event.sessionId,
                status = event.status,
                agentType = event.agentType
            )
            is WebSocketEvent.AgentOutput -> DaemonEvent.AgentOutput(
                sessionId = event.sessionId,
                content = event.content,
                toolCalls = event.toolCalls
            )
            is WebSocketEvent.AgentError -> DaemonEvent.AgentError(
                sessionId = event.sessionId,
                message = event.message
            )
            is WebSocketEvent.RouterStatusEvent -> DaemonEvent.RouterStatusEvent(
                isRunning = event.isRunning,
                port = event.port
            )
            is WebSocketEvent.RouterConfigEvent -> DaemonEvent.RouterConfigEvent(
                config = event.config
            )
            is WebSocketEvent.Error -> DaemonEvent.Error(
                message = event.message
            )
            is WebSocketEvent.Unknown -> DaemonEvent.Unknown
        }
    }

    // ==================== Connection ====================

    fun connect(daemonUrl: String = "ws://127.0.0.1:8080/ws") {
        webSocketClient.connect(daemonUrl)
    }

    fun disconnect() {
        webSocketClient.disconnect()
    }

    fun isConnected(): Boolean = webSocketClient.isConnected()

    // ==================== Terminal Commands ====================

    fun createTerminalSession(workingDirectory: String = "/") {
        val data = JsonObject().apply {
            addProperty("workingDirectory", workingDirectory)
        }
        webSocketClient.send("terminal_create", data)
    }

    fun sendTerminalInput(sessionId: String, input: String) {
        val data = JsonObject().apply {
            addProperty("sessionId", sessionId)
            addProperty("data", input)
        }
        webSocketClient.send("terminal_input", data)
    }

    fun resizeTerminal(sessionId: String, cols: Int, rows: Int) {
        val data = JsonObject().apply {
            addProperty("sessionId", sessionId)
            addProperty("cols", cols)
            addProperty("rows", rows)
        }
        webSocketClient.send("terminal_resize", data)
    }

    fun closeTerminalSession(sessionId: String) {
        val data = JsonObject().apply {
            addProperty("sessionId", sessionId)
        }
        webSocketClient.send("terminal_close", data)
    }

    // ==================== File Commands ====================

    fun readFile(path: String) {
        val data = JsonObject().apply {
            addProperty("path", path)
        }
        webSocketClient.send("file_read", data)
    }

    fun writeFile(path: String, content: String) {
        val data = JsonObject().apply {
            addProperty("path", path)
            addProperty("content", content)
        }
        webSocketClient.send("file_write", data)
    }

    fun listDirectory(path: String) {
        val data = JsonObject().apply {
            addProperty("path", path)
        }
        webSocketClient.send("directory_list", data)
    }

    fun createFile(path: String) {
        val data = JsonObject().apply {
            addProperty("path", path)
        }
        webSocketClient.send("file_create", data)
    }

    fun createDirectory(path: String) {
        val data = JsonObject().apply {
            addProperty("path", path)
        }
        webSocketClient.send("directory_create", data)
    }

    fun deleteFile(path: String) {
        val data = JsonObject().apply {
            addProperty("path", path)
        }
        webSocketClient.send("file_delete", data)
    }

    fun renameFile(oldPath: String, newPath: String) {
        val data = JsonObject().apply {
            addProperty("oldPath", oldPath)
            addProperty("newPath", newPath)
        }
        webSocketClient.send("file_rename", data)
    }

    // ==================== Database Commands ====================

    fun connectDatabase(
        type: DbType,
        host: String = "",
        port: Int = 0,
        database: String = "",
        username: String = "",
        password: String = ""
    ) {
        val data = JsonObject().apply {
            addProperty("type", type.name.lowercase())
            addProperty("host", host)
            addProperty("port", port)
            addProperty("database", database)
            addProperty("username", username)
            addProperty("password", password)
        }
        webSocketClient.send("db_connect", data)
    }

    fun disconnectDatabase(connectionId: String) {
        val data = JsonObject().apply {
            addProperty("connectionId", connectionId)
        }
        webSocketClient.send("db_disconnect", data)
    }

    fun queryDatabase(connectionId: String, sql: String) {
        val data = JsonObject().apply {
            addProperty("connectionId", connectionId)
            addProperty("sql", sql)
        }
        webSocketClient.send("db_query", data)
    }

    fun getDatabaseTables(connectionId: String) {
        val data = JsonObject().apply {
            addProperty("connectionId", connectionId)
        }
        webSocketClient.send("db_tables", data)
    }

    fun getTableSchema(connectionId: String, tableName: String) {
        val data = JsonObject().apply {
            addProperty("connectionId", connectionId)
            addProperty("tableName", tableName)
        }
        webSocketClient.send("db_schema", data)
    }

    // ==================== Tunnel Commands ====================

    fun createTunnel(port: Int, service: String = "http") {
        val data = JsonObject().apply {
            addProperty("port", port)
            addProperty("service", service)
        }
        webSocketClient.send("tunnel_create", data)
    }

    fun closeTunnel(tunnelId: String) {
        val data = JsonObject().apply {
            addProperty("tunnelId", tunnelId)
        }
        webSocketClient.send("tunnel_close", data)
    }

    fun getTunnelStatus(tunnelId: String) {
        val data = JsonObject().apply {
            addProperty("tunnelId", tunnelId)
        }
        webSocketClient.send("tunnel_status", data)
    }

    fun detectPorts(service: String = "all") {
        val data = JsonObject().apply {
            addProperty("service", service)
        }
        webSocketClient.send("port_detect", data)
    }

    // ==================== Agent Commands ====================

    fun runAgent(agent: String, prompt: String) {
        val data = JsonObject().apply {
            addProperty("agent", agent)
            addProperty("prompt", prompt)
        }
        webSocketClient.send("agent_run", data)
    }

    fun getAgentStatus(sessionId: String) {
        val data = JsonObject().apply {
            addProperty("sessionId", sessionId)
        }
        webSocketClient.send("agent_status", data)
    }

    fun stopAgent(sessionId: String) {
        val data = JsonObject().apply {
            addProperty("sessionId", sessionId)
        }
        webSocketClient.send("agent_stop", data)
    }

    fun sendAgentInput(sessionId: String, input: String) {
        val data = JsonObject().apply {
            addProperty("sessionId", sessionId)
            addProperty("input", input)
        }
        webSocketClient.send("agent_input", data)
    }

    // ==================== Skill Commands ====================

    fun getSkills() {
        webSocketClient.send("skill_list", JsonObject())
    }

    fun getSkillDetail(name: String) {
        val data = JsonObject().apply {
            addProperty("name", name)
        }
        webSocketClient.send("skill_get", data)
    }

    fun installSkill(path: String) {
        val data = JsonObject().apply {
            addProperty("path", path)
        }
        webSocketClient.send("skill_install", data)
    }

    fun uninstallSkill(name: String) {
        val data = JsonObject().apply {
            addProperty("name", name)
        }
        webSocketClient.send("skill_uninstall", data)
    }

    fun scanSkills() {
        webSocketClient.send("skill_scan", JsonObject())
    }

    fun setSkillEnabled(name: String, enabled: Boolean) {
        val data = JsonObject().apply {
            addProperty("name", name)
            addProperty("enabled", enabled)
        }
        webSocketClient.send("skill_set_enabled", data)
    }

    // ==================== Router Commands ====================

    fun getRouterStatus() {
        webSocketClient.send("router_status", JsonObject())
    }

    fun getRouterConfig() {
        webSocketClient.send("router_config", JsonObject())
    }

    fun updateRouterConfig(config: JsonObject) {
        webSocketClient.send("router_config_update", config)
    }
}

/**
 * High-level daemon events
 */
sealed class DaemonEvent {
    object Connected : DaemonEvent()
    object Disconnected : DaemonEvent()
    data class DaemonReady(
        val version: String,
        val features: List<String>,
        val cloudflaredAvailable: Boolean
    ) : DaemonEvent()
    
    // Terminal events
    data class TerminalOutput(val sessionId: String, val data: String) : DaemonEvent()
    data class TerminalExit(val sessionId: String, val exitCode: Int) : DaemonEvent()
    data class TerminalCreated(val sessionId: String, val shell: String) : DaemonEvent()
    
    // File events
    data class FileContent(val path: String, val content: String) : DaemonEvent()
    data class FileSaved(val path: String) : DaemonEvent()
    
    // Tunnel events
    data class TunnelCreated(val tunnelId: String, val url: String, val port: Int) : DaemonEvent()
    data class TunnelClosed(val tunnelId: String, val exitCode: Int? = null) : DaemonEvent()
    data class TunnelStatus(
        val tunnelId: String,
        val status: String,
        val url: String?,
        val port: Int,
        val service: String,
        val uptime: Int
    ) : DaemonEvent()
    data class TunnelDownloadProgress(
        val status: String,
        val downloaded: Long,
        val total: Long,
        val percent: Int
    ) : DaemonEvent()
    data class TunnelDownloadComplete(val path: String) : DaemonEvent()
    data class PortsDetected(val ports: List<com.artier.ide.data.model.DetectedPort>) : DaemonEvent()
    
    // Database events
    data class DbConnected(val connectionId: String, val connectionType: String) : DaemonEvent()
    data class DbDisconnected(val connectionId: String) : DaemonEvent()
    data class DbQueryResult(
        val connectionId: String,
        val rows: List<Map<String, Any?>>,
        val rowCount: Int,
        val fields: List<String>,
        val duration: Long,
        val error: String? = null
    ) : DaemonEvent()
    data class DbTables(val connectionId: String, val tables: List<String>) : DaemonEvent()
    data class DbSchema(
        val connectionId: String,
        val tableName: String,
        val columns: List<com.artier.ide.data.model.ColumnInfo>
    ) : DaemonEvent()
    data class DbChatSessionCreated(val sessionId: String) : DaemonEvent()
    data class DbChatSessions(val sessions: List<com.google.gson.JsonObject>) : DaemonEvent()
    data class DbChatMessageAdded(val messageId: String) : DaemonEvent()
    data class DbChatMessages(val sessionId: String, val messages: List<com.google.gson.JsonObject>) : DaemonEvent()
    
    // Skill events
    data class SkillListResult(val skills: List<com.artier.ide.data.model.SkillInfo>) : DaemonEvent()
    data class SkillDetailResult(
        val name: String,
        val content: String,
        val metadata: com.google.gson.JsonObject? = null
    ) : DaemonEvent()
    
    // Agent events
    data class AgentStatusUpdate(
        val sessionId: String,
        val status: String,
        val agentType: String? = null
    ) : DaemonEvent()
    data class AgentOutput(
        val sessionId: String,
        val content: String,
        val toolCalls: List<com.artier.ide.data.model.ToolCallInfo> = emptyList()
    ) : DaemonEvent()
    data class AgentError(val sessionId: String, val message: String) : DaemonEvent()
    
    // Router events
    data class RouterStatusEvent(val isRunning: Boolean, val port: Int) : DaemonEvent()
    data class RouterConfigEvent(val config: com.google.gson.JsonObject? = null) : DaemonEvent()
    
    // Error
    data class Error(val message: String) : DaemonEvent()
    object Unknown : DaemonEvent()
}
