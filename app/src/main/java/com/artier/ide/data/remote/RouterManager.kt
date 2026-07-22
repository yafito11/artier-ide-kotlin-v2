package com.artier.ide.data.remote

import android.util.Log
import com.artier.ide.data.model.ProviderStatus
import com.artier.ide.data.model.RouterConfig
import com.artier.ide.data.model.RouterStatus
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RouterManager @Inject constructor() {
    
    companion object {
        private const val TAG = "RouterManager"
        private const val DEFAULT_PORT = 20128
        private const val HEALTH_CHECK_INTERVAL = 5000L
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    
    private val _status = MutableStateFlow(RouterStatus())
    val status: StateFlow<RouterStatus> = _status.asStateFlow()
    
    private var routerProcess: Process? = null
    private var healthCheckThread: Thread? = null
    
    fun start(config: RouterConfig = RouterConfig()): Boolean {
        if (_status.value.isRunning) {
            Log.w(TAG, "Router already running")
            return true
        }
        
        return try {
            _status.value = _status.value.copy(
                isRunning = true,
                port = config.port
            )
            
            startHealthCheck()
            
            Log.i(TAG, "9Router started on port ${config.port}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start 9Router", e)
            _status.value = _status.value.copy(isRunning = false)
            false
        }
    }
    
    fun stop() {
        routerProcess?.destroy()
        routerProcess = null
        
        stopHealthCheck()
        
        _status.value = _status.value.copy(isRunning = false)
        Log.i(TAG, "9Router stopped")
    }
    
    fun restart() {
        stop()
        Thread.sleep(1000)
        start()
    }
    
    fun isRunning(): Boolean {
        return _status.value.isRunning
    }
    
    suspend fun getStatus(): RouterStatus {
        if (!_status.value.isRunning) {
            return _status.value
        }
        
        return try {
            val request = Request.Builder()
                .url("http://127.0.0.1:${_status.value.port}/api/config")
                .get()
                .build()
            
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    val config = gson.fromJson(body, JsonObject::class.java)
                    
                    _status.value.copy(
                        port = config.get("port")?.asInt ?: _status.value.port
                    )
                } else {
                    _status.value
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get status", e)
            _status.value
        }
    }
    
    suspend fun getModels(): List<String> {
        return try {
            val request = Request.Builder()
                .url("http://127.0.0.1:${_status.value.port}/v1/models")
                .get()
                .build()
            
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    val json = gson.fromJson(body, JsonObject::class.java)
                    val models = json.getAsJsonArray("data")
                    
                    models.map { it.asJsonObject.get("id").asString }
                } else {
                    emptyList()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get models", e)
            emptyList()
        }
    }
    
    suspend fun chatCompletion(
        model: String,
        messages: List<Pair<String, String>>,
        temperature: Float = 0.7f,
        maxTokens: Int = 4096
    ): String? {
        return try {
            val requestBody = JsonObject().apply {
                addProperty("model", model)
                add("messages", gson.toJsonTree(messages.map { 
                    mapOf("role" to it.first, "content" to it.second)
                }))
                addProperty("temperature", temperature)
                addProperty("max_tokens", maxTokens)
            }
            
            val request = Request.Builder()
                .url("http://127.0.0.1:${_status.value.port}/v1/chat/completions")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    val json = gson.fromJson(body, JsonObject::class.java)
                    json.getAsJsonArray("choices")
                        ?.get(0)
                        ?.asJsonObject
                        ?.getAsJsonObject("message")
                        ?.get("content")
                        ?.asString
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Chat completion failed", e)
            null
        }
    }
    
    private fun startHealthCheck() {
        healthCheckThread = Thread {
            while (_status.value.isRunning) {
                try {
                    val request = Request.Builder()
                        .url("http://127.0.0.1:${_status.value.port}/health")
                        .get()
                        .build()
                    
                    client.newCall(request).execute().use { }
                    Thread.sleep(HEALTH_CHECK_INTERVAL)
                } catch (e: Exception) {
                    Log.w(TAG, "Health check failed", e)
                    _status.value = _status.value.copy(isRunning = false)
                    break
                }
            }
        }
        healthCheckThread?.start()
    }
    
    private fun stopHealthCheck() {
        healthCheckThread?.interrupt()
        healthCheckThread = null
    }
    
    fun getDashboardUrl(): String {
        return "http://127.0.0.1:${_status.value.port}"
    }

    fun getApiEndpoint(): String {
        return "http://127.0.0.1:${_status.value.port}/v1"
    }
}
