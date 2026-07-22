package com.artier.ide.data.remote

import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * LSP client that talks to the daemon WebSocket bridge
 * (daemon owns language-server processes in proot).
 */
@Singleton
class LspClient @Inject constructor(
    private val webSocketClient: WebSocketClient
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val servers = ConcurrentHashMap<String, LspServer>()
    private val documentVersions = ConcurrentHashMap<String, AtomicInteger>()

    private val _connectionState = MutableStateFlow<LspConnectionState>(LspConnectionState.Disconnected)
    val connectionState: StateFlow<LspConnectionState> = _connectionState.asStateFlow()

    private val _diagnostics = MutableSharedFlow<DiagnosticsEvent>(extraBufferCapacity = 32)
    val diagnostics: SharedFlow<DiagnosticsEvent> = _diagnostics.asSharedFlow()

    private val _completions = MutableSharedFlow<CompletionsEvent>(extraBufferCapacity = 16)
    val completions: SharedFlow<CompletionsEvent> = _completions.asSharedFlow()

    private val _hover = MutableSharedFlow<HoverEvent>(extraBufferCapacity = 16)
    val hover: SharedFlow<HoverEvent> = _hover.asSharedFlow()

    data class LspServer(
        val id: String,
        val language: String,
        val rootUri: String,
        var isInitialized: Boolean = false
    )

    sealed class LspConnectionState {
        object Disconnected : LspConnectionState()
        object Connecting : LspConnectionState()
        data class Connected(val serverCount: Int) : LspConnectionState()
        data class Error(val message: String) : LspConnectionState()
    }

    data class CompletionItem(
        val label: String,
        val kind: Int? = null,
        val detail: String? = null,
        val documentation: String? = null,
        val insertText: String? = null
    )

    data class Diagnostic(
        val range: Range,
        val severity: Int,
        val message: String,
        val source: String? = null
    )

    data class Range(
        val startLine: Int,
        val startCharacter: Int,
        val endLine: Int,
        val endCharacter: Int
    )

    data class DiagnosticsEvent(val fileUri: String, val diagnostics: List<Diagnostic>)
    data class CompletionsEvent(val fileUri: String, val items: List<CompletionItem>)
    data class HoverEvent(val fileUri: String, val contents: String?)

    data class TextEdit(val range: Range, val newText: String)

    init {
        webSocketClient.addMessageHandler("lsp_initialized") { payload ->
            val language = payload.optString("language")
            val serverId = payload.optString("serverId")
            val rootHint = servers.values.find { it.language == language }?.rootUri ?: ""
            val id = serverId.ifBlank { "${language}_${rootHint.hashCode()}" }
            servers[id] = LspServer(
                id = id,
                language = language,
                rootUri = rootHint,
                isInitialized = true
            )
            _connectionState.value = LspConnectionState.Connected(servers.size)
        }

        webSocketClient.addMessageHandler("lsp_completion") { payload ->
            val fileUri = payload.optString("fileUri")
            val itemsJson = payload.optJSONArray("items")
            val items = mutableListOf<CompletionItem>()
            if (itemsJson != null) {
                for (i in 0 until itemsJson.length()) {
                    val obj = itemsJson.optJSONObject(i) ?: continue
                    items.add(
                        CompletionItem(
                            label = obj.optString("label"),
                            kind = if (obj.has("kind")) obj.optInt("kind") else null,
                            detail = obj.optString("detail").ifBlank { null },
                            documentation = obj.optString("documentation").ifBlank { null },
                            insertText = obj.optString("insertText").ifBlank { null }
                        )
                    )
                }
            }
            _completions.tryEmit(CompletionsEvent(fileUri, items))
        }

        webSocketClient.addMessageHandler("lsp_hover") { payload ->
            val fileUri = payload.optString("fileUri")
            val contents = payload.opt("contents")
            val text = when (contents) {
                is String -> contents
                is JSONObject -> contents.optString("value")
                else -> contents?.toString()
            }
            _hover.tryEmit(HoverEvent(fileUri, text))
        }

        webSocketClient.addMessageHandler("lsp_error") { payload ->
            _connectionState.value = LspConnectionState.Error(payload.optString("message"))
        }

        webSocketClient.addMessageHandler("error") { payload ->
            val msg = payload.optString("message")
            if (msg.contains("LSP", ignoreCase = true)) {
                _connectionState.value = LspConnectionState.Error(msg)
            }
        }
    }

    fun initialize(language: String, rootUri: String) {
        val serverId = "${language}_${rootUri.hashCode()}"
        if (servers[serverId]?.isInitialized == true) return

        _connectionState.value = LspConnectionState.Connecting
        servers[serverId] = LspServer(serverId, language, rootUri, isInitialized = false)

        val data = JsonObject().apply {
            addProperty("language", language)
            addProperty("rootUri", rootUri)
            addProperty("serverId", serverId)
        }
        webSocketClient.send("lsp_initialize", data)
    }

    fun ensureLanguage(language: String, rootUri: String) {
        if (findServerId(language) == null) {
            initialize(language, rootUri)
        }
    }

    fun getCompletions(language: String, fileUri: String, line: Int, character: Int) {
        val serverId = findServerId(language) ?: return
        val data = JsonObject().apply {
            addProperty("serverId", serverId)
            addProperty("fileUri", fileUri)
            addProperty("line", line)
            addProperty("character", character)
        }
        webSocketClient.send("lsp_completion", data)
    }

    fun getHover(language: String, fileUri: String, line: Int, character: Int) {
        val serverId = findServerId(language) ?: return
        val data = JsonObject().apply {
            addProperty("serverId", serverId)
            addProperty("fileUri", fileUri)
            addProperty("line", line)
            addProperty("character", character)
        }
        webSocketClient.send("lsp_hover", data)
    }

    fun didOpen(language: String, fileUri: String, content: String, version: Int = 1) {
        documentVersions[fileUri] = AtomicInteger(version)
        val data = JsonObject().apply {
            addProperty("language", language)
            addProperty("fileUri", fileUri)
            addProperty("content", content)
            addProperty("version", version)
            findServerId(language)?.let { addProperty("serverId", it) }
        }
        webSocketClient.send("lsp_did_open", data)
    }

    fun didChange(language: String, fileUri: String, content: String) {
        val version = documentVersions.getOrPut(fileUri) { AtomicInteger(1) }.incrementAndGet()
        val data = JsonObject().apply {
            addProperty("language", language)
            addProperty("fileUri", fileUri)
            addProperty("content", content)
            addProperty("version", version)
            findServerId(language)?.let { addProperty("serverId", it) }
        }
        webSocketClient.send("lsp_did_change", data)
    }

    fun didSave(language: String, fileUri: String, content: String? = null) {
        val data = JsonObject().apply {
            addProperty("language", language)
            addProperty("fileUri", fileUri)
            if (content != null) addProperty("content", content)
            findServerId(language)?.let { addProperty("serverId", it) }
        }
        webSocketClient.send("lsp_did_save", data)
    }

    fun didClose(language: String, fileUri: String) {
        documentVersions.remove(fileUri)
        val data = JsonObject().apply {
            addProperty("language", language)
            addProperty("fileUri", fileUri)
            findServerId(language)?.let { addProperty("serverId", it) }
        }
        webSocketClient.send("lsp_did_close", data)
    }

    fun formatDocument(language: String, fileUri: String) {
        val serverId = findServerId(language) ?: return
        val data = JsonObject().apply {
            addProperty("serverId", serverId)
            addProperty("fileUri", fileUri)
        }
        webSocketClient.send("lsp_format", data)
    }

    fun shutdown(serverId: String) {
        val data = JsonObject().apply {
            addProperty("serverId", serverId)
        }
        webSocketClient.send("lsp_stop", data)
        servers.remove(serverId)
        _connectionState.value = if (servers.isEmpty()) {
            LspConnectionState.Disconnected
        } else {
            LspConnectionState.Connected(servers.size)
        }
    }

    fun shutdownAll() {
        for (serverId in servers.keys.toList()) {
            shutdown(serverId)
        }
    }

    fun languageFromPath(path: String): String {
        val ext = path.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "kt", "kts" -> "kotlin"
            "java" -> "java"
            "py" -> "python"
            "js", "mjs", "cjs" -> "javascript"
            "ts" -> "typescript"
            "tsx" -> "typescriptreact"
            "jsx" -> "javascriptreact"
            "rs" -> "rust"
            "go" -> "go"
            "c", "h" -> "c"
            "cpp", "hpp", "cc", "cxx" -> "cpp"
            "json" -> "json"
            "html", "htm" -> "html"
            "css" -> "css"
            else -> "plaintext"
        }
    }

    fun pathToUri(path: String): String {
        return if (path.startsWith("file://")) path else "file://$path"
    }

    private fun findServerId(language: String): String? {
        return servers.entries.firstOrNull { it.value.language == language && it.value.isInitialized }?.key
            ?: servers.entries.firstOrNull { it.value.language == language }?.key
    }

    fun destroy() {
        shutdownAll()
        scope.cancel()
    }
}
