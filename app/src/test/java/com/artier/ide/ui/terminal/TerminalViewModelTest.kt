package com.artier.ide.ui.terminal

import com.artier.ide.data.model.TerminalSession
import com.artier.ide.data.model.TerminalState
import com.artier.ide.data.remote.DaemonClient
import com.artier.ide.data.remote.MockDaemonApi
import com.artier.ide.data.remote.MockWebSocketClient
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TerminalViewModelTest {

    private lateinit var terminalViewModel: TerminalViewModel
    private lateinit var mockDaemonClient: DaemonClient
    private lateinit var terminalManager: TerminalManager

    @Before
    fun setup() {
        val mockWebSocketClient = MockWebSocketClient()
        val mockDaemonApi = MockDaemonApi(mockWebSocketClient)
        mockDaemonClient = DaemonClient(mockDaemonApi, mockWebSocketClient)
        terminalManager = TerminalManager(mockWebSocketClient)
        terminalViewModel = TerminalViewModel(mockDaemonClient, terminalManager)
    }

    @Test
    fun `initial state should have no sessions`() {
        val state = terminalViewModel.state.value
        assertTrue(state.sessions.isEmpty())
        assertNull(state.activeSessionId)
    }

    @Test
    fun `initial error should be null`() {
        assertNull(terminalViewModel.error.value)
    }

    @Test
    fun `createSession should add session to state`() {
        terminalViewModel.createSession("/home/user")

        val state = terminalViewModel.state.value
        assertEquals(1, state.sessions.size)
        assertNotNull(state.activeSessionId)
        assertEquals("/home/user", state.sessions.first().workingDirectory)
    }

    @Test
    fun `createSession should set active session`() {
        terminalViewModel.createSession()

        val state = terminalViewModel.state.value
        assertEquals(state.sessions.first().id, state.activeSessionId)
    }

    @Test
    fun `createMultipleSessions should have multiple sessions`() {
        terminalViewModel.createSession("/home/user")
        terminalViewModel.createSession("/tmp")

        val state = terminalViewModel.state.value
        assertEquals(2, state.sessions.size)
    }

    @Test
    fun `switchSession should change active session`() {
        terminalViewModel.createSession("/home/user")
        terminalViewModel.createSession("/tmp")

        val state = terminalViewModel.state.value
        val secondSessionId = state.sessions[1].id

        terminalViewModel.switchSession(secondSessionId)

        assertEquals(secondSessionId, terminalViewModel.state.value.activeSessionId)
    }

    @Test
    fun `closeSession should remove session`() {
        terminalViewModel.createSession("/home/user")
        val state = terminalViewModel.state.value
        val sessionId = state.sessions.first().id

        terminalViewModel.closeSession(sessionId)

        assertEquals(0, terminalViewModel.state.value.sessions.size)
        assertNull(terminalViewModel.state.value.activeSessionId)
    }

    @Test
    fun `closeSession should switch to another session if available`() {
        terminalViewModel.createSession("/home/user")
        terminalViewModel.createSession("/tmp")

        val state = terminalViewModel.state.value
        val firstSessionId = state.sessions[0].id

        terminalViewModel.closeSession(firstSessionId)

        assertEquals(1, terminalViewModel.state.value.sessions.size)
        assertNotNull(terminalViewModel.state.value.activeSessionId)
    }

    @Test
    fun `clearOutput should clear output for session`() {
        terminalViewModel.createSession()
        val state = terminalViewModel.state.value
        val sessionId = state.sessions.first().id

        terminalViewModel.clearOutput(sessionId)

        // Output should be cleared
        assertTrue(terminalViewModel.state.value.outputBuffer[sessionId]?.isEmpty() ?: true)
    }

    @Test
    fun `clearAllOutput should clear all output`() {
        terminalViewModel.createSession()
        terminalViewModel.createSession()

        terminalViewModel.clearAllOutput()

        assertTrue(terminalViewModel.state.value.outputBuffer.isEmpty())
    }

    @Test
    fun `getSession should return session by id`() {
        terminalViewModel.createSession("/home/user")
        val state = terminalViewModel.state.value
        val sessionId = state.sessions.first().id

        val session = terminalViewModel.getSession(sessionId)

        assertNotNull(session)
        assertEquals(sessionId, session?.id)
    }

    @Test
    fun `getSession should return null for nonexistent id`() {
        val session = terminalViewModel.getSession("nonexistent")
        assertNull(session)
    }

    @Test
    fun `getActiveSession should return active session`() {
        terminalViewModel.createSession("/home/user")

        val activeSession = terminalViewModel.getActiveSession()

        assertNotNull(activeSession)
        assertEquals(terminalViewModel.state.value.activeSessionId, activeSession?.id)
    }

    @Test
    fun `getActiveSession should return null when no active session`() {
        val activeSession = terminalViewModel.getActiveSession()
        assertNull(activeSession)
    }

    @Test
    fun `getSessionCount should return correct count`() {
        assertEquals(0, terminalViewModel.getSessionCount())

        terminalViewModel.createSession()
        assertEquals(1, terminalViewModel.getSessionCount())

        terminalViewModel.createSession()
        assertEquals(2, terminalViewModel.getSessionCount())
    }

    @Test
    fun `hasActiveSession should return true when there is an active session`() {
        terminalViewModel.createSession()
        assertTrue(terminalViewModel.hasActiveSession())
    }

    @Test
    fun `hasActiveSession should return false when there is no active session`() {
        assertFalse(terminalViewModel.hasActiveSession())
    }

    @Test
    fun `clearError should clear error state`() {
        terminalViewModel.clearError()
        assertNull(terminalViewModel.error.value)
    }

    @Test
    fun `handleTerminalOutput should add to output buffer`() {
        terminalViewModel.createSession()
        val state = terminalViewModel.state.value
        val sessionId = state.sessions.first().id

        terminalViewModel.handleTerminalOutput(sessionId, "test output")

        val output = terminalViewModel.state.value.outputBuffer[sessionId]
        assertNotNull(output)
        assertTrue(output?.contains("test output") ?: false)
    }

    @Test
    fun `handleTerminalExit should remove session`() {
        terminalViewModel.createSession()
        val state = terminalViewModel.state.value
        val sessionId = state.sessions.first().id

        terminalViewModel.handleTerminalExit(sessionId, 0)

        assertEquals(0, terminalViewModel.state.value.sessions.size)
    }
}
