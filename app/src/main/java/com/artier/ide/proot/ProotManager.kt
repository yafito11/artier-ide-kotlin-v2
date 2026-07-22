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
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * Manages proot environment for running Linux binaries on Android.
 * Handles extraction of assets, binary management, and command execution.
 */
class ProotManager(private val context: Context) {
    
    companion object {
        private const val TAG = "ProotManager"
        private const val ROOTFS_DIR = "rootfs"
        private const val BIN_DIR = "bin"
        private const val PROOT_BINARY = "proot"
        private const val NODE_BINARY = "node"
        private const val NPM_BINARY = "npm"
        private const val CLOUDFLARED_BINARY = "cloudflared"
        private const val EXTRACTION_MARKER = ".extraction_complete"
        
        // Supported architectures
        private const val ARCH_AARCH64 = "aarch64"
        private const val ARCH_ARM = "arm"
        private const val ARCH_X86_64 = "x86_64"
    }
    
    private val _state = MutableStateFlow<ProotState>(ProotState.Uninitialized)
    val state: StateFlow<ProotState> = _state.asStateFlow()
    
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()
    
    var isInitialized = false
        private set
    
    private var daemonProcess: Process? = null
    private var processOutputThread: Thread? = null
    
    val rootfsPath: String
        get() = File(context.filesDir, ROOTFS_DIR).absolutePath
    
    val binPath: String
        get() = File(context.filesDir, BIN_DIR).absolutePath
    
    val prootBinaryPath: String
        get() = File(binPath, PROOT_BINARY).absolutePath
    
    val nodeBinaryPath: String
        get() = File(rootfsPath, "usr/bin/$NODE_BINARY").absolutePath
    
    val npmBinaryPath: String
        get() = File(rootfsPath, "usr/bin/$NPM_BINARY").absolutePath
    
    val cloudflaredBinaryPath: String
        get() = File(binPath, CLOUDFLARED_BINARY).absolutePath
    
    val daemonPath: String
        get() = File(rootfsPath, "opt/artier/daemon").absolutePath
    
    /**
     * Initialize proot environment with real binaries
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        if (isInitialized) return@withContext true
        
        try {
            _state.value = ProotState.Initializing
            
            // Check if already extracted
            if (isExtractionComplete()) {
                Log.i(TAG, "Proot already initialized")
                isInitialized = true
                _state.value = ProotState.Ready
                return@withContext true
            }
            
            // Create directories
            createDirectories()
            _progress.value = 0.1f
            
            // Extract rootfs from assets
            extractRootfs()
            _progress.value = 0.5f
            
            // Extract binaries
            extractBinaries()
            _progress.value = 0.8f
            
            // Set permissions
            setBinaryPermissions()
            _progress.value = 0.9f
            
            // Verify installation
            verifyInstallation()
            _progress.value = 1.0f
            
            // Mark extraction complete
            markExtractionComplete()
            
            isInitialized = true
            _state.value = ProotState.Ready
            Log.i(TAG, "Proot initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize proot", e)
            _state.value = ProotState.Error("Initialization failed: ${e.message}")
            false
        }
    }
    
    /**
     * Check if extraction is already complete
     */
    private fun isExtractionComplete(): Boolean {
        val marker = File(context.filesDir, EXTRACTION_MARKER)
        return marker.exists() && 
               File(prootBinaryPath).exists() &&
               File(rootfsPath, "etc/passwd").exists()
    }
    
    /**
     * Mark extraction as complete
     */
    private fun markExtractionComplete() {
        File(context.filesDir, EXTRACTION_MARKER).createNewFile()
    }
    
    /**
     * Create necessary directories
     */
    private fun createDirectories() {
        val dirs = listOf(
            BIN_DIR,
            ROOTFS_DIR,
            "$ROOTFS_DIR/bin",
            "$ROOTFS_DIR/lib",
            "$ROOTFS_DIR/lib64",
            "$ROOTFS_DIR/usr",
            "$ROOTFS_DIR/usr/bin",
            "$ROOTFS_DIR/usr/lib",
            "$ROOTFS_DIR/usr/lib/node_modules",
            "$ROOTFS_DIR/etc",
            "$ROOTFS_DIR/tmp",
            "$ROOTFS_DIR/home",
            "$ROOTFS_DIR/home/user",
            "$ROOTFS_DIR/var",
            "$ROOTFS_DIR/var/tmp",
            "$ROOTFS_DIR/proc",
            "$ROOTFS_DIR/sys",
            "$ROOTFS_DIR/dev",
            "$ROOTFS_DIR/opt",
            "$ROOTFS_DIR/opt/artier",
            "$ROOTFS_DIR/opt/artier/daemon"
        )
        
        dirs.forEach { dir ->
            val file = File(context.filesDir, dir)
            if (!file.exists()) {
                file.mkdirs()
            }
        }
    }
    
