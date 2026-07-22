# Fase 4: Agent Adapter Pertama - Planning Detail

## Goal
Implement agent adapter system dengan adapter pattern untuk OpenCode, Claude Code, dan Hermes Agent, plus AI Assistant panel.

## Duration
3-4 minggu

## Scope
- Base AgentAdapter interface
- OpenCode adapter (prioritas pertama)
- Claude Code adapter
- Hermes Agent adapter
- AI Assistant panel native Compose
- Agent selection dan switching
- Session history management

## Komponen yang Dibangun

### 1. Agent Adapter System
**Fitur:**
- Base interface untuk semua adapter
-统一 event system (tool_call, text_delta, error, result)
- Adapter registry
- Spawn & parse CLI agent output

**Interface:**
```kotlin
interface AgentAdapter {
    fun detect(): Boolean
    fun spawn(config: AgentConfig): AgentSession
    fun parseStream(output: Flow<String>): Flow<AgentEvent>
    fun stop(sessionId: String)
    fun getCapabilities(): List<String>
}
```

### 2. OpenCode Adapter
**Fitur:**
- Spawn OpenCode CLI
- Parse output stream
- Handle tool calls
- Error recovery

### 3. Claude Code Adapter
**Fitur:**
- Spawn Claude Code CLI
- Parse Claude-specific output
- Handle API key configuration
- Streaming responses

### 4. Hermes Agent Adapter
**Fitur:**
- Spawn Hermes Agent CLI
- Parse Hermes output
- Handle different modes
- Multi-turn conversations

### 5. AI Assistant Panel
**Fitur:**
- Chat interface
- Message history
- Code highlighting
- Tool call visualization
- Agent selection dropdown

### 6. Session Management
**Fitur:**
- Multiple sessions
- Session persistence
- History browsing
- Export/Import

## Implementation Steps

### Minggu 1: Core System

**Hari 1-2: Base Adapter**
- AgentAdapter interface
- AgentEvent sealed class
- AgentSession model
- AgentRegistry

**Hari 3-4: OpenCode Adapter**
- OpenCodeAdapter implementation
- Output parser
- Tool call handler

**Hari 5-7: Claude Code Adapter**
- ClaudeCodeAdapter implementation
- API key management
- Streaming support

### Minggu 2: More Adapters & UI

**Hari 1-3: Hermes Adapter**
- HermesAdapter implementation
- Multi-turn support
- Mode handling

**Hari 4-5: AI Assistant Panel**
- Chat UI components
- Message list
- Input area

**Hari 6-7: Agent Selection**
- Agent dropdown
- Status indicators
- Switching logic

### Minggu 3: Session Management & Polish

**Hari 1-2: Session History**
- SQLite storage
- History browser
- Search functionality

**Hari 3-4: Integration**
- Wire up all components
- Error handling
- Loading states

**Hari 5-7: Testing & Polish**
- Unit tests
- Integration tests
- UI polish

## File Structure
```
artier-ide-kotlin-v2/
├── app/
│   └── src/main/java/com/artier/ide/
│       ├── agent/
│       │   ├── AgentAdapter.kt (interface)
│       │   ├── AgentEvent.kt
│       │   ├── AgentSession.kt
│       │   ├── AgentRegistry.kt
│       │   └── adapters/
│       │       ├── OpenCodeAdapter.kt
│       │       ├── ClaudeCodeAdapter.kt
│       │       └── HermesAdapter.kt
│       ├── ui/ai/
│       │   ├── AiAssistantPanel.kt
│       │   ├── ChatMessage.kt
│       │   ├── AgentSelector.kt
│       │   └── AiViewModel.kt
│       └── data/
│           ├── model/
│           │   └── AgentModels.kt
│           └── repository/
│               └── AgentRepository.kt
```

## API Design

### AgentAdapter Interface
```kotlin
interface AgentAdapter {
    val name: String
    val version: String
    
    fun isAvailable(): Boolean
    fun spawn(config: AgentConfig): Flow<AgentEvent>
    fun sendInput(sessionId: String, input: String)
    fun stop(sessionId: String)
    fun getCapabilities(): List<String>
    fun getDefaultConfig(): AgentConfig
}
```

### AgentEvent Sealed Class
```kotlin
sealed class AgentEvent {
    data class Started(val sessionId: String) : AgentEvent()
    data class Output(val sessionId: String, val text: String) : AgentEvent()
    data class ToolCall(val sessionId: String, val tool: ToolCall) : AgentEvent()
    data class ToolResult(val sessionId: String, val result: ToolResult) : AgentEvent()
    data class Error(val sessionId: String, val error: String) : AgentEvent()
    data class Completed(val sessionId: String, val exitCode: Int) : AgentEvent()
}
```

### AgentConfig
```kotlin
data class AgentConfig(
    val workingDirectory: String,
    val environment: Map<String, String> = emptyMap(),
    val args: List<String> = emptyList(),
    val timeout: Long = 300000
)
```

## Validation Criteria
1. ✅ OpenCode bisa di-spawn dan output di-parse
2. ✅ Claude Code bisa di-spawn dengan API key
3. ✅ Hermes Agent bisa di-spawn
4. ✅ AI Assistant panel bisa chat
5. ✅ Agent bisa di-switch tanpa restart
6. ✅ Session history tersimpan

## Risks & Mitigation

### 1. CLI Agent Availability
**Risk:** CLI agents tidak ter-install
**Mitigation:** Check availability, show install instructions

### 2. Output Parsing
**Risk:** Output format berubah
**Mitigation:** Flexible parsing, regex patterns

### 3. Memory Usage
**Risk:** Multiple agents aktif
**Mitigation:** One active agent default, lazy loading

## Dependencies

### Android App
- Kotlin Coroutines
- Flow for streaming
- Hilt for DI

### CLI Agents
- OpenCode CLI
- Claude Code CLI
- Hermes Agent CLI

## Testing Strategy

### Unit Tests
- Adapter detection
- Output parsing
- Event mapping

### Integration Tests
- Spawn agents
- Stream processing
- Session management

### UI Tests
- Chat interactions
- Agent switching
- History browsing

## Success Metrics
1. Agent spawn time < 2 detik
2. Output streaming latency < 100ms
3. UI responsiveness > 60fps
4. Session save/load < 500ms
5. Zero crashes during chat