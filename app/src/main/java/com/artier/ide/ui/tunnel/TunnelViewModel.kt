package com.artier.ide.ui.tunnel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artier.ide.data.model.DetectedPort
import com.artier.ide.data.model.TunnelEvent
import com.artier.ide.data.model.TunnelSession
import com.artier.ide.data.model.TunnelState
import com.artier.ide.data.model.TunnelStatus
import com.artier.ide.data.remote.DaemonClient
import com.artier.ide.data.remote.DaemonEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TunnelViewModel @Inject constructor(
    private val daemonClient: DaemonClient
) : ViewModel() {

    private val _state = MutableStateFlow(TunnelState())
    val state: StateFlow<TunnelState> = _state.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        // Listen for tunnel events from daemon
        viewModelScope.launch {
            daemonClient.connectionState.collect { connectionState ->
                // Handle connection state changes
            }
        }
    }

    fun createTunnel(port: Int, service: String = "http") {
        _state.value = _state.value.copy(isLoading = true)
        daemonClient.createTunnel(port, service)
    }

    fun closeTunnel(tunnelId: String) {
        daemonClient.closeTunnel(tunnelId)
    }

    fun getTunnelStatus(tunnelId: String) {
        daemonClient.getTunnelStatus(tunnelId)
    }

    fun detectPorts() {
        daemonClient.detectPorts()
    }

    fun closeActiveTunnel() {
        val activeSessionId = _state.value.activeSessionId ?: return
        closeTunnel(activeSessionId)
    }

    fun handleTunnelCreated(tunnelId: String, url: String, port: Int) {
        val session = TunnelSession(
            id = tunnelId,
            port = port,
            url = url,
            status = TunnelStatus.Active
        )

        _state.value = _state.value.copy(
            sessions = _state.value.sessions + session,
            activeSessionId = tunnelId,
            isLoading = false
        )
    }

    fun handleTunnelClosed(tunnelId: String, exitCode: Int?) {
        _state.value = _state.value.copy(
            sessions = _state.value.sessions.map { session ->
                if (session.id == tunnelId) {
                    session.copy(status = TunnelStatus.Closed)
                } else {
                    session
                }
            },
            activeSessionId = if (_state.value.activeSessionId == tunnelId) {
                null
            } else {
                _state.value.activeSessionId
            }
        )
    }

    fun handleTunnelStatus(
        tunnelId: String,
        status: String,
        url: String?,
        port: Int,
        service: String,
        uptime: Int
    ) {
        val tunnelStatus = when (status) {
            "active" -> TunnelStatus.Active
            "error" -> TunnelStatus.Error
            "closed" -> TunnelStatus.Closed
            else -> TunnelStatus.Pending
        }

        _state.value = _state.value.copy(
            sessions = _state.value.sessions.map { session ->
                if (session.id == tunnelId) {
                    session.copy(
                        status = tunnelStatus,
                        url = url,
                        lastActivity = System.currentTimeMillis()
                    )
                } else {
                    session
                }
            }
        )
    }

    fun handlePortsDetected(ports: List<DetectedPort>) {
        _state.value = _state.value.copy(detectedPorts = ports)
    }

    fun clearError() {
        _error.value = null
    }

    fun getActiveTunnelUrl(): String? {
        return _state.value.activeSession?.url
    }

    fun hasActiveTunnel(): Boolean {
        return _state.value.hasActiveTunnel
    }
}
