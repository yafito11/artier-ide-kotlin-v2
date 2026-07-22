package com.artier.ide.data.remote

import android.util.Log
import com.artier.ide.data.model.*
import com.google.gson.Gson
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
 * High-level client for communicating with the Artier IDE daemon.
 * Provides typed methods for all daemon operations and manages connection state.
 */
@Singleton
class DaemonClient @Inject constructor(
    private val daemonApi: DaemonApi,
    private val webSocketClient: WebSocketClient
) {
    companion object {
        private const val TAG = "DaemonClient"
        private const val DEFAULT_DAEMON_URL = "ws://127.0.0.1:8080/ws"
        private const val CONNECTION_TIMEOUT = 10000L
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()
    
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private val _daemonInfo = MutableStateFlow<DaemonInfo?>(null)
    val daemonInfo: StateFlow<DaemonInfo?> = _daemonInfo.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    /**
     * Connect to the daemon
     */
    fun connect(url: String = DEFAULT_DAEMON_URL) {
        if (_connectionState.value == ConnectionState.Connected) {
            Log.w(TAG, "Already connected")
            return
        }
        
        _connectionState.value = ConnectionState.Connecting
        Log.i(TAG, "Connecting to daemon: $url")
        
        // Setup event listeners
        setupEventListeners()
        
        // Connect
        daemonApi.connect(url)
    }
    
    /**
     * Setup event listeners for daemon events
     */
    private fun setupEventListeners() {
        scope.launch {
            daemonApi.events.collect { event ->
                when (event) {
                    is DaemonEvent.Connected -> {
                        _connectionState.value = ConnectionState.Connected
                        Log.i(TAG, "Connected to daemon")
                    }
                    is DaemonEvent.Disconnected -> {
                        _connectionState.value = ConnectionState.Disconnected
                        _daemonInfo.value = null
                        Log.w(TAG, "Disconnected from daemon")
                    }
                    is DaemonEvent.DaemonReady -> {
                        _daemonInfo.value = DaemonInfo(
                            version = event.version,
                            features = event.features,
                            cloudflaredAvailable = event.cloudflaredAvailable
                        )
                        Log.i(TAG, "Daemon ready: v${event.version}")
                    }
                    is DaemonEvent.Error -> {
                        _error.value = event.message
                        Log.e(TAG, "Daemon error: ${event.message}")
                    }
                    else -> {
                        // Other events handled by specific methods
                    }
                }
            }
        }
    }
    
    /**
     * Disconnect from daemon
     */
    fun disconnect() {
        daemonApi.disconnect()
        _connectionState.value = ConnectionState.Disconnected
        _daemonInfo.value = null
    }
    
    /**
     * Wait for daemon to be ready
     */
    suspend fun waitForReady(timeout: Long = CONNECTION_TIMEOUT): Boolean {
        if (_daemonInfo.value != null) return true
        
        return try {
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < timeout) {
                if (_daemonInfo.value != null) return true
                kotlinx.coroutines.delay(100)
            }
            false
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check if connected and ready
     */
    fun isConnectedAndReady(): Boolean {
        return _connectionState.value == ConnectionState.Connected && _daemonInfo.value != null
    }
    
    // ==================== Terminal Commands ====================
    
    /**
     * Create a new terminal session
     */
    fun createTerminal(workingDirectory: String = "/") {
        daemonApi.createTerminalSession(workingDirectory)
    }
    
    /**
     * Send input to terminal
     */
    fun sendTerminalInput(sessionId: String, input: String) {
        daemonApi.sendTerminalInput(sessionId, input)
    }
    
    /**
     * Resize terminal
     */
    fun resizeTerminal(sessionId: String, cols: Int, rows: Int) {
        daemonApi.resizeTerminal(sessionId, cols, rows)
    }
    
    /**
     * Close terminal session
     */
    fun closeTerminal(sessionId: String) {
        daemonApi.closeTerminalSession(sessionId)
    }
    
    // ==================== File Commands ====================
    
    /**
     * Read file content
     */
    fun readFile(path: String) {
        daemonApi.readFile(path)
    }
    
    /**
     * Write file content
     */
    fun writeFile(path: String, content: String) {
        daemonApi.writeFile(path, content)
    }
    
    /**
     * List directory contents
     */
    fun listDirectory(path: String) {
        daemonApi.listDirectory(path)
    }
    
    /**
     * Create a new file
     */
    fun createFile(path: String) {
        daemonApi.createFile(path)
    }
    
    /**
     * Create a new directory
     */
    fun createDirectory(path: String) {
        daemonApi.createDirectory(path)
    }
    
    /**
     * Delete a file or directory
     */
    fun deleteFile(path: String) {
        daemonApi.deleteFile(path)
    }
    
    /**
     * Rename/move a file
     */
    fun renameFile(oldPath: String, newPath: String) {
        daemonApi.renameFile(oldPath, newPath)
    }
    
    // ==================== Database Commands ====================
    
    /**
     * Connect to a database
     */
    fun connectDatabase(
        type: DbType,
        host: String = "",
        port: Int = 0,
        database: String = "",
        username: String = "",
        password: String = ""
    ) {
        daemonApi.connectDatabase(type, host, port, database, username, password)
    }
    
    /**
     * Disconnect from database
     */
    fun disconnectDatabase(connectionId: String) {
        daemonApi.disconnectDatabase(connectionId)
    }
    
    /**
     * Execute SQL query
     */
    fun queryDatabase(connectionId: String, sql: String) {
        daemonApi.queryDatabase(connectionId, sql)
    }
    
    /**
     * Get database tables
     */
    fun getDatabaseTables(connectionId: String) {
        daemonApi.getDatabaseTables(connectionId)
    }
    
    /**
     * Get table schema
     */
    fun getTableSchema(connectionId: String, tableName: String) {
        daemonApi.getTableSchema(connectionId, tableName)
    }
    
    // ==================== Tunnel Commands ====================
    
    /**
     * Create a tunnel
     */
    fun createTunnel(port: Int, service: String = "http") {
        daemonApi.createTunnel(port, service)
    }
    
    /**
     * Close a tunnel
     */
    fun closeTunnel(tunnelId: String) {
        daemonApi.closeTunnel(tunnelId)
    }
    
    /**
     * Get tunnel status
     */
    fun getTunnelStatus(tunnelId: String) {
        daemonApi.getTunnelStatus(tunnelId)
    }
    
    /**
     * Detect available ports
     */
    fun detectPorts(service: String = "all") {
        daemonApi.detectPorts(service)
    }
    
    // ==================== Agent Commands ====================
    
    /**
     * Run an agent
     */
    fun runAgent(agent: String, prompt: String) {
        daemonApi.runAgent(agent, prompt)
    }
    
    /**
     * Get agent status
     */
    fun getAgentStatus(sessionId: String) {
        daemonApi.getAgentStatus(sessionId)
    }
    
    /**
     * Stop agent
     */
    fun stopAgent(sessionId: String) {
        daemonApi.stopAgent(sessionId)
    }
    
    /**
     * Send input to agent
     */
    fun sendAgentInput(sessionId: String, input: String) {
        daemonApi.sendAgentInput(sessionId, input)
    }
    
    // ==================== Skill Commands ====================
    
    /**
     * Get list of skills
     */
    fun getSkills() {
        daemonApi.getSkills()
    }
    
    /**
     * Get skill details
     */
    fun getSkillDetail(name: String) {
        daemonApi.getSkillDetail(name)
    }
    
    /**
     * Install a skill
     */
    fun installSkill(path: String) {
        daemonApi.installSkill(path)
    }
    
    /**
     * Uninstall a skill
     */
    fun uninstallSkill(name: String) {
        daemonApi.uninstallSkill(name)
    }
    
    /**
     * Scan for skills
     */
    fun scanSkills() {
        daemonApi.scanSkills()
    }
    
    /**
     * Enable/disable a skill
     */
    fun setSkillEnabled(name: String, enabled: Boolean) {
        daemonApi.setSkillEnabled(name, enabled)
    }
    
    // ==================== Router Commands ====================
    
    /**
     * Get router status
     */
    fun getRouterStatus() {
        daemonApi.getRouterStatus()
    }
    
    /**
     * Get router config
     */
    fun getRouterConfig() {
        daemonApi.getRouterConfig()
    }
    
    /**
     * Update router config
     */
    fun updateRouterConfig(config: JsonObject) {
        daemonApi.updateRouterConfig(config)
    }
    
    // ==================== Helper Methods ====================
    
    /**
     * Clear error state
     */
    fun clearError() {
        _error.value = null
    }
}

/**
 * Connection state
 */
sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

/**
 * Daemon information
 */
data class DaemonInfo(
    val version: String,
    val features: List<String>,
    val cloudflaredAvailable: Boolean
)
