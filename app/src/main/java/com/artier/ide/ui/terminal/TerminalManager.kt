package com.artier.ide.ui.terminal

import com.artier.ide.data.remote.WebSocketClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TerminalManager — WebSocket bridge to daemon PTY (node-pty).
 */
@Singleton
class TerminalManager @Inject constructor(
    private val webSocketClient: WebSocketClient
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sessions = mutableMapOf<String, TerminalSessionCallbacks>()

    data class TerminalSessionCallbacks(
        val sessionId: String,
        var onOutput: ((String) -> Unit)? = null,
        var onExit: ((Int) -> Unit)? = null
    )

    init {
        webSocketClient.addMessageHandler("terminal_output") { payload ->
            val sessionId = payload.optString("sessionId")
            val data = payload.optString("data")
            sessions[sessionId]?.onOutput?.invoke(data)
        }

        webSocketClient.addMessageHandler("terminal_exit") { payload ->
            val sessionId = payload.optString("sessionId")
            val exitCode = payload.optInt("exitCode", -1)
            sessions[sessionId]?.onExit?.invoke(exitCode)
            sessions.remove(sessionId)
        }
    }

    fun createSession(
        sessionId: String,
        workingDirectory: String = "/",
        cols: Int = 80,
        rows: Int = 24,
        onOutput: (String) -> Unit,
        onExit: (Int) -> Unit
    ) {
        sessions[sessionId] = TerminalSessionCallbacks(sessionId, onOutput, onExit)

        scope.launch {
            webSocketClient.send(
                JSONObject().apply {
                    put("type", "terminal_create")
                    put("payload", JSONObject().apply {
                        put("sessionId", sessionId)
                        put("workingDirectory", workingDirectory)
                        put("cols", cols)
                        put("rows", rows)
                    })
                }
            )
        }
    }

    fun sendInput(sessionId: String, data: String) {
        scope.launch {
            webSocketClient.send(
                JSONObject().apply {
                    put("type", "terminal_input")
                    put("payload", JSONObject().apply {
                        put("sessionId", sessionId)
                        put("data", data)
                    })
                }
            )
        }
    }

    fun resize(sessionId: String, cols: Int, rows: Int) {
        scope.launch {
            webSocketClient.send(
                JSONObject().apply {
                    put("type", "terminal_resize")
                    put("payload", JSONObject().apply {
                        put("sessionId", sessionId)
                        put("cols", cols)
                        put("rows", rows)
                    })
                }
            )
        }
    }

    fun closeSession(sessionId: String) {
        scope.launch {
            webSocketClient.send(
                JSONObject().apply {
                    put("type", "terminal_close")
                    put("payload", JSONObject().apply {
                        put("sessionId", sessionId)
                    })
                }
            )
        }
        sessions.remove(sessionId)
    }

    fun destroy() {
        sessions.keys.toList().forEach { closeSession(it) }
        scope.cancel()
    }
}
