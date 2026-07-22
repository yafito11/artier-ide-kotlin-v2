package com.artier.ide.proot

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ProotManagerTest {

    private lateinit var prootManager: ProotManager

    @Before
    fun setup() {
        // Note: ProotManager requires Context, so we'd use Robolectric or mock in real tests
        // For now, we test the logic without Android context
    }

    @Test
    fun `ProotState should have all states`() {
        val states = listOf(
            ProotState.Uninitialized,
            ProotState.Initializing,
            ProotState.Ready,
            ProotState.Error("test error")
        )

        assertEquals(4, states.size)
        assertTrue(states[0] is ProotState.Uninitialized)
        assertTrue(states[1] is ProotState.Initializing)
        assertTrue(states[2] is ProotState.Ready)
        assertTrue(states[3] is ProotState.Error)
    }

    @Test
    fun `ProotResult should have Success and Error states`() {
        val success = ProotResult.Success("output")
        val error = ProotResult.Error("error message")

        assertTrue(success is ProotResult.Success)
        assertTrue(error is ProotResult.Error)
        assertEquals("output", success.output)
        assertEquals("error message", error.message)
    }

    @Test
    fun `ProotResult Success should contain output`() {
        val output = "test output"
        val result = ProotResult.Success(output)

        assertEquals(output, result.output)
    }

    @Test
    fun `ProotResult Error should contain message`() {
        val message = "test error"
        val result = ProotResult.Error(message)

        assertEquals(message, result.message)
    }

    @Test
    fun `ProotState Error should contain message`() {
        val message = "initialization failed"
        val state = ProotState.Error(message)

        assertEquals(message, state.message)
    }

    @Test
    fun `ProotState equality should work`() {
        val state1 = ProotState.Error("test")
        val state2 = ProotState.Error("test")
        val state3 = ProotState.Error("different")

        assertEquals(state1, state2)
        assertNotEquals(state1, state3)
    }

    @Test
    fun `ProotResult equality should work`() {
        val result1 = ProotResult.Success("output")
        val result2 = ProotResult.Success("output")
        val result3 = ProotResult.Success("different")

        assertEquals(result1, result2)
        assertNotEquals(result1, result3)
    }
}
