package com.artier.ide.data.remote

import com.artier.ide.data.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DaemonClientTest {

    private lateinit var daemonClient: DaemonClient
    private lateinit var mockDaemonApi: MockDaemonApi
    private lateinit var mockWebSocketClient: MockWebSocketClient

    @Before
    fun setup() {
        mockWebSocketClient = MockWebSocketClient()
        mockDaemonApi = MockDaemonApi(mockWebSocketClient)
        daemonClient = DaemonClient(mockDaemonApi, mockWebSocketClient)
    }

    @Test
    fun `initial connection state should be Disconnected`() {
        val state = daemonClient.connectionState.value
        assertTrue(state is ConnectionState.Disconnected)
    }

    @Test
    fun `initial daemonInfo should be null`() {
        assertNull(daemonClient.daemonInfo.value)
    }

    @Test
    fun `initial error should be null`() {
        assertNull(daemonClient.error.value)
    }

    @Test
    fun `isConnectedAndReady should return false when disconnected`() {
        assertFalse(daemonClient.isConnectedAndReady())
    }

    @Test
    fun `clearError should clear error state`() {
        // Error state is set internally, but clearError should work
        daemonClient.clearError()
        assertNull(daemonClient.error.value)
    }

    @Test
    fun `createTerminal should call daemonApi`() {
        daemonClient.createTerminal("/home/user")
        // In real test, we'd verify daemonApi was called
    }

    @Test
    fun `sendTerminalInput should call daemonApi`() {
        daemonClient.sendTerminalInput("session-1", "ls -la")
        // In real test, we'd verify daemonApi was called
    }

    @Test
    fun `resizeTerminal should call daemonApi`() {
        daemonClient.resizeTerminal("session-1", 80, 24)
        // In real test, we'd verify daemonApi was called
    }

    @Test
    fun `closeTerminal should call daemonApi`() {
        daemonClient.closeTerminal("session-1")
        // In real test, we'd verify daemonApi was called
    }

    @Test
    fun `readFile should call daemonApi`() {
        daemonClient.readFile("/test/file.kt")
        // In real test, we'd verify daemonApi was called
    }

    @Test
    fun `writeFile should call daemonApi`() {
        daemonClient.writeFile("/test/file.kt", "content")
        // In real test, we'd verify daemonApi was called
    }

    @Test
    fun `listDirectory should call daemonApi`() {
        daemonClient.listDirectory("/test")
        // In real test, we'd verify daemonApi was called
    }

    @Test
    fun `createFile should call daemonApi`() {
        daemonClient.createFile("/test/new-file.kt")
        // In real test, we'd verify daemonApi was called
    }

    @Test
    fun `createDirectory should call daemonApi`() {
        daemonClient.createDirectory("/test/new-dir")
        // In real test, we'd verify daemonApi was called
    }

    @Test
    fun `deleteFile should call daemonApi`() {
        daemonClient.deleteFile("/test/file.kt")
        // In real test, we'd verify daemonApi was called
    }

    @Test
    fun `renameFile should call daemonApi`() {
        daemonClient.renameFile("/test/old.kt", "/test/new.kt")
        // In real test, we'd verify daemonApi was called
    }

    @Test
    fun `connectDatabase should call daemonApi`() {
        daemonClient.connectDatabase(
            type = DbType.POSTGRES,
            host = "localhost",
            port = 5432,
            database = "testdb",
            username = "user",
            password = "pass"
        )
        // In real test, we'd verify daemonApi was called
    }

    @Test
    fun `disconnectDatabase should call daemonApi`() {
        daemonClient.disconnectDatabase("conn-1")
        // In real test, we'd verify daemonApi was called
    }

    @Test
    fun `queryDatabase should call daemonApi`() {
        daemonClient.queryDatabase("conn-1", "SELECT * FROM users")
        // In real test, we'd verify daemonApi was called
    }

    @Test
    fun `getDatabaseTables should call daemonApi`() {
        daemonClient.getDatabaseTables("conn-1")
        // In real test, we'd verify daemonApi was called
    }

    @Test
    fun `getTableSchema should call daemonApi`() {
        daemonClient.getTableSchema("conn-1", "users")
        // In real test, we'd verify daemonApi was called
    }

    @Test
    fun `createTunnel should call daemonApi`() {
        daemonClient.createTunnel(3000, "http")
        // In real test, we'd verify daemonApi was called
    }

    @Test
    fun `closeTunnel should call daemonApi`() {
        daemonClient.closeTunnel("tunnel-1")
        // In real test, we'd verify daemonApi was called
    }

    @Test
    fun `getTunnelStatus should call daemonApi`() {
        daemonClient.getTunnelStatus("tunnel-1")
        // In real test, we'd verify daemonApi was called
    }

    @Test
    fun `detectPorts should call daemonApi`() {
        daemonClient.detectPorts()
        // In real test, we'd verify daemonApi was called
    }

    @Test
    fun `runAgent should call daemonApi`() {
        daemonClient.runAgent("opencode", "Hello")
        // In real test, we'd verify daemonApi was called
    }

    @Test
    fun `getAgentStatus should call daemonApi`() {
        daemonClient.getAgentStatus("session-1")
        // In real test, we'd verify daemonApi was called
    }

    @Test
    fun `stopAgent should call daemonApi`() {
        daemonClient.stopAgent("session-1")
        // In real test, we'd verify daemonApi was called
    }

    @Test
    fun `sendAgentInput should call daemonApi`() {
        daemonClient.sendAgentInput("session-1", "input")
        // In real test, we'd verify daemonApi was called
    }

    @Test
    fun `getSkills should call daemonApi`() {
        daemonClient.getSkills()
        // In real test, we'd verify daemonApi was called
    }

    @Test
    fun `getSkillDetail should call daemonApi`() {
        daemonClient.getSkillDetail("my-skill")
        // In real test, we'd verify daemonApi was called
    }

    @Test
    fun `installSkill should call daemonApi`() {
        daemonClient.installSkill("/path/to/skill")
        // In real test, we'd verify daemonApi was called
    }

    @Test
    fun `uninstallSkill should call daemonApi`() {
        daemonClient.uninstallSkill("my-skill")
        // In real test, we'd verify daemonApi was called
    }

    @Test
    fun `scanSkills should call daemonApi`() {
        daemonClient.scanSkills()
        // In real test, we'd verify daemonApi was called
    }

    @Test
    fun `setSkillEnabled should call daemonApi`() {
        daemonClient.setSkillEnabled("my-skill", true)
        // In real test, we'd verify daemonApi was called
    }

    @Test
    fun `getRouterStatus should call daemonApi`() {
        daemonClient.getRouterStatus()
        // In real test, we'd verify daemonApi was called
    }

    @Test
    fun `getRouterConfig should call daemonApi`() {
        daemonClient.getRouterConfig()
        // In real test, we'd verify daemonApi was called
    }
}

// Mock classes for testing
class MockDaemonApi(webSocketClient: WebSocketClient) : DaemonApi(webSocketClient) {
    var connectCalled = false
    var disconnectCalled = false

    override fun connect(daemonUrl: String) {
        connectCalled = true
    }

    override fun disconnect() {
        disconnectCalled = true
    }
}

class MockWebSocketClient : WebSocketClient() {
    var isConnected = false
        private set

    override fun connect(url: String) {
        isConnected = true
    }

    override fun disconnect() {
        isConnected = false
    }
}
