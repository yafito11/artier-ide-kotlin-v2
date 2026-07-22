package com.artier.ide.ui.router

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artier.ide.data.model.ProviderStatus
import com.artier.ide.data.model.QuotaUsage
import com.artier.ide.data.model.RouterConfig
import com.artier.ide.data.model.RouterEvent
import com.artier.ide.data.model.RouterStatus
import com.artier.ide.data.remote.RouterManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RouterViewModel @Inject constructor(
    private val routerManager: RouterManager
) : ViewModel() {
    
    private val _status = MutableStateFlow(RouterStatus())
    val status: StateFlow<RouterStatus> = _status.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _showWebView = MutableStateFlow(false)
    val showWebView: StateFlow<Boolean> = _showWebView.asStateFlow()
    
    private val _models = MutableStateFlow<List<String>>(emptyList())
    val models: StateFlow<List<String>> = _models.asStateFlow()
    
    init {
        // Observe router status
        viewModelScope.launch {
            routerManager.status.collect { status ->
                _status.value = status
            }
        }
        
        // Load models if router is running
        viewModelScope.launch {
            if (routerManager.isRunning()) {
                loadModels()
            }
        }
    }
    
    fun startRouter(config: RouterConfig = RouterConfig()) {
        viewModelScope.launch {
            val success = routerManager.start(config)
            if (success) {
                loadModels()
            } else {
                _error.value = "Failed to start 9Router"
            }
        }
    }
    
    fun stopRouter() {
        routerManager.stop()
        _models.value = emptyList()
    }
    
    fun restartRouter() {
        routerManager.restart()
        viewModelScope.launch {
            loadModels()
        }
    }
    
    fun showDashboard() {
        _showWebView.value = true
    }
    
    fun hideDashboard() {
        _showWebView.value = false
    }
    
    fun getDashboardUrl(): String {
        return routerManager.getDashboardUrl()
    }
    
    fun getApiEndpoint(): String {
        return routerManager.getApiEndpoint()
    }
    
    private suspend fun loadModels() {
        val models = routerManager.getModels()
        _models.value = models
    }
    
    fun clearError() {
        _error.value = null
    }
    
    fun isRunning(): Boolean {
        return routerManager.isRunning()
    }
}