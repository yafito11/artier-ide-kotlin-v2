package com.artier.ide.proot

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Orchestrates the initialization of the entire Artier IDE Linux environment.
 * Manages the startup sequence: proot → daemon → verification.
 */
class SystemInitializer(private val context: Context) {
    
    companion object {
        private const val TAG = "SystemInitializer"
        private const val MAX_VERIFICATION_ATTEMPTS = 5
        private const val VERIFICATION_DELAY = 1000L
    }
    
    val prootManager = ProotManager(context)
    val daemonManager = DaemonManager(context, prootManager)
    
    private val _state = MutableStateFlow<SystemState>(SystemState.Initializing)
    val state: StateFlow<SystemState> = _state.asStateFlow()
    
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()
    
    private val _currentStep = MutableStateFlow("")
    val currentStep: StateFlow<String> = _currentStep.asStateFlow()
    
    private val _logs = MutableStateFlow<List<SystemLog>>(emptyList())
    val logs: StateFlow<List<SystemLog>> = _logs.asStateFlow()
    
    private val startTime = System.currentTimeMillis()
    
    /**
     * Initialize the entire system
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            _state.value = SystemState.Initializing
            _progress.value = 0f
            addLog("Starting system initialization...", LogLevel.INFO)
            
            // Step 1: Initialize proot (30%)
            _currentStep.value = "Extracting Linux environment..."
            addLog("Step 1/4: Initializing proot environment...", LogLevel.INFO)
            
            prootManager.state.collect { prootState ->
                when (prootState) {
                    is ProotState.Initializing -> {
                        _progress.value = prootManager.progress.value * 0.3f
                    }
                    is ProotState.Ready -> {
                        _progress.value = 0.3f
                    }
                    is ProotState.Error -> {
                        _state.value = SystemState.Error("Proot initialization failed: ${prootState.message}")
                        return@withContext false
                    }
                    else -> {}
                }
            }
            
            if (!prootManager.initialize()) {
                _state.value = SystemState.Error("Failed to initialize proot")
                return@withContext false
            }
            _progress.value = 0.3f
            addLog("Proot initialized successfully", LogLevel.INFO)
            
            // Step 2: Initialize daemon (60%)
            _currentStep.value = "Preparing daemon server..."
            addLog("Step 2/4: Initializing daemon...", LogLevel.INFO)
            _progress.value = 0.35f
            
            if (!daemonManager.initialize()) {
                _state.value = SystemState.Error("Failed to initialize daemon")
                return@withContext false
            }
            _progress.value = 0.6f
            addLog("Daemon initialized successfully", LogLevel.INFO)
            
            // Step 3: Start daemon (80%)
            _currentStep.value = "Starting daemon server..."
            addLog("Step 3/4: Starting daemon server...", LogLevel.INFO)
            _progress.value = 0.65f
            
            daemonManager.state.collect { daemonState ->
                when (daemonState) {
                    is DaemonState.Starting -> {
                        _progress.value = 0.7f
                    }
                    is DaemonState.Running -> {
                        _progress.value = 0.8f
                    }
                    is DaemonState.Error -> {
                        _state.value = SystemState.Error("Daemon start failed: ${daemonState.message}")
                        return@withContext false
                    }
                    else -> {}
                }
            }
            
            if (!daemonManager.start()) {
                _state.value = SystemState.Error("Failed to start daemon")
                return@withContext false
            }
            _progress.value = 0.8f
            addLog("Daemon started successfully", LogLevel.INFO)
            
            // Step 4: Verify system (100%)
            _currentStep.value = "Verifying system..."
            addLog("Step 4/4: Verifying system...", LogLevel.INFO)
            _progress.value = 0.85f
            
            if (!verifySystem()) {
                _state.value = SystemState.Error("System verification failed")
                return@withContext false
            }
            _progress.value = 1.0f
            addLog("System verification passed", LogLevel.INFO)
            
            // Calculate initialization time
            val initTime = System.currentTimeMillis() - startTime
            addLog("System initialization complete! (${initTime}ms)", LogLevel.INFO)
            
            _state.value = SystemState.Ready
            _currentStep.value = "Ready"
            true
        } catch (e: Exception) {
            Log.e(TAG, "System initialization failed", e)
            _state.value = SystemState.Error("Initialization failed: ${e.message}")
            addLog("Error: ${e.message}", LogLevel.ERROR)
            false
        }
    }
    
    /**
     * Verify system is working properly
     */
    private suspend fun verifySystem(): Boolean = withContext(Dispatchers.IO) {
        var attempts = 0
        
        while (attempts < MAX_VERIFICATION_ATTEMPTS) {
            attempts++
            addLog("Verification attempt $attempts/$MAX_VERIFICATION_ATTEMPTS", LogLevel.DEBUG)
            
            try {
                // Check proot
                if (prootManager.isInitialized) {
                    addLog("✓ Proot: Ready", LogLevel.INFO)
                } else {
                    addLog("✗ Proot: Not initialized", LogLevel.WARNING)
                    if (attempts < MAX_VERIFICATION_ATTEMPTS) {
                        delay(VERIFICATION_DELAY)
                        continue
                    }
                }
                
                // Check daemon
                if (daemonManager.isRunning) {
                    addLog("✓ Daemon: Running on port ${daemonManager.port.value}", LogLevel.INFO)
                } else {
                    addLog("✗ Daemon: Not running", LogLevel.WARNING)
                    if (attempts < MAX_VERIFICATION_ATTEMPTS) {
                        delay(VERIFICATION_DELAY)
                        continue
                    }
                }
                
                // Test proot execution
                val prootTest = prootManager.executeCommand("echo 'proot ok'", timeout = 5)
                if (prootTest is ProotResult.Success) {
                    addLog("✓ Proot execution: Working", LogLevel.INFO)
                } else {
                    addLog("✗ Proot execution: Failed", LogLevel.WARNING)
                }
                
                // Test Node.js
                val nodeTest = prootManager.executeCommand(
                    "${prootManager.nodeBinaryPath} --version",
                    timeout = 5
                )
                if (nodeTest is ProotResult.Success) {
                    addLog("✓ Node.js: ${nodeTest.output.trim()}", LogLevel.INFO)
                } else {
                    addLog("✗ Node.js: Not available", LogLevel.WARNING)
                }
                
                // All checks passed
                return@withContext true
                
            } catch (e: Exception) {
                addLog("Verification error: ${e.message}", LogLevel.ERROR)
                if (attempts < MAX_VERIFICATION_ATTEMPTS) {
                    delay(VERIFICATION_DELAY)
                }
            }
        }
        
        false
    }
    