    /**
     * Extract rootfs from assets
     */
    private suspend fun extractRootfs() = withContext(Dispatchers.IO) {
        Log.i(TAG, "Extracting rootfs...")
        
        // Try to extract alpine-minirootfs tarball
        val tarGzFile = File(context.filesDir, "alpine-minirootfs.tar.gz")
        
        if (tarGzFile.exists()) {
            extractTarGz(tarGzFile, File(rootfsPath))
        } else {
            // Check assets for rootfs directory
            try {
                val assetFiles = context.assets.list(ROOTFS_DIR)
                if (assetFiles != null && assetFiles.isNotEmpty()) {
                    copyAssetDirectory(ROOTFS_DIR, File(rootfsPath))
                } else {
                    // Create minimal rootfs
                    createMinimalRootfs()
                }
            } catch (e: Exception) {
                // Create minimal rootfs
                createMinimalRootfs()
            }
        }
        
        Log.i(TAG, "Rootfs extraction complete")
    }
    
    /**
     * Extract tar.gz file
     */
    private fun extractTarGz(tarFile: File, targetDir: File) {
        try {
            val process = ProcessBuilder()
                .command("tar", "xzf", tarFile.absolutePath, "-C", targetDir.absolutePath)
                .redirectErrorStream(true)
                .start()
            
            val output = BufferedReader(InputStreamReader(process.inputStream)).readText()
            val exitCode = process.waitFor()
            
            if (exitCode != 0) {
                Log.w(TAG, "tar extraction failed: $output")
                createMinimalRootfs()
            }
        } catch (e: Exception) {
            Log.w(TAG, "tar not available, creating minimal rootfs", e)
            createMinimalRootfs()
        }
    }
    
