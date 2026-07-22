# Artier IDE — Testing Coverage Map

> Peta antara fitur, source file, dan test file.
> Gunakan file ini untuk memastikan setiap fitur memiliki test yang memadai.

---

## Daftar Isi

1. [Source → Test Mapping](#1-source--test-mapping)
2. [Feature → Test Coverage](#2-feature--test-coverage)
3. [Critical Path Flow](#3-critical-path-flow)
4. [Missing Tests Alert](#4-missing-tests-alert)

---

## 1. Source → Test Mapping

### 1.1 Remote Layer

| Source File | Unit Test | Integration Test | UI Test | Daemon Test |
|---|---|---|---|---|
| `WebSocketClient.kt` | `WebSocketClientTest.kt` | `WebSocketIntegrationTest.kt` | - | - |
| `DaemonApi.kt` | `DaemonApiTest.kt` | `WebSocketIntegrationTest.kt` | - | - |
| `RouterManager.kt` | `RouterManagerTest.kt` | - | - | - |
| `LspClient.kt` | `LspClientTest.kt` | - | - | - |

### 1.2 Repository Layer

| Source File | Unit Test | Integration Test | UI Test |
|---|---|---|---|
| `ChatRepository.kt` | `ChatRepositoryTest.kt` | `AiChatFlowIntegrationTest.kt` | - |
| `DatabaseRepository.kt` | `DatabaseRepositoryTest.kt` | `DatabaseFlowIntegrationTest.kt` | - |
| `EditorRepository.kt` | `EditorRepositoryTest.kt` | - | - |
| `FileRepository.kt` | `FileRepositoryTest.kt` | - | - |
| `SkillRepository.kt` | `SkillRepositoryTest.kt` | `SkillFlowIntegrationTest.kt` | - |

### 1.3 ViewModel Layer

| Source File | Unit Test | UI Test |
|---|---|---|
| `AiViewModel.kt` | `AiViewModelTest.kt` | `AiAssistantPanelTest.kt` |
| `CanvasViewModel.kt` | `CanvasViewModelTest.kt` | `WorkspaceCanvasTest.kt` |
| `DatabaseViewModel.kt` | `DatabaseViewModelTest.kt` | `DatabasePanelTest.kt` |
| `EditorViewModel.kt` | `EditorViewModelTest.kt` | - |
| `FileExplorerViewModel.kt` | `FileExplorerViewModelTest.kt` | `FileExplorerTest.kt` |
| `RouterViewModel.kt` | `RouterViewModelTest.kt` | `RouterPanelTest.kt` |
| `SkillViewModel.kt` | `SkillViewModelTest.kt` | `SkillPanelTest.kt` |
| `TerminalViewModel.kt` | `TerminalViewModelTest.kt` | `TerminalWrapperTest.kt` |
| `TunnelViewModel.kt` | `TunnelViewModelTest.kt` | `TunnelPanelTest.kt` |

### 1.4 Model Layer

| Source File | Unit Test |
|---|---|
| `AiModels.kt` | `AiModelsTest.kt` |
| `CanvasModels.kt` | `CanvasModelsTest.kt` |
| `DatabaseModels.kt` | `DatabaseModelsTest.kt` |
| `EditorTab.kt` | `EditorTabTest.kt` |
| `FileNode.kt` | `FileNodeTest.kt` |
| `RouterConfig.kt` | `RouterConfigTest.kt` |
| `SkillModels.kt` | `SkillModelsTest.kt` |
| `TerminalSession.kt` | `TerminalSessionTest.kt` |
| `TunnelSession.kt` | `TunnelSessionTest.kt` |

### 1.5 UI Layer

| Source File | Compose UI Test |
|---|---|
| `AiAssistantPanel.kt` | `AiAssistantPanelTest.kt` |
| `WorkspaceCanvas.kt` | `WorkspaceCanvasTest.kt` |
| `DatabasePanel.kt` | `DatabasePanelTest.kt` |
| `EditorTabManager.kt` | `EditorTabManagerTest.kt` |
| `SoraEditorWrapper.kt` | - (needs SoraEditor) |
| `FileExplorer.kt` | `FileExplorerTest.kt` |
| `FileItem.kt` | `FileItemTest.kt` |
| `RouterPanel.kt` | `RouterPanelTest.kt` |
| `WebViewDashboard.kt` | `WebViewDashboardTest.kt` |
| `SkillPanel.kt` | `SkillPanelTest.kt` |
| `TerminalWrapper.kt` | `TerminalWrapperTest.kt` |
| `TunnelPanel.kt` | `TunnelPanelTest.kt` |
| `WorkspaceScreen.kt` | `WorkspaceScreenTest.kt` |

### 1.6 Agent Layer

| Source File | Unit Test |
|---|---|
| `AgentAdapter.kt` | - (interface) |
| `AgentRouterEnv.kt` | `AgentRouterEnvTest.kt` |
| `ClaudeCodeAdapter.kt` | `ClaudeCodeAdapterTest.kt` |
| `HermesAdapter.kt` | `HermesAdapterTest.kt` |
| `OpenCodeAdapter.kt` | `OpenCodeAdapterTest.kt` |
| `ToolLoopAgentAdapter.kt` | `ToolLoopAgentAdapterTest.kt` |

### 1.7 Daemon Layer (TypeScript)

| Source File | Jest Test |
|---|---|
| `server.ts` | `server.test.ts` |
| `types.ts` | - (types only) |
| `database/db-client.ts` | `db-client.test.ts` |
| `database/db-manager.ts` | `db-manager.test.ts` |
| `lsp/lsp-server-manager.ts` | `lsp-server-manager.test.ts` |
| `pkg/pkg-manager.ts` | `pkg-manager.test.ts` |
| `pty/pty-manager.ts` | `pty-manager.test.ts` |
| `skills/skill-manager.ts` | `skill-manager.test.ts` |
| `sse/sse-manager.ts` | `sse-manager.test.ts` |
| `tunnel/cloudflared-manager.ts` | `cloudflared-manager.test.ts` |
| `tunnel/ssh-tunnel-manager.ts` | `ssh-tunnel-manager.test.ts` |

---

## 2. Feature → Test Coverage

### 2.1 Terminal Feature

```
User Action              → Source File                    → Test File
─────────────────────────────────────────────────────────────────────
Open terminal            → TerminalViewModel.kt           → TerminalViewModelTest.kt
                         → TerminalWrapper.kt             → TerminalWrapperTest.kt
Type command             → TerminalViewModel.kt           → TerminalViewModelTest.kt
                         → WebSocketClient.kt             → WebSocketClientTest.kt
Receive output           → TerminalViewModel.kt           → TerminalViewModelTest.kt
                         → WebSocketClient.kt             → WebSocketClientTest.kt
Create session           → DaemonApi.kt                   → DaemonApiTest.kt
                         → daemon/pty/pty-manager.ts      → pty-manager.test.ts
Destroy session          → DaemonApi.kt                   → DaemonApiTest.kt
                         → daemon/pty/pty-manager.ts      → pty-manager.test.ts
```

**Coverage Gaps:**
- [ ] Terminal output rendering performance
- [ ] PTY resize handling
- [ ] Multiple concurrent sessions
- [ ] Terminal history persistence

### 2.2 Database Feature

```
User Action              → Source File                    → Test File
─────────────────────────────────────────────────────────────────────
Open DB panel            → DatabaseViewModel.kt           → DatabaseViewModelTest.kt
                         → DatabasePanel.kt               → DatabasePanelTest.kt
Connect to DB            → DatabaseViewModel.kt           → DatabaseViewModelTest.kt
                         → DatabaseRepository.kt          → DatabaseRepositoryTest.kt
                         → daemon/db/db-manager.ts        → db-manager.test.ts
Run query                → DatabaseViewModel.kt           → DatabaseViewModelTest.kt
                         → DatabaseRepository.kt          → DatabaseRepositoryTest.kt
                         → daemon/db/db-client.ts         → db-client.test.ts
View tables              → DatabaseViewModel.kt           → DatabaseViewModelTest.kt
                         → DatabaseRepository.kt          → DatabaseRepositoryTest.kt
View schema              → DatabaseViewModel.kt           → DatabaseViewModelTest.kt
                         → DatabaseRepository.kt          → DatabaseRepositoryTest.kt
Disconnect               → DatabaseViewModel.kt           → DatabaseViewModelTest.kt
                         → DatabaseRepository.kt          → DatabaseRepositoryTest.kt
```

**Coverage Gaps:**
- [ ] Query result pagination
- [ ] Large result set rendering
- [ ] Connection pool management
- [ ] Multiple database types (PostgreSQL, SQLite, LibSQL)

### 2.3 AI Chat Feature

```
User Action              → Source File                    → Test File
─────────────────────────────────────────────────────────────────────
Send message             → AiViewModel.kt                 → AiViewModelTest.kt
                         → ChatRepository.kt              → ChatRepositoryTest.kt
                         → AiAssistantPanel.kt            → AiAssistantPanelTest.kt
Receive response         → AiViewModel.kt                 → AiViewModelTest.kt
                         → ChatRepository.kt              → ChatRepositoryTest.kt
Tool call                → ToolLoopAgentAdapter.kt        → ToolLoopAgentAdapterTest.kt
                         → DaemonApi.kt                   → DaemonApiTest.kt
Agent status             → AiViewModel.kt                 → AiViewModelTest.kt
                         → AiModels.kt                    → AiModelsTest.kt
Session management       → AiViewModel.kt                 → AiViewModelTest.kt
                         → ChatRepository.kt              → ChatRepositoryTest.kt
```

**Coverage Gaps:**
- [ ] Streaming response handling
- [ ] Tool call result parsing
- [ ] Multi-turn conversation context
- [ ] Agent switching (Claude, Hermes, OpenCode)

### 2.4 File Explorer Feature

```
User Action              → Source File                    → Test File
─────────────────────────────────────────────────────────────────────
Load file tree           → FileExplorerViewModel.kt       → FileExplorerViewModelTest.kt
                         → FileRepository.kt              → FileRepositoryTest.kt
Expand folder            → FileExplorerViewModel.kt       → FileExplorerViewModelTest.kt
                         → FileExplorer.kt                → FileExplorerTest.kt
Select file              → FileExplorerViewModel.kt       → FileExplorerViewModelTest.kt
                         → FileExplorer.kt                → FileExplorerTest.kt
Open file in editor      → EditorViewModel.kt             → EditorViewModelTest.kt
                         → EditorRepository.kt            → EditorRepositoryTest.kt
Create file              → FileExplorerViewModel.kt       → FileExplorerViewModelTest.kt
                         → FileRepository.kt              → FileRepositoryTest.kt
Delete file              → FileExplorerViewModel.kt       → FileExplorerViewModelTest.kt
                         → FileRepository.kt              → FileRepositoryTest.kt
```

**Coverage Gaps:**
- [ ] File search/filter
- [ ] File rename
- [ ] Drag and drop
- [ ] File icons by type

### 2.5 Tunnel Feature

```
User Action              → Source File                    → Test File
─────────────────────────────────────────────────────────────────────
Start tunnel             → TunnelViewModel.kt             → TunnelViewModelTest.kt
                         → TunnelPanel.kt                 → TunnelPanelTest.kt
                         → daemon/tunnel/cloudflared-manager.ts → cloudflared-manager.test.ts
Stop tunnel              → TunnelViewModel.kt             → TunnelViewModelTest.kt
                         → TunnelPanel.kt                 → TunnelPanelTest.kt
Copy URL                 → TunnelPanel.kt                 → TunnelPanelTest.kt
View status              → TunnelViewModel.kt             → TunnelViewModelTest.kt
                         → TunnelSession.kt               → TunnelSessionTest.kt
```

**Coverage Gaps:**
- [ ] SSH tunnel flow
- [ ] Tunnel reconnection
- [ ] Multiple tunnel sessions
- [ ] Tunnel error recovery

### 2.6 Router Feature

```
User Action              → Source File                    → Test File
─────────────────────────────────────────────────────────────────────
Load config              → RouterViewModel.kt             → RouterViewModelTest.kt
                         → RouterManager.kt               → RouterManagerTest.kt
Save config              → RouterViewModel.kt             → RouterViewModelTest.kt
                         → RouterManager.kt               → RouterManagerTest.kt
Toggle provider          → RouterViewModel.kt             → RouterViewModelTest.kt
View dashboard           → WebViewDashboard.kt            → WebViewDashboardTest.kt
Check status             → RouterViewModel.kt             → RouterViewModelTest.kt
```

**Coverage Gaps:**
- [ ] Provider health check
- [ ] Load balancing
- [ ] Fallback routing
- [ ] Rate limiting

### 2.7 Skill Feature

```
User Action              → Source File                    → Test File
─────────────────────────────────────────────────────────────────────
Load skill list          → SkillViewModel.kt              → SkillViewModelTest.kt
                         → SkillRepository.kt             → SkillRepositoryTest.kt
Search skills            → SkillViewModel.kt              → SkillViewModelTest.kt
                         → SkillPanel.kt                  → SkillPanelTest.kt
View skill detail        → SkillViewModel.kt              → SkillViewModelTest.kt
                         → SkillRepository.kt             → SkillRepositoryTest.kt
                         → SkillPanel.kt                  → SkillPanelTest.kt
Load bundled skills      → daemon/skills/skill-manager.ts → skill-manager.test.ts
```

**Coverage Gaps:**
- [ ] Custom skill loading
- [ ] Skill execution
- [ ] Skill dependency resolution

### 2.8 Canvas Feature

```
User Action              → Source File                    → Test File
─────────────────────────────────────────────────────────────────────
Load workspace           → CanvasViewModel.kt             → CanvasViewModelTest.kt
                         → WorkspaceCanvas.kt             → WorkspaceCanvasTest.kt
Pan canvas               → CanvasViewModel.kt             → CanvasViewModelTest.kt
                         → WorkspaceCanvas.kt             → WorkspaceCanvasTest.kt
Zoom canvas              → CanvasViewModel.kt             → CanvasViewModelTest.kt
                         → WorkspaceCanvas.kt             → WorkspaceCanvasTest.kt
Select node              → CanvasViewModel.kt             → CanvasViewModelTest.kt
                         → WorkspaceCanvas.kt             → WorkspaceCanvasTest.kt
Expand/collapse          → CanvasViewModel.kt             → CanvasViewModelTest.kt
                         → CanvasModels.kt                → CanvasModelsTest.kt
```

**Coverage Gaps:**
- [ ] Node drag and drop
- [ ] Edge connections
- [ ] Auto-layout algorithm
- [ ] Canvas export (PNG)

---

## 3. Critical Path Flow

### 3.1 App Launch Flow

```
┌─────────────┐
│ MainActivity │
└──────┬──────┘
       │
       ▼
┌─────────────┐     ┌──────────────┐
│ SystemInit  │────▶│ DaemonManager│
└──────┬──────┘     └──────┬───────┘
       │                   │
       ▼                   ▼
┌─────────────┐     ┌──────────────┐
│ ProotManager│     │ Daemon Start │
└──────┬──────┘     └──────┬───────┘
       │                   │
       ▼                   ▼
┌─────────────┐     ┌──────────────┐
│ Workspace   │◀───│ WS Connect   │
│ Screen      │     │              │
└─────────────┘     └──────────────┘

Test Files Required:
- SystemInitializerTest.kt
- DaemonManagerTest.kt
- ProotManagerTest.kt
- WebSocketClientTest.kt (connection)
- WorkspaceScreenTest.kt (render)
```

### 3.2 Database Query Flow

```
┌─────────────┐
│ DatabasePanel│
└──────┬──────┘
       │ User clicks "Connect"
       ▼
┌─────────────┐     ┌──────────────┐
│ DatabaseVM  │────▶│ DB Repository│
└──────┬──────┘     └──────┬───────┘
       │                   │
       ▼                   ▼
┌─────────────┐     ┌──────────────┐
│ WS: db_     │◀───│ Daemon DB    │
│ connect     │     │ Manager      │
└──────┬──────┘     └──────────────┘
       │
       ▼
┌─────────────┐     ┌──────────────┐
│ DB: db_     │────▶│ Update UI    │
│ connected   │     │ State        │
└─────────────┘     └──────────────┘

Test Files Required:
- DatabasePanelTest.kt (UI interaction)
- DatabaseViewModelTest.kt (state updates)
- DatabaseRepositoryTest.kt (WS messages)
- db-manager.test.ts (daemon handling)
- DatabaseFlowIntegrationTest.kt (end-to-end)
```

### 3.3 AI Chat Flow

```
┌─────────────┐
│ AiAssistant │
│ Panel       │
└──────┬──────┘
       │ User sends message
       ▼
┌─────────────┐     ┌──────────────┐
│ AiViewModel │────▶│ ChatRepo     │
└──────┬──────┘     └──────┬───────┘
       │                   │
       ▼                   ▼
┌─────────────┐     ┌──────────────┐
│ WS: agent_  │◀───│ Daemon Agent │
│ input       │     │ Handler      │
└──────┬──────┘     └──────────────┘
       │
       ▼
┌─────────────┐     ┌──────────────┐
│ WS: agent_  │────▶│ Tool Loop    │
│ output      │     │ Adapter      │
└──────┬──────┘     └──────────────┘
       │
       ▼
┌─────────────┐
│ Update UI   │
│ Messages    │
└─────────────┘

Test Files Required:
- AiAssistantPanelTest.kt (UI)
- AiViewModelTest.kt (state)
- ChatRepositoryTest.kt (WS)
- ToolLoopAgentAdapterTest.kt (tool calls)
- AiChatFlowIntegrationTest.kt (end-to-end)
```

---

## 4. Missing Tests Alert

### 4.1 Critical Missing Tests

| Feature | Missing Test | Priority | Risk |
|---------|-------------|----------|------|
| Terminal | PTY resize handling | P0 | Terminal crashes on resize |
| Database | Query result pagination | P1 | Memory issues with large results |
| AI Chat | Streaming response | P0 | Response truncated |
| File Explorer | File search | P1 | Poor UX for large projects |
| Tunnel | SSH tunnel flow | P1 | Feature incomplete |
| Router | Provider health check | P1 | Stale provider used |
| Canvas | Node drag and drop | P1 | Poor UX |

### 4.2 Performance Missing Tests

| Feature | Missing Test | Priority | Risk |
|---------|-------------|----------|------|
| All | Memory leak detection | P0 | App crashes over time |
| File Explorer | 1000+ file scroll | P0 | UI freezes |
| Database | 10000+ row result | P1 | OOM |
| Canvas | 100+ node render | P1 | UI freezes |
| WebSocket | 1000 msg/sec load | P1 | Message loss |

### 4.3 Security Missing Tests

| Feature | Missing Test | Priority | Risk |
|---------|-------------|----------|------|
| Database | SQL injection | P0 | Data breach |
| WebView | XSS prevention | P0 | Code execution |
| File System | Path traversal | P0 | File access |
| Terminal | Command injection | P0 | System compromise |

---

## 5. Test Execution Order

### 5.1 Smoke Tests (Run First)

```
1. App startup test
2. WebSocket connection test
3. Daemon startup test
4. Basic UI render test (all panels)
```

### 5.2 Unit Tests (Run Second)

```
1. Model serialization tests
2. ViewModel state tests
3. Repository logic tests
4. Adapter parsing tests
```

### 5.3 Integration Tests (Run Third)

```
1. WebSocket message flow tests
2. Database connect/query flow
3. AI chat send/receive flow
4. File operation flow
```

### 5.4 UI Tests (Run Fourth)

```
1. Panel rendering tests
2. User interaction tests
3. Navigation tests
```

### 5.5 Performance Tests (Run Last)

```
1. Startup benchmark
2. Scroll performance
3. Memory leak detection
4. WebSocket throughput
```

---

## 6. Coverage Report Template

```markdown
## Test Coverage Report - [Date]

### Summary
- Total Tests: [X]
- Passed: [X]
- Failed: [X]
- Skipped: [X]
- Coverage: [X]%

### By Layer
| Layer | Tests | Passed | Coverage |
|-------|-------|--------|----------|
| Unit | X | X | X% |
| Integration | X | X | X% |
| UI | X | X | X% |
| Daemon | X | X | X% |
| Performance | X | X | X% |

### By Feature
| Feature | Tests | Passed | Coverage |
|---------|-------|--------|----------|
| Terminal | X | X | X% |
| Database | X | X | X% |
| AI Chat | X | X | X% |
| File Explorer | X | X | X% |
| Tunnel | X | X | X% |
| Router | X | X | X% |
| Skills | X | X | X% |
| Canvas | X | X | X% |

### Failed Tests
1. [Test Name] - [Reason]
2. [Test Name] - [Reason]

### Action Items
- [ ] Fix failed tests
- [ ] Add missing tests
- [ ] Improve coverage for [Feature]
```

---

*Last updated: Juli 2026*
*Project: Artier IDE Kotlin Native v2*
