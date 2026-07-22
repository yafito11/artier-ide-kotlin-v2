package com.artier.ide.proot

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * Manages the Artier IDE daemon server lifecycle.
 * Handles starting, stopping, and monitoring the Node.js daemon.
 */
class DaemonManager(
    private val context: Context,
    private val prootManager: ProotManager
) {
    
    companion object {
        private const val TAG = "DaemonManager"
        private const val DAEMON_DIR = "opt/artier/daemon"
        private const val DEFAULT_PORT = 8080
        private const val HEALTH_CHECK_INTERVAL = 5000L
        private const val MAX_RESTART_ATTEMPTS = 3
        private const val RESTART_DELAY = 2000L
    }
    
    private val _state = MutableStateFlow<DaemonState>(DaemonState.Stopped)
    val state: StateFlow<DaemonState> = _state.asStateFlow()
    
    private val _logs = MutableStateFlow<List<DaemonLog>>(emptyList())
    val logs: StateFlow<List<DaemonLog>> = _logs.asStateFlow()
    
    private val _port = MutableStateFlow(DEFAULT_PORT)
    val port: StateFlow<Int> = _port.asStateFlow()
    
    private var daemonProcess: Process? = null
    private var healthCheckThread: Thread? = null
    private var restartCount = 0
    private var isManualStop = false
    
    val daemonPath: String
        get() = File(prootManager.rootfsPath, DAEMON_DIR).absolutePath
    
    val serverJsPath: String
        get() = File(daemonPath, "server.js").absolutePath
    
    val isRunning: Boolean
        get() = _state.value is DaemonState.Running
    
    /**
     * Initialize daemon environment
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            addLog("Initializing daemon environment...")
            
            // Create daemon directory structure
            createDaemonDirectories()
            
            // Copy daemon files if needed
            copyDaemonFiles()
            
            // Install dependencies if needed
            installDependencies()
            
            addLog("Daemon environment initialized")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize daemon", e)
            addLog("Error initializing daemon: ${e.message}")
            false
        }
    }
    
    /**
     * Create daemon directory structure
     */
    private fun createDaemonDirectories() {
        val dirs = listOf(
            DAEMON_DIR,
            "$DAEMON_DIR/src",
            "$DAEMON_DIR/src/database",
            "$DAEMON_DIR/src/lsp",
            "$DAEMON_DIR/src/pkg",
            "$DAEMON_DIR/src/pty",
            "$DAEMON_DIR/src/skills",
            "$DAEMON_DIR/src/sse",
            "$DAEMON_DIR/src/tunnel",
            "$DAEMON_DIR/router",
            "$DAEMON_DIR/node_modules"
        )
        
        dirs.forEach { dir ->
            val file = File(prootManager.rootfsPath, dir)
            if (!file.exists()) {
                file.mkdirs()
            }
        }
    }
    
    /**
     * Copy daemon files from assets
     */
    private fun copyDaemonFiles() {
        addLog("Copying daemon files...")
        
        // Check if already copied
        val serverJs = File(serverJsPath)
        if (serverJs.exists() && serverJs.length() > 100) {
            addLog("Daemon files already copied")
            return
        }
        
        try {
            // Copy from assets
            copyAssetDirectory("rootfs/opt/artier/daemon", File(daemonPath))
            addLog("Daemon files copied from assets")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to copy from assets, creating minimal daemon", e)
            createMinimalDaemon()
        }
    }
    
    /**
     * Copy asset directory
     */
    private fun copyAssetDirectory(assetPath: String, targetDir: File) {
        targetDir.mkdirs()
        
        try {
            val files = context.assets.list(assetPath) ?: emptyArray()
            
            for (file in files) {
                val assetFilePath = if (assetPath.isEmpty()) file else "$assetPath/$file"
                val targetFile = File(targetDir, file)
                
                val subFiles = context.assets.list(assetFilePath)
                if (subFiles != null && subFiles.isNotEmpty()) {
                    // Directory
                    targetFile.mkdirs()
                    copyAssetDirectory(assetFilePath, targetFile)
                } else {
                    // File
                    context.assets.open(assetFilePath).use { input ->
                        java.io.FileOutputStream(targetFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy asset directory: $assetPath", e)
        }
    }
    
    /**
     * Create minimal daemon server
     */
    private fun createMinimalDaemon() {
        addLog("Creating minimal daemon server...")
        
        // Create package.json
        File(daemonPath, "package.json").writeText("""
            {
                "name": "artier-ide-daemon",
                "version": "1.0.0",
                "description": "Artier IDE Daemon Server",
                "main": "server.js",
                "scripts": {
                    "start": "node server.js"
                },
                "dependencies": {
                    "express": "^4.18.2",
                    "ws": "^8.14.2"
                }
            }
        """.trimIndent())
        
        // Create minimal server.js
        File(serverJsPath).writeText("""
            const http = require('http');
            const { WebSocketServer } = require('ws');
            
            const PORT = process.env.PORT || ${DEFAULT_PORT};
            
            const server = http.createServer((req, res) => {
                res.writeHead(200, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ status: 'ok', daemon: 'artier-ide', version: '1.0.0' }));
            });
            
            const wss = new WebSocketServer({ server });
            
            wss.on('connection', (ws) => {
                console.log('[Daemon] Client connected');
                
                ws.on('message', (data) => {
                    try {
                        const msg = JSON.parse(data.toString());
                        console.log('[Daemon] Received:', msg.type);
                        
                        // Echo back for now
                        ws.send(JSON.stringify({ type: 'ack', id: msg.id }));
                    } catch (e) {
                        console.error('[Daemon] Parse error:', e.message);
                    }
                });
                
                ws.on('close', () => {
                    console.log('[Daemon] Client disconnected');
                });
                
                // Send welcome message
                ws.send(JSON.stringify({
                    type: 'welcome',
                    version: '1.0.0',
                    capabilities: ['terminal', 'files', 'database', 'skills']
                }));
            });
            
            server.listen(PORT, '127.0.0.1', () => {
                console.log(`[Daemon] Server running on http://127.0.0.1:${'$'}{PORT}`);
                console.log(`[Daemon] WebSocket available on ws://127.0.0.1:${'$'}{PORT}`);
            });
            
            process.on('SIGTERM', () => {
                console.log('[Daemon] Shutting down...');
                wss.close();
                server.close();
                process.exit(0);
            });
        """.trimIndent())
        
        addLog("Minimal daemon created")
    }
    
    /**
     * Install Node.js dependencies
     */
    private suspend fun installDependencies() = withContext(Dispatchers.IO) {
        addLog("Installing dependencies...")
        
        val nodeModules = File(daemonPath, "node_modules")
        if (nodeModules.exists() && nodeModules.listFiles()?.isNotEmpty() == true) {
            addLog("Dependencies already installed")
            return@withContext
        }
        
        try {
            val result = prootManager.executeCommand(
                "cd $DAEMON_DIR && npm install --production 2>&1",
                daemonPath,
                timeout = 120
            )
            
            when (result) {
                is ProotResult.Success -> {
                    addLog("Dependencies installed successfully")
                }
                is ProotResult.Error -> {
                    addLog("npm install failed: ${result.message}")
                    // Continue anyway - minimal server doesn't need dependencies
                }
            }
        } catch (e: Exception) {
            addLog("Failed to install dependencies: ${e.message}")
            // Continue anyway
        }
    }
    
    /**
     * Start daemon server
     */
    suspend fun start(): Boolean = withContext(Dispatchers.IO) {
        if (isRunning) {
            addLog("Daemon already running")
            return@withContext true
        }
        
        try {
            _state.value = DaemonState.Starting
            isManualStop = false
            
            addLog("Starting daemon server...")
            
            // Check Node.js availability
            if (!prootManager.isNodeAvailable()) {
                addLog("Error: Node.js not available")
                _state.value = DaemonState.Error("Node.js not available")
                return@withContext false
            }
            
            // Check server.js exists
            if (!File(serverJsPath).exists()) {
                addLog("Error: server.js not found")
                _state.value = DaemonState.Error("server.js not found")
                return@withContext false
            }
            
            // Start daemon process
            daemonProcess = prootManager.startProcess(
                command = "cd $DAEMON_DIR && node server.js",
                workingDirectory = daemonPath,
                outputCallback = { line ->
                    addLog(line)
                    
                    // Check for ready signal
                    if (line.contains("Server running")) {
                        _state.value = DaemonState.Running(port = DEFAULT_PORT)
                        restartCount = 0
                    }
                }
            )
            
            if (daemonProcess == null) {
                addLog("Failed to start daemon process")
                _state.value = DaemonState.Error("Failed to start process")
                return@withContext false
            }
            
            // Start health check
            startHealthCheck()
            
            // Wait for daemon to start
            var waitTime = 0L
            while (waitTime < 10000 && _state.value !is DaemonState.Running) {
                Thread.sleep(100)
                waitTime += 100
            }
            
            if (_state.value !is DaemonState.Running) {
                addLog("Daemon startup timeout")
                _state.value = DaemonState.Error("Startup timeout")
                return@withContext false
            }
            
            addLog("Daemon started on port $DEFAULT_PORT")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start daemon", e)
            addLog("Error starting daemon: ${e.message}")
            _state.value = DaemonState.Error("Start failed: ${e.message}")
            false
        }
    }
    
    /**
     * Start health check thread
     */
    private fun startHealthCheck() {
        healthCheckThread?.interrupt()
        
        healthCheckThread = Thread {
            while (!Thread.currentThread().isInterrupted && isRunning) {
                try {
                    Thread.sleep(HEALTH_CHECK_INTERVAL)
                    
                    // Check if process is still alive
                    val process = daemonProcess
                    if (process != null && !process.isAlive) {
                        Log.w(TAG, "Daemon process died")
                        addLog("Daemon process died unexpectedly")
                        
                        if (!isManualStop && restartCount < MAX_RESTART_ATTEMPTS) {
                            addLog("Attempting restart...")
                            restartCount++
                            restartAsync()
                        } else {
                            _state.value = DaemonState.Error("Process died")
                        }
                    }
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "Health check error", e)
                }
            }
        }.apply {
            isDaemon = true
            start()
        }
    }
    
    /**
     * Stop daemon server
     */
    fun stop() {
        isManualStop = true
        addLog("Stopping daemon...")
        
        healthCheckThread?.interrupt()
        healthCheckThread = null
        
        prootManager.stopDaemon()
        daemonProcess = null
        
        _state.value = DaemonState.Stopped
        addLog("Daemon stopped")
    }
    
    /**
     * Restart daemon
     */
    suspend fun restart(): Boolean {
        addLog("Restarting daemon...")
        stop()
        Thread.sleep(RESTART_DELAY)
        return start()
    }
    
    /**
     * Restart async (fire and forget)
     */
    private fun restartAsync() {
        Thread {
            try {
                Thread.sleep(RESTART_DELAY)
                prootManager.stopDaemon()
                daemonProcess = null
                startDaemonProcess()
            } catch (e: Exception) {
                Log.e(TAG, "Restart failed", e)
            }
        }.start()
    }
    
    /**
     * Start daemon process (internal)
     */
    private fun startDaemonProcess() {
        daemonProcess = prootManager.startProcess(
            command = "cd $DAEMON_DIR && node server.js",
            workingDirectory = daemonPath,
            outputCallback = { line ->
                addLog(line)
                if (line.contains("Server running")) {
                    _state.value = DaemonState.Running(port = DEFAULT_PORT)
                }
            }
        )
    }
    
    /**
     * Get daemon status
     */
    fun getStatus(): DaemonStatusInfo {
        val process = daemonProcess
        return DaemonStatusInfo(
            state = _state.value,
            pid = process?.let { getProcessId(it) },
            port = DEFAULT_PORT,
            uptime = getUptime(),
            memoryUsage = getMemoryUsage(),
            restartCount = restartCount
        )
    }
    
    /**
     * Get process ID
     */
    private fun getProcessId(process: Process): Long? {
        return try {
            if (process.javaClass.name.contains("UnixProcess")) {
                val pidField = process.javaClass.getDeclaredField("pid")
                pidField.isAccessible = true
                pidField.getLong(process)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get uptime in seconds
     */
    private fun getUptime(): Long {
        val state = _state.value
        return if (state is DaemonState.Running) {
            // TODO: Track actual start time
            0L
        } else {
            0L
        }
    }
    
    /**
     * Get memory usage in bytes
     */
    private fun getMemoryUsage(): Long {
        val process = daemonProcess ?: return 0L
        return try {
            val pid = getProcessId(process) ?: return 0L
            // Read from /proc/pid/status
            val statusFile = File("/proc/$pid/status")
            if (statusFile.exists()) {
                val content = statusFile.readText()
                val vmRSSMatch = Regex("VmRSS:\\s+(\\d+)\\s+kB").find(content)
                vmRSSMatch?.groupValues?.get(1)?.toLongOrNull()?.times(1024) ?: 0L
            } else {
                0L
            }
        } catch (e: Exception) {
            0L
        }
    }
    
    /**
     * Add log entry
     */
    private fun addLog(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault())
            .format(java.util.Date())
        val entry = DaemonLog(timestamp = timestamp, message = message)
        _logs.value = (_logs.value + entry).takeLast(1000) // Keep last 1000 entries
        Log.d(TAG, "[$timestamp] $message")
    }
    
    /**
     * Clear logs
     */
    fun clearLogs() {
        _logs.value = emptyList()
    }
    
    /**
     * Cleanup daemon resources
     */
    fun cleanup() {
        stop()
        Log.i(TAG, "Daemon cleanup complete")
    }
}

/**
 * Daemon state
 */
sealed class DaemonState {
    object Stopped : DaemonState()
    object Starting : DaemonState()
    data class Running(val port: Int) : DaemonState()
    data class Error(val message: String) : DaemonState()
}

/**
 * Daemon log entry
 */
data class DaemonLog(
    val timestamp: String,
    val message: String
)

/**
 * Daemon status info
 */
data class DaemonStatusInfo(
    val state: DaemonState,
    val pid: Long?,
    val port: Int,
    val uptime: Long,
    val memoryUsage: Long,
    val restartCount: Int
)
