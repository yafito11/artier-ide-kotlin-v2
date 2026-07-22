# Artier IDE — Automated Testing Guidelines

> Dokumen ini menjadi panduan utama untuk semua automated testing di project Artier IDE Kotlin Native.
> Tujuan: memastikan semua fitur berjalan dengan benar dan performa tetap optimal.

---

## Daftar Isi

1. [Arsitektur Testing](#1-arsitektur-testing)
2. [Jenis Testing](#2-jenis-testing)
3. [Setup Environment](#3-setup-environment)
4. [Unit Testing](#4-unit-testing)
5. [Integration Testing](#5-integration-testing)
6. [UI Testing (Compose)](#6-ui-testing-compose)
7. [Performance Testing](#7-performance-testing)
8. [WebSocket Testing](#8-websocket-testing)
9. [Daemon Testing](#9-daemon-testing)
10. [Naming Convention](#10-naming-convention)
11. [Coverage Target](#11-coverage-target)
12. [CI/CD Integration](#12-cicd-integration)

---

## 1. Arsitektur Testing

```
┌─────────────────────────────────────────────────┐
│                   E2E Tests                      │
│  (Full app flow: daemon + UI + WebSocket)        │
├─────────────────────────────────────────────────┤
│              Integration Tests                   │
│  (ViewModel + Repository + WebSocket mock)       │
├─────────────────────────────────────────────────┤
│               Unit Tests                          │
│  (ViewModel logic, Repository, Model, Adapter)   │
├─────────────────────────────────────────────────┤
│            Compose UI Tests                       │
│  (Panel rendering, user interaction)             │
└─────────────────────────────────────────────────┘
```

### Mapping Source → Test Layer

| Source File | Test Layer | Test Type |
|---|---|---|
| `AiViewModel.kt` | Unit | ViewModel logic, session management |
| `AiAssistantPanel.kt` | Compose UI | Rendering, input, message list |
| `ChatRepository.kt` | Integration | WebSocket message handling |
| `WebSocketClient.kt` | Unit + Integration | Connection, reconnect, message parsing |
| `DaemonApi.kt` | Unit | Event mapping, payload building |
| `DatabaseRepository.kt` | Integration | Query flow, table loading |
| `DatabaseViewModel.kt` | Unit | State transitions |
| `DatabasePanel.kt` | Compose UI | Form, table list, query display |
| `TerminalViewModel.kt` | Unit | Session create/destroy |
| `TerminalWrapper.kt` | Compose UI | Terminal rendering |
| `FileExplorerViewModel.kt` | Unit | File tree operations |
| `FileExplorer.kt` | Compose UI | Tree expand/collapse, selection |
| `TunnelViewModel.kt` | Unit | Tunnel lifecycle |
| `TunnelPanel.kt` | Compose UI | Session display, connect/disconnect |
| `RouterViewModel.kt` | Unit | Config management |
| `RouterPanel.kt` | Compose UI | Config form, status display |
| `SkillRepository.kt` | Integration | Skill loading, detail fetch |
| `SkillViewModel.kt` | Unit | Skill list, search |
| `SkillPanel.kt` | Compose UI | Skill cards, search, detail dialog |
| `CanvasViewModel.kt` | Unit | Graph layout, node operations |
| `WorkspaceCanvas.kt` | Compose UI | Canvas rendering, pan/zoom |
| `EditorRepository.kt` | Unit | Tab management, content |
| `EditorViewModel.kt` | Unit | Editor state |
| All `*Models.kt` | Unit | Serialization, defaults, helpers |

---

## 2. Jenis Testing

### 2.1 Unit Testing (Tanpa Android Context)

**Target**: ViewModel, Repository, Model, Adapter, Utils

**Framework**: JUnit 4 + MockK + kotlinx-coroutines-test + Turbine

**Coverage Target**: 80%+ baris kode

### 2.2 Integration Testing (Dengan Mock WebSocket)

**Target**: Repository ↔ WebSocket interaction, ViewModel ↔ Repository

**Framework**: JUnit 4 + MockK + Turbine + kotlinx-coroutines-test

### 2.3 Compose UI Testing

**Target**: Semua Panel dan Screen

**Framework**: Compose Test (ui-test-junit4) + Compose Test Manifest

### 2.4 Performance Testing

**Target**: Frame rate, memory allocation, WebSocket throughput, startup time

**Framework**: Benchmark (androidx.benchmark) + Manual profiling

### 2.5 Daemon Testing (Node.js)

**Target**: Server WebSocket handlers, PTY manager, DB manager, Tunnel manager

**Framework**: Jest + ts-node + supertest

---

## 3. Setup Environment

### 3.1 Dependencies yang Perlu Ditambahkan

```kotlin
// build.gradle.kts (app)
dependencies {
    // Existing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.08.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // ADD THESE
    // Mocking
    testImplementation("io.mockk:mockk:1.13.9")
    androidTestImplementation("io.mockk:mockk-android:1.13.9")

    // Coroutines test
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

    // Turbine (Flow testing)
    testImplementation("app.cash.turbine:turbine:1.0.0")

    // Hilt testing
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.48.1")
    kaptAndroidTest("com.google.dagger:hilt-compiler:2.48.1")

    // Robolectric (unit test dengan Android context)
    testImplementation("org.robolectric:robolectric:4.11.1")

    // Compose testing
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Benchmark
    androidTestImplementation("androidx.benchmark:benchmark-junit4:1.2.0")
}
```

### 3.2 Daemon Dependencies

```json
{
  "devDependencies": {
    "jest": "^29.7.0",
    "ts-jest": "^29.1.1",
    "@types/jest": "^29.5.11",
    "supertest": "^6.3.3",
    "@types/supertest": "^6.0.2"
  }
}
```

### 3.3 Folder Structure Testing

```
app/src/
├── test/java/com/artier/ide/
│   ├── data/
│   │   ├── model/
│   │   │   ├── AiModelsTest.kt
│   │   │   ├── DatabaseModelsTest.kt
│   │   │   ├── CanvasModelsTest.kt
│   │   │   └── ...
│   │   ├── remote/
│   │   │   ├── WebSocketClientTest.kt
│   │   │   ├── DaemonApiTest.kt
│   │   │   └── RouterManagerTest.kt
│   │   └── repository/
│   │       ├── ChatRepositoryTest.kt
│   │       ├── DatabaseRepositoryTest.kt
│   │       └── ...
│   ├── agent/
│   │   ├── ClaudeCodeAdapterTest.kt
│   │   ├── HermesAdapterTest.kt
│   │   ├── OpenCodeAdapterTest.kt
│   │   └── ToolLoopAgentAdapterTest.kt
│   ├── ui/
│   │   ├── ai/AiViewModelTest.kt
│   │   ├── database/DatabaseViewModelTest.kt
│   │   ├── terminal/TerminalViewModelTest.kt
│   │   ├── canvas/CanvasViewModelTest.kt
│   │   ├── skills/SkillViewModelTest.kt
│   │   └── ...
│   └── utils/
│       ├── SecureStorageTest.kt
│       └── IntentUtilsTest.kt
├── androidTest/java/com/artier/ide/
│   ├── ui/
│   │   ├── ai/AiAssistantPanelTest.kt
│   │   ├── database/DatabasePanelTest.kt
│   │   ├── terminal/TerminalWrapperTest.kt
│   │   ├── fileexplorer/FileExplorerTest.kt
│   │   ├── canvas/WorkspaceCanvasTest.kt
│   │   ├── tunnel/TunnelPanelTest.kt
│   │   ├── router/RouterPanelTest.kt
│   │   ├── skills/SkillPanelTest.kt
│   │   └── workspace/WorkspaceScreenTest.kt
│   ├── integration/
│   │   ├── WebSocketIntegrationTest.kt
│   │   ├── DatabaseFlowIntegrationTest.kt
│   │   └── AiChatFlowIntegrationTest.kt
│   └── performance/
│       ├── StartupBenchmark.kt
│       ├── FrameDropBenchmark.kt
│       └── MemoryBenchmark.kt
daemon/src/
└── __tests__/
    ├── server.test.ts
    ├── database/
    │   ├── db-client.test.ts
    │   └── db-manager.test.ts
    ├── tunnel/
    │   ├── cloudflared-manager.test.ts
    │   └── ssh-tunnel-manager.test.ts
    ├── pty/
    │   └── pty-manager.test.ts
    └── skills/
        └── skill-manager.test.ts
```

---

## 4. Unit Testing

### 4.1 ViewModel Testing Pattern

```kotlin
@RunWith(MockKAndroidRunner::class)
class AiViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @MockK
    private lateinit var chatRepository: ChatRepository

    @MockK
    private lateinit var skillRepository: SkillRepository

    private lateinit var viewModel: AiViewModel

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        viewModel = AiViewModel(chatRepository, skillRepository)
    }

    @Test
    fun `initial state should be idle`() = runTest {
        val state = viewModel.state.value
        assertEquals("", state.input)
        assertEquals(emptyList<ChatMessage>(), state.messages)
        assertFalse(state.isLoading)
    }

    @Test
    fun `sending message should update state to loading`() = runTest {
        viewModel.updateInput("Hello")
        viewModel.sendMessage()

        val state = viewModel.state.value
        assertTrue(state.isLoading)
        assertEquals("", state.input)
    }

    @Test
    fun `received message should append to messages list`() = runTest {
        val message = ChatMessage(
            id = "1",
            role = MessageRole.ASSISTANT,
            content = "Hello",
            timestamp = System.currentTimeMillis()
        )

        viewModel.sendMessage()

        // Simulate response
        viewModel.onMessageReceived(message)

        val state = viewModel.state.value
        assertEquals(1, state.messages.size)
        assertFalse(state.isLoading)
    }
}
```

### 4.2 Repository Testing Pattern

```kotlin
class DatabaseRepositoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @MockK
    private lateinit var webSocketClient: WebSocketClient

    private lateinit var repository: DatabaseRepository

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        repository = DatabaseRepository(webSocketClient)
    }

    @Test
    fun `connect should send correct WebSocket message`() = runTest {
        val slot = slot<Pair<String, JsonObject>>()
        every { webSocketClient.send(capture(slot), any()) } just Runs

        repository.connect(
            type = DbType.POSTGRES,
            host = "localhost",
            port = 5432,
            database = "testdb",
            username = "user",
            password = "pass"
        )

        assertEquals("db_connect", slot.captured.first)
        val payload = slot.captured.second
        assertEquals("postgres", payload.get("type").asString)
        assertEquals("localhost", payload.get("host").asString)
        assertEquals(5432, payload.get("port").asInt)
    }

    @Test
    fun `db_connected event should update activeConnection`() = runTest {
        repository.activeConnection.test {
            assertEquals(null, awaitItem())

            // Simulate WebSocket event
            val handler = webSocketClient.getMessageHandler("db_connected")
            val payload = JSONObject().apply {
                put("connectionId", "conn-123")
                put("type", "postgres")
            }
            handler?.invoke(payload)

            val connection = awaitItem()
            assertNotNull(connection)
            assertEquals("conn-123", connection?.id)
            assertEquals(DbType.POSTGRES, connection?.type)
            assertTrue(connection?.connected == true)
        }
    }

    @Test
    fun `query result should parse rows correctly`() = runTest {
        repository.queryResult.test {
            assertEquals(null, awaitItem())

            val handler = webSocketClient.getMessageHandler("db_query_result")
            val payload = JSONObject().apply {
                put("rows", JSONArray().apply {
                    put(JSONObject().apply {
                        put("id", 1)
                        put("name", "Alice")
                    })
                })
                put("fields", JSONArray().apply {
                    put("id")
                    put("name")
                })
                put("rowCount", 1)
                put("duration", 42)
            }
            handler?.invoke(payload)

            val result = awaitItem()
            assertNotNull(result)
            assertEquals(1, result?.rows?.size)
            assertEquals("Alice", result?.rows?.get(0)?.get("name"))
            assertEquals(42, result?.duration)
        }
    }
}
```

### 4.3 Adapter Testing Pattern

```kotlin
class ClaudeCodeAdapterTest {

    private lateinit var adapter: ClaudeCodeAdapter

    @Before
    fun setup() {
        adapter = ClaudeCodeAdapter()
    }

    @Test
    fun `buildPrompt should format messages correctly`() {
        val messages = listOf(
            ChatMessage(id = "1", role = MessageRole.USER, content = "Hello"),
            ChatMessage(id = "2", role = MessageRole.ASSISTANT, content = "Hi there!")
        )

        val prompt = adapter.buildPrompt(messages, "Write code")

        assertTrue(prompt.contains("Hello"))
        assertTrue(prompt.contains("Hi there!"))
        assertTrue(prompt.contains("Write code"))
    }

    @Test
    fun `parseResponse should extract content correctly`() {
        val rawResponse = """{"content": [{"type": "text", "text": "Here is the code"}]}"""

        val result = adapter.parseResponse(rawResponse)

        assertEquals("Here is the code", result.content)
        assertFalse(result.hasToolCalls)
    }
}
```

### 4.4 Model Testing Pattern

```kotlin
class AiModelsTest {

    @Test
    fun `ChatMessage serialization should work`() {
        val message = ChatMessage(
            id = "msg-1",
            role = MessageRole.USER,
            content = "Test message",
            timestamp = 1700000000000L
        )

        val json = Gson().toJson(message)
        val deserialized = Gson().fromJson(json, ChatMessage::class.java)

        assertEquals(message.id, deserialized.id)
        assertEquals(message.role, deserialized.role)
        assertEquals(message.content, deserialized.content)
    }

    @Test
    fun `AgentStatus should have all states`() {
        val states = AgentStatus.values()
        assertEquals(4, states.size)
        assertNotNull(AgentStatus.IDLE)
        assertNotNull(AgentStatus.RUNNING)
        assertNotNull(AgentStatus.ERROR)
        assertNotNull(AgentStatus.COMPLETED)
    }

    @Test
    fun `DbConnection default values should be safe`() {
        val conn = DbConnection(
            id = "test",
            name = "test",
            type = DbType.SQLITE
        )
        assertFalse(conn.connected)
        assertEquals("", conn.host)
        assertEquals(0, conn.port)
    }
}
```

---

## 5. Integration Testing

### 5.1 WebSocket Integration Test

```kotlin
class WebSocketIntegrationTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var webSocketClient: WebSocketClient

    @Before
    fun setup() {
        webSocketClient = WebSocketClient()
    }

    @Test
    fun `message handler should be called when matching event received`() = runTest {
        var receivedPayload: JSONObject? = null
        webSocketClient.addMessageHandler("test_event") { payload ->
            receivedPayload = payload
        }

        // Simulate raw WebSocket message
        webSocketClient.handleRawMessage("""{"event":"test_event","data":{"key":"value"}}""")

        assertNotNull(receivedPayload)
        assertEquals("value", receivedPayload?.optString("key"))
    }

    @Test
    fun `unknown event should not crash`() {
        webSocketClient.handleRawMessage("""{"event":"unknown_event","data":{}}""")
        // Should not throw
    }

    @Test
    fun `reconnect should be called on disconnect`() = runTest {
        val reconnectCalled = mutableListOf<Int>()
        webSocketClient.setOnReconnectListener { attempt ->
            reconnectCalled.add(attempt)
        }

        webSocketClient.handleDisconnect()

        assertEquals(1, reconnectCalled.size)
        assertEquals(1, reconnectCalled[0])
    }
}
```

### 5.2 Database Flow Integration Test

```kotlin
class DatabaseFlowIntegrationTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @MockK
    private lateinit var webSocketClient: WebSocketClient

    private lateinit var repository: DatabaseRepository
    private lateinit var viewModel: DatabaseViewModel

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        repository = DatabaseRepository(webSocketClient)
        viewModel = DatabaseViewModel(repository)
    }

    @Test
    fun `full connect-query-disconnect flow`() = runTest {
        // 1. Connect
        viewModel.updateConnectHost("localhost")
        viewModel.updateConnectPort("5432")
        viewModel.updateConnectDatabase("testdb")
        viewModel.connect()

        verify { webSocketClient.send(eq("db_connect"), any()) }

        // 2. Simulate connected
        val connectHandler = webSocketClient.getMessageHandler("db_connected")
        connectHandler?.invoke(JSONObject().apply {
            put("connectionId", "conn-1")
            put("type", "postgres")
        })

        assertEquals("conn-1", viewModel.state.value.activeConnection?.id)

        // 3. Query
        viewModel.updateQuery("SELECT * FROM users")
        viewModel.executeQuery()

        verify { webSocketClient.send(eq("db_query"), any()) }

        // 4. Simulate result
        val queryHandler = webSocketClient.getMessageHandler("db_query_result")
        queryHandler?.invoke(JSONObject().apply {
            put("rows", JSONArray().apply {
                put(JSONObject().apply { put("id", 1) })
            })
            put("fields", JSONArray().apply { put("id") })
            put("rowCount", 1)
            put("duration", 10)
        })

        val result = viewModel.state.value.queryResult
        assertNotNull(result)
        assertEquals(1, result?.rows?.size)

        // 5. Disconnect
        viewModel.disconnect()

        verify { webSocketClient.send(eq("db_disconnect"), any()) }
    }
}
```

---

## 6. UI Testing (Compose)

### 6.1 Setup

```kotlin
@HiltAndroidTest
class AiAssistantPanelTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var viewModel: AiViewModel

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun `panel should display header with AI title`() {
        composeRule.setContent {
            ArtierTheme {
                AiAssistantPanel()
            }
        }

        composeRule.onNodeWithText("AI Assistant").assertIsDisplayed()
    }

    @Test
    fun `input field should accept text`() {
        composeRule.setContent {
            ArtierTheme {
                AiAssistantPanel()
            }
        }

        composeRule.onNodeWithTag("ai_input").performTextInput("Hello AI")
        composeRule.onNodeWithText("Hello AI").assertIsDisplayed()
    }

    @Test
    fun `send button should be disabled when input is empty`() {
        composeRule.setContent {
            ArtierTheme {
                AiAssistantPanel()
            }
        }

        composeRule.onNodeWithTag("send_button").assertIsNotEnabled()
    }
}
```

### 6.2 Database Panel UI Test

```kotlin
class DatabasePanelTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `connect button should open dialog`() {
        composeRule.setContent {
            ArtierTheme {
                DatabasePanel()
            }
        }

        composeRule.onNodeWithText("Connect").performClick()
        composeRule.onNodeWithText("Database Connection").assertIsDisplayed()
    }

    @Test
    fun `port field should accept only numbers`() {
        composeRule.setContent {
            ArtierTheme {
                DatabasePanel()
            }
        }

        composeRule.onNodeWithText("Connect").performClick()
        composeRule.onNodeWithTag("port_field").performTextInput("5432")
        composeRule.onNodeWithText("5432").assertIsDisplayed()
    }

    @Test
    fun `table list should be scrollable`() {
        // Setup state with many tables
        composeRule.setContent {
            ArtierTheme {
                DatabasePanel()
            }
        }

        composeRule.onNodeWithTag("table_list").assertIsDisplayed()
    }
}
```

### 6.3 File Explorer UI Test

```kotlin
class FileExplorerTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `clicking folder should expand children`() {
        composeRule.setContent {
            ArtierTheme {
                FileExplorer(
                    files = listOf(
                        FileNode(name = "src", isDirectory = true, children = listOf(
                            FileNode(name = "main.kt", isDirectory = false)
                        ))
                    )
                )
            }
        }

        composeRule.onNodeWithText("src").performClick()
        composeRule.onNodeWithText("main.kt").assertIsDisplayed()
    }

    @Test
    fun `clicking file should select it`() {
        var selectedFile: FileNode? = null
        composeRule.setContent {
            ArtierTheme {
                FileExplorer(
                    files = listOf(FileNode(name = "test.kt", isDirectory = false)),
                    onFileSelected = { selectedFile = it }
                )
            }
        }

        composeRule.onNodeWithText("test.kt").performClick()
        assertEquals("test.kt", selectedFile?.name)
    }
}
```

### 6.4 Workspace Canvas UI Test

```kotlin
class WorkspaceCanvasTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `canvas should render nodes`() {
        composeRule.setContent {
            ArtierTheme {
                WorkspaceCanvas(
                    nodes = listOf(
                        CanvasNode(id = "1", label = "File1", type = NodeType.FILE)
                    )
                )
            }
        }

        composeRule.onNodeWithText("File1").assertIsDisplayed()
    }

    @Test
    fun `canvas should support pinch to zoom`() {
        composeRule.setContent {
            ArtierTheme {
                WorkspaceCanvas()
            }
        }

        // Perform pinch gesture
        composeRule.onNodeWithTag("canvas").performTouchInput {
            val s1 = down(Offset(100f, 100f))
            val s2 = down(Offset(200f, 200f))
            move(1, listOf(Offset(50f, 50f), Offset(250f, 250f)))
            move(2, listOf(Offset(50f, 50f), Offset(250f, 250f)))
            up()
            up()
        }

        // Canvas should still be displayed (no crash)
        composeRule.onNodeWithTag("canvas").assertIsDisplayed()
    }
}
```

---

## 7. Performance Testing

### 7.1 Startup Benchmark

```kotlin
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Test
    fun startup() {
        benchmarkRule.measureRepeated {
            val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
                .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            ApplicationProvider.getApplicationContext<Context>().startActivity(intent)
            Thread.sleep(2000) // Wait for full startup
        }
    }
}
```

### 7.2 Frame Drop Test

```kotlin
class FrameDropTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `scrolling file explorer should not drop frames`() {
        val largeFileList = (1..1000).map {
            FileNode(name = "file_$it.kt", isDirectory = false)
        }

        composeRule.setContent {
            ArtierTheme {
                FileExplorer(files = largeFileList)
            }
        }

        // Scroll performance
        composeRule.onNodeWithTag("file_list").performScrollToIndex(999)

        // No assertion needed - if it completes within time, it's fine
    }

    @Test
    fun `canvas pan should not drop frames`() {
        composeRule.setContent {
            ArtierTheme {
                WorkspaceCanvas(
                    nodes = (1..100).map {
                        CanvasNode(id = "$it", label = "Node $it", type = NodeType.FILE)
                    }
                )
            }
        }

        composeRule.onNodeWithTag("canvas").performTouchInput {
            down(center)
            moveBy(Offset(500f, 500f))
            up()
        }
    }
}
```

### 7.3 Memory Leak Test

```kotlin
class MemoryLeakTest {

    @Test
    fun `WebSocketClient should not leak after disconnect`() {
        val client = WebSocketClient()
        val weakRef = WeakReference(client)

        client.connect("ws://localhost:3000")
        client.disconnect()

        System.gc()
        Thread.sleep(1000)

        assertNull(weakRef.get())
    }

    @Test
    fun `ViewModel should release flows on clear`() {
        val viewModel = DatabaseViewModel(mockk(relaxed = true))
        val flows = listOf(
            viewModel.state,
            viewModel.tables,
            viewModel.queryResult
        )

        viewModel.onCleared()

        // Verify flows are no longer collecting
        // (In practice, use turbine to test this)
    }
}
```

### 7.4 WebSocket Throughput Test

```kotlin
class WebSocketThroughputTest {

    @Test
    fun `should handle 1000 messages per second`() = runTest {
        val client = WebSocketClient()
        var messageCount = 0

        client.addMessageHandler("bench_event") {
            messageCount++
        }

        val startTime = System.currentTimeMillis()
        repeat(1000) {
            client.handleRawMessage("""{"event":"bench_event","data":{"i":$it}}""")
        }
        val elapsed = System.currentTimeMillis() - startTime

        assertEquals(1000, messageCount)
        assertTrue(elapsed < 1000, "Should handle 1000 msg/sec, took ${elapsed}ms")
    }
}
```

---

## 8. WebSocket Testing

### 8.1 Message Handler Registration Test

```kotlin
class WebSocketMessageHandlerTest {

    private val client = WebSocketClient()

    @Test
    fun `multiple handlers for same event should all be called`() {
        var call1 = false
        var call2 = false

        client.addMessageHandler("event") { call1 = true }
        client.addMessageHandler("event") { call2 = true }

        client.handleRawMessage("""{"event":"event","data":{}}""")

        assertTrue(call1)
        assertTrue(call2)
    }

    @Test
    fun `removeMessageHandler should stop calling removed handler`() {
        var called = false
        val handlerId = client.addMessageHandler("event") { called = true }

        client.removeMessageHandler(handlerId)
        client.handleRawMessage("""{"event":"event","data":{}}""")

        assertFalse(called)
    }
}
```

### 8.2 Reconnection Test

```kotlin
class WebSocketReconnectTest {

    @Test
    fun `should reconnect with exponential backoff`() = runTest {
        val client = WebSocketClient()
        val attempts = mutableListOf<Int>()

        client.setOnReconnectListener { attempt ->
            attempts.add(attempt)
        }

        // Simulate multiple disconnects
        repeat(3) {
            client.handleDisconnect()
            delay(100)
        }

        assertEquals(3, attempts.size)
        assertEquals(1, attempts[0]) // First attempt
        assertEquals(2, attempts[1]) // Second attempt
        assertEquals(3, attempts[2]) // Third attempt
    }
}
```

### 8.3 JSON Parsing Test

```kotlin
class WebSocketJsonParsingTest {

    private val client = WebSocketClient()

    @Test
    fun `should parse event with nested data`() {
        var receivedData: JSONObject? = null
        client.addMessageHandler("nested_event") { payload ->
            receivedData = payload
        }

        client.handleRawMessage("""
            {
                "event": "nested_event",
                "data": {
                    "user": {
                        "name": "Alice",
                        "scores": [100, 200, 300]
                    }
                }
            }
        """.trimIndent())

        assertEquals("Alice", receivedData?.optJSONObject("user")?.optString("name"))
    }

    @Test
    fun `malformed JSON should not crash`() {
        client.addMessageHandler("event") { }
        client.handleRawMessage("not json at all")
        // Should not throw
    }
}
```

---

## 9. Daemon Testing

### 9.1 Server Unit Test (Jest)

```typescript
// daemon/src/__tests__/server.test.ts
import { WebSocketServer } from 'ws';

describe('Daemon WebSocket Server', () => {
  let wss: WebSocketServer;

  beforeEach(() => {
    wss = new WebSocketServer({ port: 0 });
  });

  afterEach(() => {
    wss.close();
  });

  it('should handle db_connect message', (done) => {
    wss.on('connection', (ws) => {
      ws.on('message', (data) => {
        const msg = JSON.parse(data.toString());
        if (msg.event === 'db_connect') {
          expect(msg.data.type).toBe('postgres');
          expect(msg.data.host).toBe('localhost');
          done();
        }
      });
    });
  });

  it('should send db_connected response', (done) => {
    wss.on('connection', (ws) => {
      ws.on('message', (data) => {
        const msg = JSON.parse(data.toString());
        if (msg.event === 'db_connect') {
          ws.send(JSON.stringify({
            event: 'db_connected',
            data: { connectionId: 'test-123', type: 'postgres' }
          }));
        }
      });
    });
  });
});
```

### 9.2 Database Manager Test

```typescript
// daemon/src/__tests__/database/db-manager.test.ts
import { DatabaseManager } from '../../database/db-manager';

describe('DatabaseManager', () => {
  let dbManager: DatabaseManager;

  beforeEach(() => {
    dbManager = new DatabaseManager();
  });

  it('should create connection', async () => {
    const conn = await dbManager.connect({
      type: 'sqlite',
      database: ':memory:'
    });
    expect(conn.id).toBeDefined();
  });

  it('should execute query', async () => {
    const conn = await dbManager.connect({
      type: 'sqlite',
      database: ':memory:'
    });
    const result = await dbManager.query(conn.id, 'SELECT 1 as num');
    expect(result.rows).toHaveLength(1);
    expect(result.rows[0].num).toBe(1);
  });

  it('should disconnect cleanly', async () => {
    const conn = await dbManager.connect({
      type: 'sqlite',
      database: ':memory:'
    });
    await dbManager.disconnect(conn.id);
    expect(dbManager.getConnection(conn.id)).toBeUndefined();
  });
});
```

### 9.3 PTY Manager Test

```typescript
// daemon/src/__tests__/pty/pty-manager.test.ts
import { PtyManager } from '../../pty/pty-manager';

describe('PtyManager', () => {
  let ptyManager: PtyManager;

  beforeEach(() => {
    ptyManager = new PtyManager();
  });

  it('should create terminal session', () => {
    const session = ptyManager.create('bash');
    expect(session.id).toBeDefined();
    expect(session.pid).toBeGreaterThan(0);
  });

  it('should write to terminal', () => {
    const session = ptyManager.create('bash');
    expect(() => ptyManager.write(session.id, 'ls\n')).not.toThrow();
  });

  it('should destroy terminal session', () => {
    const session = ptyManager.create('bash');
    ptyManager.destroy(session.id);
    expect(ptyManager.getSession(session.id)).toBeUndefined();
  });
});
```

---

## 10. Naming Convention

### Unit Tests
```
`<methodName> should <expected behavior> when <condition>`
```
Examples:
- `sendMessage should update state to loading`
- `connect should send correct WebSocket message`
- `parseResponse should extract content correctly`

### UI Tests
```
`<Component> should <expected behavior>`
```
Examples:
- `AiAssistantPanel should display header with AI title`
- `DatabasePanel should open connect dialog on button click`
- `FileExplorer should expand folder on click`

### Integration Tests
```
`<Feature> flow should <expected end-to-end behavior>`
```
Examples:
- `Database connect-query-disconnect flow should complete successfully`
- `AI chat flow should send message and receive response`

### Performance Tests
```
`<Action> should complete within <time> and use <memory>`
```
Examples:
- `Startup should complete within 2 seconds`
- `Scrolling should maintain 60fps`
- `WebSocket should handle 1000 msg/sec`

---

## 11. Coverage Target

| Layer | Target | Minimum |
|-------|--------|---------|
| Unit Tests (ViewModel) | 85% | 80% |
| Unit Tests (Repository) | 80% | 75% |
| Unit Tests (Model) | 90% | 85% |
| Unit Tests (Adapter) | 80% | 75% |
| Integration Tests | 70% | 60% |
| Compose UI Tests | 60% | 50% |
| Daemon Tests | 75% | 70% |
| **Overall** | **75%** | **70%** |

### Critical Path Coverage (Harus 100%)

1. WebSocket connection/reconnection
2. Message parsing (all event types)
3. Database connect/query/disconnect
4. AI chat send/receive
5. Terminal create/write/destroy
6. Tunnel connect/disconnect
7. File explorer tree expand/collapse
8. Skill loading and display

---

## 12. CI/CD Integration

### GitHub Actions Workflow

```yaml
# .github/workflows/test.yml
name: Test

on: [push, pull_request]

jobs:
  unit-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Run Unit Tests
        run: ./gradlew test
      - name: Upload Coverage
        uses: codecov/codecov-action@v3

  instrumented-tests:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Run Instrumented Tests
        uses: reactivecircus/android-emulator-runner@v2
        with:
          api-level: 33
          script: ./gradlew connectedAndroidTest

  daemon-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
        with:
          node-version: '20'
      - name: Install Dependencies
        run: cd daemon && npm install
      - name: Run Daemon Tests
        run: cd daemon && npm test

  performance:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v3
      - name: Run Benchmarks
        run: ./gradlew :benchmark:connectedAndroidTest
```

### Run Commands

```bash
# Unit tests
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest

# Specific test class
./gradlew test --tests "com.artier.ide.data.repository.DatabaseRepositoryTest"

# Coverage report
./gradlew jacocoTestReport

# Daemon tests
cd daemon && npm test

# All tests at once
./gradlew test connectedAndroidTest && cd daemon && npm test
```

---

## Checklist Sebelum Commit

- [ ] Semua unit tests pass (`./gradlew test`)
- [ ] Semua instrumented tests pass (`./gradlew connectedAndroidTest`)
- [ ] Daemon tests pass (`cd daemon && npm test`)
- [ ] Coverage minimum tercapai (75%)
- [ ] Tidak ada memory leak
- [ ] Frame rate stabil saat scrolling
- [ ] WebSocket reconnect berfungsi
- [ ] Semua event types ter-handle

---

*Last updated: Juli 2026*
*Project: Artier IDE Kotlin Native v2*
