package com.artier.ide

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.artier.ide.data.model.EditorTab
import com.artier.ide.data.model.FileNode
import com.artier.ide.data.model.FileNode.Companion.getName
import com.artier.ide.data.repository.EditorRepository
import com.artier.ide.data.repository.FileRepository
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ArtierIDEInstrumentedTest {

    private lateinit var editorRepository: EditorRepository
    private lateinit var fileRepository: FileRepository

    @Before
    fun setup() {
        editorRepository = EditorRepository()
        fileRepository = FileRepository(MockDaemonApi())
    }

    @Test
    fun useAppContext() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.artier.ide", appContext.packageName)
    }

    @Test
    fun testEditorTabCreation() {
        val tab = EditorTab(
            filePath = "/test/file.kt",
            fileName = "file.kt",
            content = "fun main() {}"
        )

        assertEquals("/test/file.kt", tab.filePath)
        assertEquals("file.kt", tab.fileName)
        assertEquals("fun main() {}", tab.content)
        assertFalse(tab.isModified)
    }

    @Test
    fun testEditorTabManager() {
        // Open a file
        editorRepository.openFile("/test/file.kt", "file.kt", "content")
        assertEquals(1, editorRepository.tabs.value.size)

        val tab = editorRepository.tabs.value.first()
        assertEquals(tab.id, editorRepository.activeTabId.value)

        // Update content
        editorRepository.updateContent(tab.id, "new content")
        val updatedTab = editorRepository.getTabById(tab.id)
        assertTrue(updatedTab?.isModified ?: false)
        assertEquals("new content", updatedTab?.content)

        // Close tab
        editorRepository.closeTab(tab.id)
        assertEquals(0, editorRepository.tabs.value.size)
        assertNull(editorRepository.activeTabId.value)
    }

    @Test
    fun testFileNodeCreation() {
        val testDir = File.createTempFile("test", ".txt")
        testDir.deleteOnExit()

        val fileNode = FileNode.FileEntry(
            name = "test.txt",
            path = testDir.absolutePath,
            extension = "txt",
            size = 100,
            lastModified = System.currentTimeMillis()
        )

        assertEquals("test.txt", fileNode.name)
        assertEquals("txt", fileNode.extension)
        assertEquals(100, fileNode.size)
    }

    @Test
    fun testDirectoryNodeExpansion() {
        val directory = FileNode.Directory(
            name = "src",
            path = "/test/src",
            children = emptyList(),
            isExpanded = false
        )

        assertFalse(directory.isExpanded)

        val expanded = directory.toggleExpanded()
        assertTrue(expanded.isExpanded)

        val collapsed = expanded.toggleExpanded()
        assertFalse(collapsed.isExpanded)
    }

    @Test
    fun testWebSocketClientEvents() {
        val client = MockWebSocketClient()

        // Test connection
        client.connect("ws://localhost:8080/ws")
        assertTrue(client.isConnected)

        // Test disconnect
        client.disconnect()
        assertFalse(client.isConnected)
    }

    @Test
    fun testFileRepositorySearch() {
        val repository = FileRepository(MockDaemonApi())

        // Create test file tree
        val root = FileNode.Directory(
            name = "project",
            path = "/project",
            children = listOf(
                FileNode.FileEntry(
                    name = "main.kt",
                    path = "/project/main.kt",
                    extension = "kt",
                    size = 100,
                    lastModified = System.currentTimeMillis()
                ),
                FileNode.Directory(
                    name = "src",
                    path = "/project/src",
                    children = listOf(
                        FileNode.FileEntry(
                            name = "App.kt",
                            path = "/project/src/App.kt",
                            extension = "kt",
                            size = 200,
                            lastModified = System.currentTimeMillis()
                        )
                    )
                )
            )
        )

        // Set the file tree on the repository
        repository.setFileTree(root)

        // Test search
        val results = repository.searchFiles("main")
        assertEquals(1, results.size)
        assertEquals("main.kt", results.first().getName())
    }

    @Test
    fun testTerminalSessionCreation() {
        val session = com.artier.ide.data.model.TerminalSession(
            workingDirectory = "/home/user"
        )

        assertEquals("/home/user", session.workingDirectory)
        assertTrue(session.isActive)
        assertNotNull(session.id)
    }
}

// Mock classes for testing
class MockDaemonApi : com.artier.ide.data.remote.DaemonApi(MockWebSocketClient()) {
    // Mock implementation
}

class MockWebSocketClient : com.artier.ide.data.remote.WebSocketClient() {
    var isConnected = false
        private set

    override fun connect(url: String) {
        isConnected = true
    }

    override fun disconnect() {
        isConnected = false
    }
}