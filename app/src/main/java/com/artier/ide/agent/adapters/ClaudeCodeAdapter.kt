package com.artier.ide.agent.adapters

import com.artier.ide.agent.AgentAdapter
import com.artier.ide.agent.AgentCapability
import com.artier.ide.agent.AgentConfig
import com.artier.ide.agent.AgentEvent
import com.artier.ide.agent.AgentRouterEnv
import com.artier.ide.agent.ToolCallRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Adapter for Claude Code CLI agent
 * https://github.com/anthropics/claude-code
 */
class ClaudeCodeAdapter : AgentAdapter {
    
    override val name = "claude-code"
    override val version = "1.0.0"
    
    private val command = "claude"
    
    override fun isAvailable(): Boolean {
        return try {
            val process = ProcessBuilder(command, "--version")
                .redirectErrorStream(true)
                .start()
            
            val exited = process.waitFor(5, TimeUnit.SECONDS)
            exited && process.exitValue() == 0
        } catch (e: Exception) {
            false
        }
    }
    
    override fun getCommand(): String = command
    
    override fun getDefaultArgs(): List<String> {
        return listOf(
            "--print",
            "--output-format", "json"
        )
    }
    
    override fun getCapabilities(): List<AgentCapability> {
        return listOf(
            AgentCapability.CODE_GENERATION,
            AgentCapability.CODE_REVIEW,
            AgentCapability.BUG_FIXING,
            AgentCapability.REFACTORING,
            AgentCapability.DOCUMENTATION,
            AgentCapability.TESTING,
            AgentCapability.EXPLANATION,
            AgentCapability.CHAT,
            AgentCapability.TOOL_USE,
            AgentCapability.FILE_OPERATIONS,
            AgentCapability.SHELL_COMMANDS
        )
    }
    
    override fun getDefaultConfig(): AgentConfig {
        return AgentConfig(
            workingDirectory = System.getProperty("user.dir"),
            args = getDefaultArgs()
        )
    }
    
    override fun spawn(config: AgentConfig): Flow<AgentEvent> = flow {
        val sessionId = UUID.randomUUID().toString()
        
        // Emit started event
        emit(AgentEvent.Started(sessionId, name))
        
        try {
            val processBuilder = ProcessBuilder()
                .command(buildCommand(config))
                .directory(java.io.File(config.workingDirectory))
                .redirectErrorStream(true)
            
            // Point Claude Code at local 9Router
            val env = processBuilder.environment()
            AgentRouterEnv.applyTo(env, config.environment)
            if (config.apiKey != null) {
                env["ANTHROPIC_API_KEY"] = config.apiKey
            }

            val process = processBuilder.start()
            
            // Read output
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                line?.let { outputLine ->
                    val event = parseOutput(outputLine)
                    if (event != null) {
                        emit(event)
                    } else {
                        emit(AgentEvent.Output(sessionId, outputLine))
                    }
                }
            }
            
            val exitCode = process.waitFor()
            emit(AgentEvent.Completed(sessionId, exitCode))
            
        } catch (e: Exception) {
            emit(AgentEvent.Error(sessionId, e.message ?: "Unknown error"))
        }
    }.flowOn(Dispatchers.IO)
    
    override fun sendInput(sessionId: String, input: String) {
        // Claude Code doesn't support interactive input in print mode
    }
    
    override fun stop(sessionId: String) {
        // Process will be stopped when the flow collection is cancelled
    }
    
    override fun parseOutput(line: String): AgentEvent? {
        // Try to parse JSON output from Claude Code
        return try {
            val json = org.json.JSONObject(line)
            
            when (json.optString("type")) {
                "assistant" -> {
                    val content = json.optJSONArray("content")
                    if (content != null && content.length() > 0) {
                        val firstBlock = content.getJSONObject(0)
                        when (firstBlock.optString("type")) {
                            "tool_use" -> {
                                val tool = ToolCallRequest(
                                    id = firstBlock.optString("id", UUID.randomUUID().toString()),
                                    name = firstBlock.optString("name", "unknown"),
                                    arguments = firstBlock.optJSONObject("input")?.toMap() ?: emptyMap()
                                )
                                AgentEvent.ToolCall(json.optString("session_id", ""), tool)
                            }
                            "text" -> {
                                AgentEvent.Output(
                                    sessionId = json.optString("session_id", ""),
                                    text = firstBlock.optString("text", "")
                                )
                            }
                            else -> null
                        }
                    } else {
                        null
                    }
                }
                "tool_result" -> {
                    AgentEvent.ToolResult(
                        sessionId = json.optString("session_id", ""),
                        result = com.artier.ide.agent.ToolCallResult(
                            id = json.optString("tool_use_id", ""),
                            toolCallId = json.optString("tool_use_id", ""),
                            output = json.optString("content", ""),
                            isError = json.optBoolean("is_error", false)
                        )
                    )
                }
                "error" -> {
                    AgentEvent.Error(
                        sessionId = json.optString("session_id", ""),
                        error = json.optString("error", "Unknown error")
                    )
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun buildCommand(config: AgentConfig): List<String> {
        val cmd = mutableListOf(command)
        cmd.addAll(config.args)
        
        if (config.model != null) {
            cmd.addAll(listOf("--model", config.model))
        }
        
        return cmd
    }
    
    private fun org.json.JSONObject.toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        this.keys().forEach { key ->
            this.get(key)?.let { value ->
                map[key] = when (value) {
                    is org.json.JSONObject -> value.toMap()
                    is org.json.JSONArray -> value.toList()
                    else -> value
                }
            }
        }
        return map
    }
    
    private fun org.json.JSONArray.toList(): List<Any> {
        val list = mutableListOf<Any>()
        for (i in 0 until this.length()) {
            this.get(i)?.let { value ->
                list.add(when (value) {
                    is org.json.JSONObject -> value.toMap()
                    is org.json.JSONArray -> value.toList()
                    else -> value
                })
            }
        }
        return list
    }
}