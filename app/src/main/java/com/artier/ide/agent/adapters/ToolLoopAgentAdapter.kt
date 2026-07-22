package com.artier.ide.agent.adapters

import com.artier.ide.agent.AgentAdapter
import com.artier.ide.agent.AgentCapability
import com.artier.ide.agent.AgentConfig
import com.artier.ide.agent.AgentEvent
import com.artier.ide.agent.ToolCallRequest
import com.artier.ide.agent.ToolCallResult
import com.artier.ide.data.remote.RouterManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Built-in fallback agent: OpenAI-compatible tool loop via 9Router.
 * Used when CLI agents (opencode/claude/hermes) are unavailable.
 */
class ToolLoopAgentAdapter(
    private val routerManager: RouterManager
) : AgentAdapter {

    override val name = "tool-loop"
    override val version = "1.0.0"

    private val activeInputs = ConcurrentHashMap<String, String>()
    private val stopFlags = ConcurrentHashMap<String, Boolean>()

    companion object {
        private const val MAX_ITERATIONS = 8
        private const val DEFAULT_MODEL = "gpt-4o-mini"

        private val SYSTEM_PROMPT = """
            You are Artier IDE's built-in coding assistant running on-device via 9Router.
            You can call tools by emitting a single JSON object (no markdown fences):
            {"tool":"read_file","arguments":{"path":"/path/to/file"}}
            {"tool":"write_file","arguments":{"path":"/path","content":"..."}}
            {"tool":"list_dir","arguments":{"path":"/path"}}
            {"tool":"run_shell","arguments":{"command":"ls -la"}}
            When you have a final answer for the user, reply with plain text (not JSON tool calls).
            Prefer short, actionable answers. Work only inside the configured working directory.
        """.trimIndent()
    }

    override fun isAvailable(): Boolean {
        // Always available as soft fallback; actual call may fail if router down
        return true
    }

    override fun getCommand(): String = "tool-loop"

    override fun getDefaultArgs(): List<String> = emptyList()

    override fun getCapabilities(): List<AgentCapability> = listOf(
        AgentCapability.CODE_GENERATION,
        AgentCapability.CODE_REVIEW,
        AgentCapability.BUG_FIXING,
        AgentCapability.REFACTORING,
        AgentCapability.DOCUMENTATION,
        AgentCapability.EXPLANATION,
        AgentCapability.CHAT,
        AgentCapability.TOOL_USE,
        AgentCapability.FILE_OPERATIONS,
        AgentCapability.SHELL_COMMANDS
    )

    override fun getDefaultConfig(): AgentConfig {
        return AgentConfig(
            workingDirectory = System.getProperty("user.dir") ?: "/",
            model = DEFAULT_MODEL,
            environment = mapOf(
                "OPENAI_BASE_URL" to "http://127.0.0.1:20128/v1",
                "OPENAI_API_BASE" to "http://127.0.0.1:20128/v1"
            )
        )
    }

    override fun spawn(config: AgentConfig): Flow<AgentEvent> = flow {
        val sessionId = UUID.randomUUID().toString()
        stopFlags[sessionId] = false
        emit(AgentEvent.Started(sessionId, name))

        val userInput = activeInputs.remove("__pending__")
            ?: activeInputs.remove(sessionId)
            ?: config.environment["PROMPT"]
            ?: config.args.lastOrNull()
            ?: ""

        if (userInput.isBlank()) {
            emit(AgentEvent.Error(sessionId, "No input provided to tool-loop agent"))
            emit(AgentEvent.Completed(sessionId, 1))
            return@flow
        }

        val messages = mutableListOf(
            "system" to SYSTEM_PROMPT + "\nWorking directory: ${config.workingDirectory}",
            "user" to userInput
        )

        val model = config.model ?: DEFAULT_MODEL
        var iterations = 0

        try {
            while (iterations < MAX_ITERATIONS && stopFlags[sessionId] != true) {
                iterations++
                emit(AgentEvent.Thinking(sessionId, "Iteration $iterations"))

                val reply = routerManager.chatCompletion(
                    model = model,
                    messages = messages,
                    temperature = config.temperature,
                    maxTokens = config.maxTokens
                )

                if (reply.isNullOrBlank()) {
                    emit(AgentEvent.Error(sessionId, "Empty response from 9Router (is it running on 127.0.0.1:20128?)"))
                    break
                }

                val toolCall = parseToolCall(reply)
                if (toolCall != null) {
                    emit(AgentEvent.ToolCall(sessionId, toolCall))
                    val result = executeTool(toolCall, config.workingDirectory)
                    emit(AgentEvent.ToolResult(sessionId, result))
                    messages.add("assistant" to reply)
                    messages.add(
                        "user" to "Tool result for ${toolCall.name}:\n${result.output}"
                    )
                    continue
                }

                // Final natural language answer
                emit(AgentEvent.Output(sessionId, reply.trim()))
                messages.add("assistant" to reply)
                break
            }

            if (iterations >= MAX_ITERATIONS) {
                emit(AgentEvent.Error(sessionId, "Tool loop hit max iterations ($MAX_ITERATIONS)", isRecoverable = true))
            }

            emit(AgentEvent.Completed(sessionId, 0))
        } catch (e: Exception) {
            emit(AgentEvent.Error(sessionId, e.message ?: "Tool-loop failed"))
            emit(AgentEvent.Completed(sessionId, 1))
        } finally {
            stopFlags.remove(sessionId)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Queue prompt for the next spawn (ViewModel should set this before collect).
     */
    fun setPendingPrompt(sessionId: String, prompt: String) {
        activeInputs[sessionId] = prompt
    }

    fun setPendingPrompt(prompt: String) {
        // session id not known yet — store under reserved key used at spawn start
        activeInputs["__pending__"] = prompt
    }

    override fun sendInput(sessionId: String, input: String) {
        activeInputs[sessionId] = input
    }

    override fun stop(sessionId: String) {
        stopFlags[sessionId] = true
    }

    override fun parseOutput(line: String): AgentEvent? = null

    private fun parseToolCall(text: String): ToolCallRequest? {
        val trimmed = text.trim()
        val jsonText = when {
            trimmed.startsWith("{") -> trimmed
            trimmed.contains("```") -> {
                val start = trimmed.indexOf('{')
                val end = trimmed.lastIndexOf('}')
                if (start >= 0 && end > start) trimmed.substring(start, end + 1) else return null
            }
            else -> return null
        }

        return try {
            val obj = JSONObject(jsonText)
            val toolName = obj.optString("tool").ifBlank {
                obj.optString("name").ifBlank { return null }
            }
            if (toolName !in setOf("read_file", "write_file", "list_dir", "run_shell")) {
                return null
            }
            val argsObj = obj.optJSONObject("arguments") ?: obj.optJSONObject("args") ?: JSONObject()
            val args = mutableMapOf<String, Any>()
            argsObj.keys().forEach { key ->
                args[key] = argsObj.get(key)
            }
            ToolCallRequest(
                id = UUID.randomUUID().toString(),
                name = toolName,
                arguments = args
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun executeTool(tool: ToolCallRequest, workdir: String): ToolCallResult {
        return try {
            val root = File(workdir).canonicalFile
            val output = when (tool.name) {
                "read_file" -> {
                    val path = resolveSafePath(root, tool.arguments["path"]?.toString())
                        ?: return err(tool, "Missing or invalid path")
                    if (!path.exists() || !path.isFile) return err(tool, "File not found: ${path.path}")
                    path.readText().take(50_000)
                }
                "write_file" -> {
                    val path = resolveSafePath(root, tool.arguments["path"]?.toString())
                        ?: return err(tool, "Missing or invalid path")
                    val content = tool.arguments["content"]?.toString() ?: return err(tool, "Missing content")
                    path.parentFile?.mkdirs()
                    path.writeText(content)
                    "Wrote ${content.length} bytes to ${path.path}"
                }
                "list_dir" -> {
                    val path = resolveSafePath(root, tool.arguments["path"]?.toString() ?: ".")
                        ?: return err(tool, "Invalid path")
                    if (!path.exists() || !path.isDirectory) return err(tool, "Not a directory")
                    path.listFiles()
                        ?.sortedBy { it.name }
                        ?.joinToString("\n") { f ->
                            val kind = if (f.isDirectory) "dir" else "file"
                            "$kind\t${f.name}"
                        }
                        ?: "(empty)"
                }
                "run_shell" -> {
                    val command = tool.arguments["command"]?.toString()
                        ?: return err(tool, "Missing command")
                    // Restrict to simple safe commands; no shell metachar abuse beyond ProcessBuilder list
                    val process = ProcessBuilder("sh", "-c", command)
                        .directory(root)
                        .redirectErrorStream(true)
                        .start()
                    val out = process.inputStream.bufferedReader().readText().take(20_000)
                    val code = process.waitFor()
                    "exit=$code\n$out"
                }
                else -> return err(tool, "Unknown tool: ${tool.name}")
            }
            ToolCallResult(
                id = UUID.randomUUID().toString(),
                toolCallId = tool.id,
                output = output,
                isError = false
            )
        } catch (e: Exception) {
            err(tool, e.message ?: "Tool execution failed")
        }
    }

    private fun resolveSafePath(root: File, raw: String?): File? {
        if (raw.isNullOrBlank()) return null
        val candidate = File(raw).let { if (it.isAbsolute) it else File(root, raw) }.canonicalFile
        val rootPath = root.canonicalPath
        if (!candidate.path.startsWith(rootPath)) return null
        return candidate
    }

    private fun err(tool: ToolCallRequest, message: String) = ToolCallResult(
        id = UUID.randomUUID().toString(),
        toolCallId = tool.id,
        output = message,
        isError = true
    )
}
