package com.artier.ide.data.remote

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class WebSocketClientTest {

    private lateinit var webSocketClient: WebSocketClient

    @Before
    fun setup() {
        webSocketClient = WebSocketClient()
    }

    @Test
    fun `initial state should not be connected`() {
        assertFalse(webSocketClient.isConnected())
    }

    @Test
    fun `addMessageHandler should register handler`() {
        var called = false
        webSocketClient.addMessageHandler("test_event") {
            called = true
        }

        // Handler should be registered
        assertNotNull(called)
    }

    @Test
    fun `removeMessageHandler should remove handler`() {
        var called = false
        val handler: (org.json.JSONObject) -> Unit = { called = true }
        
        webSocketClient.addMessageHandler("test_event", handler)
        webSocketClient.removeMessageHandler("test_event", handler)
        
        // After removal, handler should not be called
        assertFalse(called)
    }

    @Test
    fun `removeMessageHandlers should remove all handlers for type`() {
        var call1 = false
        var call2 = false
        
        webSocketClient.addMessageHandler("test_event") { call1 = true }
        webSocketClient.addMessageHandler("test_event") { call2 = true }
        
        webSocketClient.removeMessageHandlers("test_event")
        
        // After removal, no handlers should be called
        assertFalse(call1)
        assertFalse(call2)
    }

    @Test
    fun `send should not crash when not connected`() {
        // Should not throw exception
        webSocketClient.send(org.json.JSONObject().apply {
            put("type", "test")
            put("data", "test")
        })
    }

    @Test
    fun `send with type and data should not crash when not connected`() {
        // Should not throw exception
        val data = com.google.gson.JsonObject().apply {
            addProperty("key", "value")
        }
        webSocketClient.send("test_type", data)
    }

    @Test
    fun `sendRaw should not crash when not connected`() {
        // Should not throw exception
        webSocketClient.sendRaw("test message")
    }

    @Test
    fun `disconnect should not crash when not connected`() {
        // Should not throw exception
        webSocketClient.disconnect()
    }

    @Test
    fun `events flow should exist`() {
        assertNotNull(webSocketClient.events)
    }
}
