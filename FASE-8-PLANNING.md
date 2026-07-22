# Fase 8: Polish & Optimization — Planning Detail

## Goal
Audit memory, lazy-load agresif, minimalkan recomposition, perbaiki performance bottleneck, dan clean up code quality.

## Scope
- Compose recomposition optimization (stable keys, granular state, remember)
- Memory management (CoroutineScope lifecycle, LRU cache, unbounded growth)
- Lazy loading (flatten FileExplorer tree, viewport culling canvas)
- Performance (single JSON library, coroutine-based reconnection, timeout)
- Code quality (deduplicate classes, remove dead code, fix deprecated APIs)

## Issues Found (44 total)

### HIGH Priority (16)
- ResultGrid O(n) indexOf in every row
- FileExplorer LazyColumn defeats laziness
- WebSocketClient raw Thread leak
- WorkspaceCanvas Paint allocation per frame
- WebSocket dual JSON parsing
- Duplicate ChatMessage class
- LspClient/TerminalManager CoroutineScope never cancelled
- FileRepository unbounded memory growth
- RouterManager Thread.sleep blocks
- ToolLoopAgentAdapter shell waitFor no timeout
- AiViewModel excessive list copies

### MEDIUM Priority (22)
- AiAssistantPanel collects full state
- items() without stable key
- SoraEditorWrapper LaunchedEffect content key
- Unmanaged CoroutineScopes
- Blocking HTTP calls
- Duplicated extension functions
- Hardcoded magic strings

### LOW Priority (8)
- error!! force unwrap
- Unused imports
- Deprecated Divider(), statusBarColor

## Validation
1. App compiles tanpa error
2. FileExplorer responsive untuk project besar (1000+ files)
3. Canvas rendering smooth (no Paint allocation per frame)
4. WebSocket reconnect tidak leak thread
5. Tidak ada duplicate class name