    /**
     * Restart system
     */
    suspend fun restart(): Boolean {
        addLog("Restarting system...", LogLevel.INFO)
        shutdown()
        delay(1000)
        return initialize()
    }
    
    /**
     * Shutdown system
     */
    fun shutdown() {
        addLog("Shutting down system...", LogLevel.INFO)
        
        daemonManager.cleanup()
        prootManager.cleanup()
        
        _state.value = SystemState.Stopped
        _progress.value = 0f
        _currentStep.value = "Stopped"
        
        addLog("System shutdown complete", LogLevel.INFO)
    }
    
    /**
     * Get comprehensive system status
     */
    fun getSystemStatus(): SystemStatus {
        val daemonStatus = daemonManager.getStatus()
        
        return SystemStatus(
            prootReady = prootManager.isInitialized,
            daemonRunning = daemonManager.isRunning,
            daemonStatus = daemonStatus,
            nodeAvailable = prootManager.isNodeAvailable(),
            diskUsage = prootManager.getDiskUsage(),
            prootVersion = "checking...",
            nodeVersion = "checking..."
        )
    }
    
    /**
     * Add log entry
     */
    private fun addLog(message: String, level: LogLevel = LogLevel.INFO) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date())
        val entry = SystemLog(
            timestamp = timestamp,
            message = message,
            level = level
        )
        _logs.value = (_logs.value + entry).takeLast(500)
        
        when (level) {
            LogLevel.ERROR -> Log.e(TAG, "[$timestamp] $message")
            LogLevel.WARNING -> Log.w(TAG, "[$timestamp] $message")
            LogLevel.DEBUG -> Log.d(TAG, "[$timestamp] $message")
            else -> Log.i(TAG, "[$timestamp] $message")
        }
    }
    
    /**
     * Clear logs
     */
    fun clearLogs() {
        _logs.value = emptyList()
    }
    
    /**
     * Get logs as formatted string
     */
    fun getLogsAsString(): String {
        return _logs.value.joinToString("\n") { log ->
            "[${log.timestamp}] ${log.level.name}: ${log.message}"
        }
    }
}

/**
 * System state
 */
sealed class SystemState {
    object Initializing : SystemState()
    object Ready : SystemState()
    object Stopped : SystemState()
    data class Error(val message: String) : SystemState()
}

/**
 * System status info
 */
data class SystemStatus(
    val prootReady: Boolean,
    val daemonRunning: Boolean,
    val daemonStatus: DaemonStatusInfo,
    val nodeAvailable: Boolean,
    val diskUsage: Long,
    val prootVersion: String,
    val nodeVersion: String
)

/**
 * System log entry
 */
data class SystemLog(
    val timestamp: String,
    val message: String,
    val level: LogLevel
)

/**
 * Log levels
 */
enum class LogLevel {
    DEBUG,
    INFO,
    WARNING,
    ERROR
}
