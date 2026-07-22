package com.artier.ide.ui.terminal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artier.ide.data.model.TerminalEvent
import com.artier.ide.data.model.TerminalSession
import com.artier.ide.data.model.TerminalState
import com.artier.ide.data.remote.DaemonClient
import com.artier.ide.data.remote.DaemonEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TerminalViewModel @Inject constructor(
    private val daemonClient: DaemonClient,
    val terminalManager: TerminalManager
) : ViewModel() {

    private val _state = MutableStateFlow(TerminalState())
    val state: StateFlow<TerminalState> = _state.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        // Listen for terminal events from daemon
        viewModelScope.launch {
            daemonClient.connectionState.collect { connectionState ->
                // Handle connection state changes
            }
        }
    }

    fun createSession(workingDirectory: String = "/") {
        val session = TerminalSession(workingDirectory = workingDirectory)
        _state.value = _state.value.copy(
            sessions = _state.value.sessions + session,
            activeSessionId = session.id
        )
        
        // Create terminal session via DaemonClient
        daemonClient.createTerminal(workingDirectory)
    }

    fun sendInput(sessionId: String, input: String) {
        // Add to local output buffer
        val currentOutput = _state.value.outputBuffer[sessionId] ?: emptyList()
        _state.value = _state.value.copy(
            outputBuffer = _state.value.outputBuffer + (sessionId to (currentOutput + input))
        )

        // Send to daemon
        daemonClient.sendTerminalInput(sessionId, input)
    }

    fun sendInputToActiveSession(input: String) {
        val activeSessionId = _state.value.activeSessionId ?: return
        sendInput(activeSessionId, input)
    }

    fun resizeTerminal(sessionId: String, cols: Int, rows: Int) {
        daemonClient.resizeTerminal(sessionId, cols, rows)
    }

    fun closeSession(sessionId: String) {
        _state.value = _state.value.copy(
            sessions = _state.value.sessions.filter { it.id != sessionId },
            activeSessionId = if (_state.value.activeSessionId == sessionId) {
                _state.value.sessions.firstOrNull { it.id != sessionId }?.id
            } else {
                _state.value.activeSessionId
            },
            outputBuffer = _state.value.outputBuffer - sessionId
        )

        // Notify daemon
        daemonClient.closeTerminal(sessionId)
    }

    fun switchSession(sessionId: String) {
        _state.value = _state.value.copy(activeSessionId = sessionId)
    }

    fun clearOutput(sessionId: String) {
        _state.value = _state.value.copy(
            outputBuffer = _state.value.outputBuffer + (sessionId to emptyList())
        )
    }

    fun clearAllOutput() {
        _state.value = _state.value.copy(outputBuffer = emptyMap())
    }

    fun handleTerminalOutput(sessionId: String, data: String) {
        val currentOutput = _state.value.outputBuffer[sessionId] ?: emptyList()
        _state.value = _state.value.copy(
            outputBuffer = _state.value.outputBuffer + (sessionId to (currentOutput + data))
        )
    }

    fun handleTerminalExit(sessionId: String, exitCode: Int) {
        // Remove session
        _state.value = _state.value.copy(
            sessions = _state.value.sessions.filter { it.id != sessionId },
            activeSessionId = if (_state.value.activeSessionId == sessionId) {
                _state.value.sessions.firstOrNull { it.id != sessionId }?.id
            } else {
                _state.value.activeSessionId
            }
        )
    }

    fun clearError() {
        _error.value = null
    }

    fun getSession(sessionId: String): TerminalSession? {
        return _state.value.sessions.find { it.id == sessionId }
    }

    fun getActiveSession(): TerminalSession? {
        return _state.value.activeSession
    }

    fun getSessionCount(): Int {
        return _state.value.sessions.size
    }

    fun hasActiveSession(): Boolean {
        return _state.value.activeSessionId != null
    }
}