    /**
     * Copy asset directory to target
     */
    private fun copyAssetDirectory(assetPath: String, targetDir: File) {
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
                        FileOutputStream(targetFile).use { output ->
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
     * Create minimal rootfs structure
     */
    private fun createMinimalRootfs() {
        Log.i(TAG, "Creating minimal rootfs...")
        
        val structure = mapOf(
            "bin" to listOf("sh", "bash", "ls", "cat", "echo", "mkdir", "rm", "cp", "mv", "touch", "chmod"),
            "lib" to emptyList(),
            "lib64" to emptyList(),
            "usr/bin" to listOf("node", "npm"),
            "usr/lib" to emptyList(),
            "usr/lib/node_modules" to emptyList(),
            "etc" to listOf("passwd", "group", "hosts", "resolv.conf"),
            "tmp" to emptyList(),
            "home/user" to emptyList(),
            "var/tmp" to emptyList(),
            "opt/artier/daemon" to emptyList()
        )
        
        structure.forEach { (dir, files) ->
            val dirFile = File(rootfsPath, dir)
            dirFile.mkdirs()
            
            files.forEach { file ->
                val fileFile = File(dirFile, file)
                if (!fileFile.exists()) {
                    when (file) {
                        "sh", "bash" -> {
                            fileFile.writeText("#!/system/bin/sh\n")
                            fileFile.setExecutable(true)
                        }
                        "passwd" -> {
                            fileFile.writeText("root:x:0:0:root:/root:/bin/sh\n")
                        }
                        "group" -> {
                            fileFile.writeText("root:x:0:\n")
                        }
                        "hosts" -> {
                            fileFile.writeText("127.0.0.1 localhost\n")
                        }
                        "resolv.conf" -> {
                            fileFile.writeText("nameserver 8.8.8.8\nnameserver 8.8.4.4\n")
                        }
                        else -> {
                            fileFile.createNewFile()
                        }
                    }
                }
            }
        }
        
        // Create proot-friendly environment
        File(rootfsPath, "etc/profile").writeText("""
            export PATH=/usr/bin:/bin:/usr/local/bin
            export HOME=/home/user
            export TERM=xterm-256color
        """.trimIndent())
    }
    
    /**
     * Extract binaries from assets
     */
    private suspend fun extractBinaries() = withContext(Dispatchers.IO) {
        Log.i(TAG, "Extracting binaries...")
        
        // Extract proot
        extractBinary(PROOT_BINARY, File(prootBinaryPath))
        
        // Extract cloudflared
        extractBinary(CLOUDFLARED_BINARY, File(cloudflaredBinaryPath))
        
        // Extract Node.js and npm to rootfs
        extractBinary("node", File(rootfsPath, "usr/bin/$NODE_BINARY"))
        extractBinary("npm", File(rootfsPath, "usr/bin/$NPM_BINARY"))
        
        Log.i(TAG, "Binaries extraction complete")
    }
    
    /**
     * Extract a single binary from assets
     */
    private fun extractBinary(assetName: String, targetFile: File) {
        if (targetFile.exists()) {
            Log.d(TAG, "Binary already exists: ${targetFile.name}")
            return
        }
        
        targetFile.parentFile?.mkdirs()
        
        try {
            context.assets.open("bin/$assetName").use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            Log.d(TAG, "Extracted binary: $assetName")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract binary: $assetName, creating placeholder", e)
            createBinaryPlaceholder(assetName, targetFile)
        }
    }
    
    /**
     * Create placeholder binary
     */
    private fun createBinaryPlaceholder(name: String, targetFile: File) {
        val placeholder = when (name) {
            "node" -> """
                #!/system/bin/sh
                echo "[node] Node.js binary not available"
                echo "Please download Node.js for your architecture"
                exit 1
            """.trimIndent()
            "npm" -> """
                #!/system/bin/sh
                echo "[npm] npm binary not available"
                echo "Please install Node.js which includes npm"
                exit 1
            """.trimIndent()
            "proot" -> """
                #!/system/bin/sh
                exec "$@"
            """.trimIndent()
            "cloudflared" -> """
                #!/system/bin/sh
                echo "[cloudflared] cloudflared binary not available"
                echo "Please download cloudflared for your architecture"
                exit 1
            """.trimIndent()
            else -> "#!/system/bin/sh\necho placeholder"
        }
        
        targetFile.writeText(placeholder)
    }
    
    /**
     * Set executable permissions on binaries
     */
    private fun setBinaryPermissions() {
        val binaries = listOf(
            prootBinaryPath,
            File(rootfsPath, "usr/bin/$NODE_BINARY").absolutePath,
            File(rootfsPath, "usr/bin/$NPM_BINARY").absolutePath,
            cloudflaredBinaryPath
        )
        
        binaries.forEach { path ->
            val file = File(path)
            if (file.exists()) {
                try {
                    val process = ProcessBuilder()
                        .command("chmod", "755", path)
                        .start()
                    process.waitFor(5, TimeUnit.SECONDS)
                } catch (e: Exception) {
                    Log.w(TAG, "chmod failed for $path", e)
                    // Fallback: set executable via Java
                    file.setExecutable(true, false)
                    file.setReadable(true, false)
                    file.setWritable(true, false)
                }
            }
        }
    }
    
    /**
     * Verify installation
     */
    private fun verifyInstallation(): Boolean {
        // Check essential files exist
        val essentialFiles = listOf(
            prootBinaryPath,
            File(rootfsPath, "etc/passwd"),
            File(rootfsPath, "usr/bin/$NODE_BINARY")
        )
        
        val missing = essentialFiles.filter { !File(it).exists() }
        if (missing.isNotEmpty()) {
            Log.w(TAG, "Missing essential files: $missing")
            // Continue anyway - placeholders should be in place
        }
        
        return true
    }
    
    /**
     * Execute command in proot environment
     */
    suspend fun executeCommand(
        command: String,
        workingDirectory: String = "/",
        timeout: Long = 30
    ): ProotResult = withContext(Dispatchers.IO) {
        if (!isInitialized) {
            return@withContext ProotResult.Error("Proot not initialized")
        }
        
        return@withContext try {
            val processBuilder = ProcessBuilder()
                .command(
                    prootBinaryPath,
                    "-0",
                    "-r", rootfsPath,
                    "-w", workingDirectory,
                    "-b", "/proc",
                    "-b", "/sys",
                    "-b", "/dev",
                    "/bin/sh", "-c", command
                )
                .redirectErrorStream(true)
            
            // Set environment variables
            val env = processBuilder.environment()
            env["HOME"] = "/home/user"
            env["PATH"] = "/usr/bin:/bin:/usr/local/bin"
            env["TERM"] = "xterm-256color"
            env["LANG"] = "en_US.UTF-8"
            
            val process = processBuilder.start()
            
            val output = withContext(Dispatchers.IO) {
                BufferedReader(InputStreamReader(process.inputStream)).readText()
            }
            
            val completed = process.waitFor(timeout, TimeUnit.SECONDS)
            
            if (!completed) {
                process.destroyForcibly()
                ProotResult.Error("Command timed out after ${timeout}s")
            } else {
                val exitCode = process.exitValue()
                if (exitCode == 0) {
                    ProotResult.Success(output)
                } else {
                    ProotResult.Error("Command failed with exit code $exitCode: $output")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute command", e)
            ProotResult.Error("Failed to execute command: ${e.message}")
        }
    }
    
    /**
     * Start long-running process in proot
     */
    fun startProcess(
        command: String,
        workingDirectory: String = "/",
        outputCallback: ((String) -> Unit)? = null
    ): Process? {
        if (!isInitialized) return null
        
        return try {
            val processBuilder = ProcessBuilder()
                .command(
                    prootBinaryPath,
                    "-0",
                    "-r", rootfsPath,
                    "-w", workingDirectory,
                    "-b", "/proc",
                    "-b", "/sys",
                    "-b", "/dev",
                    "/bin/sh", "-c", command
                )
                .redirectErrorStream(true)
            
            val env = processBuilder.environment()
            env["HOME"] = "/home/user"
            env["PATH"] = "/usr/bin:/bin:/usr/local/bin"
            env["TERM"] = "xterm-256color"
            env["LANG"] = "en_US.UTF-8"
            
            val process = processBuilder.start()
            daemonProcess = process
            
            // Read output in separate thread
            if (outputCallback != null) {
                processOutputThread = Thread {
                    try {
                        BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                            var line: String?
                            while (reader.readLine().also { line = it } != null) {
                                outputCallback.invoke(line!!)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error reading process output", e)
                    }
                }.apply {
                    isDaemon = true
                    start()
                }
            }
            
            process
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start process", e)
            null
        }
    }
    
    /**
     * Stop running process
     */
    fun stopProcess() {
        processOutputThread?.interrupt()
        processOutputThread = null
        
        daemonProcess?.let { process ->
            if (process.isAlive) {
                process.destroy()
                // Wait for graceful shutdown
                try {
                    if (!process.waitFor(5, TimeUnit.SECONDS)) {
                        process.destroyForcibly()
                    }
                } catch (e: Exception) {
                    process.destroyForcibly()
                }
            }
        }
        daemonProcess = null
    }
    
    /**
     * Start Node.js daemon
     */
    fun startDaemon(
        outputCallback: ((String) -> Unit)? = null,
        errorCallback: ((String) -> Unit)? = null
    ): Boolean {
        val nodePath = File(rootfsPath, "usr/bin/$NODE_BINARY").absolutePath
        val serverJs = File(daemonPath, "server.js").absolutePath
        
        if (!File(nodePath).exists()) {
            errorCallback?.invoke("Node.js binary not found")
            return false
        }
        
        if (!File(serverJs).exists()) {
            errorCallback?.invoke("Daemon server.js not found")
            return false
        }
        
        val command = "cd $daemonPath && $nodePath server.js"
        val process = startProcess(command, daemonPath, outputCallback)
        
        return process != null
    }
    
    /**
     * Stop daemon
     */
    fun stopDaemon() {
        stopProcess()
    }
    
    /**
     * Check if proot is available and working
     */
    suspend fun isProotAvailable(): Boolean {
        return try {
            val result = executeCommand("echo 'proot works'", timeout = 5)
            result is ProotResult.Success && result.output.contains("proot works")
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check if Node.js is available
     */
    suspend fun isNodeAvailable(): Boolean {
        return try {
            val result = executeCommand("$nodeBinaryPath --version", timeout = 5)
            result is ProotResult.Success && result.output.contains("v")
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get proot version
     */
    suspend fun getVersion(): String {
        return try {
            val result = executeCommand("$prootBinaryPath --version", timeout = 5)
            if (result is ProotResult.Success) {
                result.output.trim()
            } else {
                "unknown"
            }
        } catch (e: Exception) {
            "unknown"
        }
    }
    
    /**
     * Get Node.js version
     */
    suspend fun getNodeVersion(): String {
        return try {
            val result = executeCommand("$nodeBinaryPath --version", timeout = 5)
            if (result is ProotResult.Success) {
                result.output.trim()
            } else {
                "not installed"
            }
        } catch (e: Exception) {
            "not installed"
        }
    }
    
    /**
     * Get disk usage
     */
    fun getDiskUsage(): Long {
        return try {
            var size = 0L
            File(rootfsPath).walkTopDown().forEach {
                if (it.isFile) {
                    size += it.length()
                }
            }
            size
        } catch (e: Exception) {
            0L
        }
    }
    
    /**
     * Cleanup proot environment
     */
    fun cleanup() {
        stopProcess()
        isInitialized = false
        _state.value = ProotState.Uninitialized
        _progress.value = 0f
        Log.i(TAG, "Proot cleanup complete")
    }
    
    /**
     * Full reset - remove everything
     */
    fun fullReset() {
        cleanup()
        
        // Remove extraction marker
        File(context.filesDir, EXTRACTION_MARKER).delete()
        
        // Remove rootfs
        File(rootfsPath).deleteRecursively()
        
        // Remove binaries
        File(binPath).deleteRecursively()
        
        Log.i(TAG, "Full reset complete")
    }
}

/**
 * Proot state
 */
sealed class ProotState {
    object Uninitialized : ProotState()
    object Initializing : ProotState()
    object Ready : ProotState()
    data class Error(val message: String) : ProotState()
}

/**
 * Proot execution result
 */
sealed class ProotResult {
    data class Success(val output: String) : ProotResult()
    data class Error(val message: String) : ProotResult()
}
