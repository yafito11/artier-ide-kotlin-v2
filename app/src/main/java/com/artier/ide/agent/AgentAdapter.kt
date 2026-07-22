package com.artier.ide.agent

import kotlinx.coroutines.flow.Flow

/**
 * Base interface for all agent adapters
 * Each adapter implements this interface to integrate with a specific CLI agent
 */
interface AgentAdapter {
    /**
     * Name of the agent
     */
    val name: String
    
    /**
     * Version of the adapter
     */
    val version: String
    
    /**
     * Check if the agent is available on the system
     */
    fun isAvailable(): Boolean
    
    /**
     * Get the executable command for the agent
     */
    fun getCommand(): String
    
    /**
     * Get default arguments for the agent
     */
    fun getDefaultArgs(): List<String>
    
    /**
     * Get capabilities of this agent
     */
    fun getCapabilities(): List<AgentCapability>
    
    /**
     * Get default configuration
     */
    fun getDefaultConfig(): AgentConfig
    
    /**
     * Spawn the agent and return a flow of events
     */
    fun spawn(config: AgentConfig): Flow<AgentEvent>
    
    /**
     * Send input to a running agent session
     */
    fun sendInput(sessionId: String, input: String)
    
    /**
     * Stop a running agent session
     */
    fun stop(sessionId: String)
    
    /**
     * Parse a line of output from the agent
     * Returns an AgentEvent or null if the line should be ignored
     */
    fun parseOutput(line: String): AgentEvent?
}

/**
 * Capabilities of an agent
 */
enum class AgentCapability {
    CODE_GENERATION,
    CODE_REVIEW,
    BUG_FIXING,
    REFACTORING,
    DOCUMENTATION,
    TESTING,
    EXPLANATION,
    CHAT,
    TOOL_USE,
    FILE_OPERATIONS,
    SHELL_COMMANDS
}

/**
 * Configuration for spawning an agent
 */
data class AgentConfig(
    val workingDirectory: String,
    val model: String? = null,
    val apiKey: String? = null,
    val environment: Map<String, String> = emptyMap(),
    val args: List<String> = emptyList(),
    val timeout: Long = 300000, // 5 minutes
    val maxTokens: Int = 4096,
    val temperature: Float = 0.7f
)

/**
 * Events emitted by agent adapters
 */
sealed class AgentEvent {
    /**
     * Agent session started
     */
    data class Started(
        val sessionId: String,
        val agentName: String
    ) : AgentEvent()
    
    /**
     * Agent output (regular text)
     */
    data class Output(
        val sessionId: String,
        val text: String
    ) : AgentEvent()
    
    /**
     * Agent is thinking/processing
     */
    data class Thinking(
        val sessionId: String,
        val text: String? = null
    ) : AgentEvent()
    
    /**
     * Tool call request from agent
     */
    data class ToolCall(
        val sessionId: String,
        val tool: ToolCallRequest
    ) : AgentEvent()
    
    /**
     * Tool call result
     */
    data class ToolResult(
        val sessionId: String,
        val result: ToolCallResult
    ) : AgentEvent()
    
    /**
     * Error occurred
     */
    data class Error(
        val sessionId: String,
        val error: String,
        val isRecoverable: Boolean = true
    ) : AgentEvent()
    
    /**
     * Agent session completed
     */
    data class Completed(
        val sessionId: String,
        val exitCode: Int,
        val summary: String? = null
    ) : AgentEvent()
    
    /**
     * Status update
     */
    data class Status(
        val sessionId: String,
        val status: String
    ) : AgentEvent()
}

/**
 * Tool call request from agent
 */
data class ToolCallRequest(
    val id: String,
    val name: String,
    val arguments: Map<String, Any> = emptyMap()
)

/**
 * Tool call result
 */
data class ToolCallResult(
    val id: String,
    val toolCallId: String,
    val output: String,
    val isError: Boolean = false
)

/**
 * Agent session information
 */
data class AgentSession(
    val id: String,
    val agentName: String,
    val config: AgentConfig,
    val startTime: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val messageCount: Int = 0
)

/**
 * Agent registry for managing multiple adapters
 */
class AgentRegistry {
    private val adapters = mutableMapOf<String, AgentAdapter>()
    
    fun register(adapter: AgentAdapter) {
        adapters[adapter.name] = adapter
    }
    
    fun unregister(name: String) {
        adapters.remove(name)
    }
    
    fun getAdapter(name: String): AgentAdapter? {
        return adapters[name]
    }
    
    fun getAvailableAdapters(): List<AgentAdapter> {
        return adapters.values.filter { it.isAvailable() }
    }
    
    fun getAllAdapters(): List<AgentAdapter> {
        return adapters.values.toList()
    }
    
    fun getAdapterNames(): List<String> {
        return adapters.keys.toList()
    }
}