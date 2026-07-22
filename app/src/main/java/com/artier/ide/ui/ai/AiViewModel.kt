package com.artier.ide.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artier.ide.agent.AgentConfig
import com.artier.ide.agent.AgentEvent
import com.artier.ide.agent.AgentRegistry
import com.artier.ide.agent.AgentRouterEnv
import com.artier.ide.agent.adapters.ClaudeCodeAdapter
import com.artier.ide.agent.adapters.HermesAdapter
import com.artier.ide.agent.adapters.OpenCodeAdapter
import com.artier.ide.agent.adapters.ToolLoopAgentAdapter
import com.artier.ide.data.model.AiState
import com.artier.ide.data.model.ChatMessage
import com.artier.ide.data.model.ChatSession
import com.artier.ide.data.model.MessageRole
import com.artier.ide.data.remote.RouterManager
import com.artier.ide.data.repository.ChatRepository
import com.artier.ide.data.repository.SkillRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AiViewModel @Inject constructor(
    private val routerManager: RouterManager,
    private val chatRepository: ChatRepository,
    private val skillRepository: SkillRepository
) : ViewModel() {

    private val agentRegistry = AgentRegistry()
    private val toolLoopAdapter = ToolLoopAgentAdapter(routerManager)

    private val _state = MutableStateFlow(AiState())
    val state: StateFlow<AiState> = _state.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private var processingJob: Job? = null

    init {
        registerAdapters()
        updateAvailableAgents()
        chatRepository.loadSessions()
        skillRepository.scan()
        skillRepository.refreshAgentContext()

        viewModelScope.launch {
            chatRepository.sessions.collect { sessions ->
                if (sessions.isNotEmpty()) {
                    _state.value = _state.value.copy(
                        sessions = mergeSessions(_state.value.sessions, sessions),
                        activeSessionId = _state.value.activeSessionId
                            ?: sessions.firstOrNull()?.id
                    )
                }
            }
        }
    }

    private fun skillContextForAgent(): String {
        val fromDaemon = skillRepository.agentContext.value
        if (fromDaemon.isNotBlank()) return fromDaemon
        return skillRepository.getEnabledContextLocal()
    }

    private fun registerAdapters() {
        agentRegistry.register(OpenCodeAdapter())
        agentRegistry.register(ClaudeCodeAdapter())
        agentRegistry.register(HermesAdapter())
        agentRegistry.register(toolLoopAdapter)
    }

    private fun updateAvailableAgents() {
        val agents = agentRegistry.getAllAdapters().map { adapter ->
            com.artier.ide.data.model.AgentStatus(
                name = adapter.name,
                isAvailable = adapter.isAvailable()
            )
        }

        _state.value = _state.value.copy(availableAgents = agents)

        // Prefer first truly available CLI; else tool-loop fallback
        val preferred = agents.firstOrNull { it.isAvailable && it.name != "tool-loop" }
            ?: agents.firstOrNull { it.name == "tool-loop" }
        if (preferred != null && _state.value.selectedAgent == null) {
            selectAgent(preferred.name)
        }
    }

    fun selectAgent(agentName: String) {
        _state.value = _state.value.copy(selectedAgent = agentName)
    }

    fun createNewSession() {
        val agentName = _state.value.selectedAgent ?: "tool-loop"

        val session = ChatSession(
            title = "New Chat",
            agentName = agentName
        )

        chatRepository.createSession(session)

        _state.value = _state.value.copy(
            sessions = listOf(session) + _state.value.sessions.filter { it.id != session.id },
            activeSessionId = session.id
        )
    }

    fun setActiveSession(sessionId: String) {
        _state.value = _state.value.copy(activeSessionId = sessionId)
        chatRepository.loadMessages(sessionId)
    }

    fun deleteSession(sessionId: String) {
        chatRepository.deleteSession(sessionId)
        _state.value = _state.value.copy(
            sessions = _state.value.sessions.filter { it.id != sessionId },
            activeSessionId = if (_state.value.activeSessionId == sessionId) {
                _state.value.sessions.firstOrNull { it.id != sessionId }?.id
            } else {
                _state.value.activeSessionId
            }
        )
    }

    fun updateInputText(text: String) {
        _inputText.value = text
    }

    fun sendMessage() {
        val text = _inputText.value.trim()
        if (text.isEmpty()) return

        val sessionId = _state.value.activeSessionId ?: createNewSessionAndReturnId()
        var agentName = _state.value.selectedAgent ?: "tool-loop"

        // Auto-fallback to tool-loop if selected CLI is missing
        val adapter = resolveAdapter(agentName)
        if (adapter.name != agentName) {
            agentName = adapter.name
            _state.value = _state.value.copy(selectedAgent = agentName)
        }

        val userMessage = ChatMessage(
            role = MessageRole.USER,
            content = text,
            agentName = agentName
        )

        addMessageToSession(sessionId, userMessage)
        chatRepository.addMessage(sessionId, userMessage)

        _inputText.value = ""
        processWithAgent(sessionId, agentName, text)
    }

    private fun resolveAdapter(agentName: String): com.artier.ide.agent.AgentAdapter {
        val preferred = agentRegistry.getAdapter(agentName)
        if (preferred != null && preferred.isAvailable() && agentName != "tool-loop") {
            return preferred
        }
        // Explicit tool-loop or CLI unavailable → fallback
        return toolLoopAdapter
    }

    private fun createNewSessionAndReturnId(): String {
        createNewSession()
        return _state.value.activeSessionId ?: UUID.randomUUID().toString()
    }

    private fun addMessageToSession(sessionId: String, message: ChatMessage) {
        _state.value = _state.value.copy(
            sessions = _state.value.sessions.map { session ->
                if (session.id == sessionId) {
                    session.copy(
                        messages = session.messages + message,
                        updatedAt = System.currentTimeMillis()
                    )
                } else {
                    session
                }
            }
        )
    }

    private fun processWithAgent(sessionId: String, agentName: String, input: String) {
        val adapter = resolveAdapter(agentName)

        processingJob?.cancel()
        processingJob = viewModelScope.launch {
            _state.value = _state.value.copy(isProcessing = true)

            try {
                val skillCtx = skillContextForAgent()
                val enrichedInput = if (skillCtx.isNotBlank()) {
                    "$skillCtx\n\n---\n\nUser request:\n$input"
                } else {
                    input
                }

                val baseConfig = adapter.getDefaultConfig()
                val config = baseConfig.copy(
                    environment = AgentRouterEnv.forProcess(
                        baseConfig.environment + mapOf("PROMPT" to enrichedInput)
                    ),
                    args = if (adapter.name == "tool-loop") {
                        listOf(enrichedInput)
                    } else {
                        baseConfig.args + enrichedInput
                    }
                )

                if (adapter is ToolLoopAgentAdapter) {
                    adapter.setPendingPrompt(enrichedInput)
                }

                adapter.spawn(config).collect { event ->
                    when (event) {
                        is AgentEvent.Started -> Unit
                        is AgentEvent.Output -> {
                            val assistantMessage = ChatMessage(
                                role = MessageRole.ASSISTANT,
                                content = event.text,
                                agentName = adapter.name
                            )
                            addMessageToSession(sessionId, assistantMessage)
                            chatRepository.addMessage(sessionId, assistantMessage)
                        }
                        is AgentEvent.Thinking -> Unit
                        is AgentEvent.ToolCall -> {
                            val toolMessage = ChatMessage(
                                role = MessageRole.TOOL,
                                content = "Calling tool: ${event.tool.name}",
                                agentName = adapter.name
                            )
                            addMessageToSession(sessionId, toolMessage)
                            chatRepository.addMessage(sessionId, toolMessage)
                        }
                        is AgentEvent.ToolResult -> {
                            val resultMessage = ChatMessage(
                                role = MessageRole.TOOL,
                                content = event.result.output.take(2000),
                                agentName = adapter.name,
                                isError = event.result.isError
                            )
                            addMessageToSession(sessionId, resultMessage)
                        }
                        is AgentEvent.Error -> {
                            // On CLI error, try tool-loop once if we weren't already on it
                            if (adapter.name != "tool-loop" && event.isRecoverable) {
                                val fallbackMsg = ChatMessage(
                                    role = MessageRole.ASSISTANT,
                                    content = "CLI agent failed (${event.error}). Falling back to tool-loop via 9Router…",
                                    agentName = adapter.name,
                                    isError = true
                                )
                                addMessageToSession(sessionId, fallbackMsg)
                                runToolLoopFallback(sessionId, input)
                            } else {
                                val errorMessage = ChatMessage(
                                    role = MessageRole.ASSISTANT,
                                    content = "Error: ${event.error}",
                                    agentName = adapter.name,
                                    isError = true
                                )
                                addMessageToSession(sessionId, errorMessage)
                                chatRepository.addMessage(sessionId, errorMessage)
                            }
                        }
                        is AgentEvent.Completed -> Unit
                        else -> Unit
                    }
                }
            } catch (e: Exception) {
                // Hard failure → tool-loop fallback
                if (agentName != "tool-loop") {
                    runToolLoopFallback(sessionId, input)
                } else {
                    val errorMessage = ChatMessage(
                        role = MessageRole.ASSISTANT,
                        content = "Error: ${e.message}",
                        agentName = agentName,
                        isError = true
                    )
                    addMessageToSession(sessionId, errorMessage)
                    chatRepository.addMessage(sessionId, errorMessage)
                }
            } finally {
                _state.value = _state.value.copy(isProcessing = false)
            }
        }
    }

    private suspend fun runToolLoopFallback(sessionId: String, input: String) {
        val skillCtx = skillContextForAgent()
        val enrichedInput = if (skillCtx.isNotBlank()) {
            "$skillCtx\n\n---\n\nUser request:\n$input"
        } else {
            input
        }
        toolLoopAdapter.setPendingPrompt(enrichedInput)
        val config = toolLoopAdapter.getDefaultConfig().copy(
            environment = AgentRouterEnv.forProcess(mapOf("PROMPT" to enrichedInput)),
            args = listOf(enrichedInput)
        )
        toolLoopAdapter.spawn(config).collect { event ->
            when (event) {
                is AgentEvent.Output -> {
                    val msg = ChatMessage(
                        role = MessageRole.ASSISTANT,
                        content = event.text,
                        agentName = "tool-loop"
                    )
                    addMessageToSession(sessionId, msg)
                    chatRepository.addMessage(sessionId, msg)
                }
                is AgentEvent.ToolCall -> {
                    addMessageToSession(
                        sessionId,
                        ChatMessage(
                            role = MessageRole.TOOL,
                            content = "Calling tool: ${event.tool.name}",
                            agentName = "tool-loop"
                        )
                    )
                }
                is AgentEvent.Error -> {
                    val msg = ChatMessage(
                        role = MessageRole.ASSISTANT,
                        content = "Error: ${event.error}",
                        agentName = "tool-loop",
                        isError = true
                    )
                    addMessageToSession(sessionId, msg)
                    chatRepository.addMessage(sessionId, msg)
                }
                else -> Unit
            }
        }
    }

    fun stopProcessing() {
        processingJob?.cancel()
        _state.value = _state.value.copy(isProcessing = false)
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    private fun mergeSessions(
        local: List<ChatSession>,
        remote: List<ChatSession>
    ): List<ChatSession> {
        val byId = LinkedHashMap<String, ChatSession>()
        remote.forEach { byId[it.id] = it }
        local.forEach { localSession ->
            val existing = byId[localSession.id]
            if (existing == null) {
                byId[localSession.id] = localSession
            } else if (localSession.messages.size > existing.messages.size) {
                byId[localSession.id] = localSession
            }
        }
        return byId.values.sortedByDescending { it.updatedAt }
    }
}
