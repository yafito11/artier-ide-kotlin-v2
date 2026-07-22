package com.artier.ide

import com.artier.ide.data.model.FileNode
import com.artier.ide.data.model.FileNode.Companion.getName
import com.artier.ide.data.remote.DaemonApi
import com.artier.ide.data.remote.WebSocketClient
import com.artier.ide.data.repository.EditorRepository
import com.artier.ide.data.repository.FileRepository
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class EditorRepositoryTest {

    private lateinit var editorRepository: EditorRepository

    @Before
    fun setup() {
        editorRepository = EditorRepository()
    }

    @Test
    fun testOpenFile() {
        editorRepository.openFile("/test/file.kt", "file.kt", "content")

        assertEquals(1, editorRepository.tabs.value.size)
        assertEquals(editorRepository.tabs.value.first().id, editorRepository.activeTabId.value)
    }

    @Test
    fun testOpenExistingFile() {
        editorRepository.openFile("/test/file.kt", "file.kt", "content")
        editorRepository.openFile("/test/file.kt", "file.kt", "new content")

        // Should not create duplicate tab
        assertEquals(1, editorRepository.tabs.value.size)
    }

    @Test
    fun testCloseTab() {
        editorRepository.openFile("/test/file.kt", "file.kt", "content")
        val tabId = editorRepository.tabs.value.first().id

        editorRepository.closeTab(tabId)

        assertEquals(0, editorRepository.tabs.value.size)
        assertNull(editorRepository.activeTabId.value)
    }

    @Test
    fun testSwitchTab() {
        editorRepository.openFile("/test/file1.kt", "file1.kt", "content1")
        editorRepository.openFile("/test/file2.kt", "file2.kt", "content2")

        val tab1Id = editorRepository.tabs.value[0].id
        val tab2Id = editorRepository.tabs.value[1].id

        editorRepository.switchToTab(tab1Id)
        assertEquals(tab1Id, editorRepository.activeTabId.value)

        editorRepository.switchToTab(tab2Id)
        assertEquals(tab2Id, editorRepository.activeTabId.value)
    }

    @Test
    fun testUpdateContent() {
        editorRepository.openFile("/test/file.kt", "file.kt", "content")
        val tabId = editorRepository.tabs.value.first().id

        editorRepository.updateContent(tabId, "new content")

        val tab = editorRepository.getTabById(tabId)
        assertEquals("new content", tab?.content)
        assertTrue(tab?.isModified ?: false)
    }

    @Test
    fun testSaveFile() {
        editorRepository.openFile("/test/file.kt", "file.kt", "content")
        val tabId = editorRepository.tabs.value.first().id

        editorRepository.updateContent(tabId, "new content")
        assertTrue(editorRepository.getTabById(tabId)?.isModified ?: false)

        editorRepository.saveFile(tabId)
        assertFalse(editorRepository.getTabById(tabId)?.isModified ?: true)
    }

    @Test
    fun testMoveTab() {
        editorRepository.openFile("/test/file1.kt", "file1.kt", "content1")
        editorRepository.openFile("/test/file2.kt", "file2.kt", "content2")
        editorRepository.openFile("/test/file3.kt", "file3.kt", "content3")

        val tab1Id = editorRepository.tabs.value[0].id
        val tab2Id = editorRepository.tabs.value[1].id
        val tab3Id = editorRepository.tabs.value[2].id

        editorRepository.moveTab(0, 2)

        // After moving index 0 to index 2: [tab2, tab3, tab1]
        assertEquals(tab2Id, editorRepository.tabs.value[0].id)
        assertEquals(tab3Id, editorRepository.tabs.value[1].id)
        assertEquals(tab1Id, editorRepository.tabs.value[2].id)
    }

    @Test
    fun testCloseAllTabs() {
        editorRepository.openFile("/test/file1.kt", "file1.kt", "content1")
        editorRepository.openFile("/test/file2.kt", "file2.kt", "content2")

        editorRepository.closeAllTabs()

        assertEquals(0, editorRepository.tabs.value.size)
        assertNull(editorRepository.activeTabId.value)
    }

    @Test
    fun testCloseOtherTabs() {
        editorRepository.openFile("/test/file1.kt", "file1.kt", "content1")
        editorRepository.openFile("/test/file2.kt", "file2.kt", "content2")
        editorRepository.openFile("/test/file3.kt", "file3.kt", "content3")

        val tab1Id = editorRepository.tabs.value[0].id

        editorRepository.closeOtherTabs(tab1Id)

        assertEquals(1, editorRepository.tabs.value.size)
        assertEquals(tab1Id, editorRepository.tabs.value.first().id)
    }

    @Test
    fun testCloseTabsToRight() {
        editorRepository.openFile("/test/file1.kt", "file1.kt", "content1")
        editorRepository.openFile("/test/file2.kt", "file2.kt", "content2")
        editorRepository.openFile("/test/file3.kt", "file3.kt", "content3")

        val tab1Id = editorRepository.tabs.value[0].id

        editorRepository.closeTabsToRight(tab1Id)

        assertEquals(1, editorRepository.tabs.value.size)
        assertEquals(tab1Id, editorRepository.tabs.value.first().id)
    }

    @Test
    fun testCloseTabsToLeft() {
        editorRepository.openFile("/test/file1.kt", "file1.kt", "content1")
        editorRepository.openFile("/test/file2.kt", "file2.kt", "content2")
        editorRepository.openFile("/test/file3.kt", "file3.kt", "content3")

        val tab3Id = editorRepository.tabs.value[2].id

        editorRepository.closeTabsToLeft(tab3Id)

        assertEquals(1, editorRepository.tabs.value.size)
        assertEquals(tab3Id, editorRepository.tabs.value.first().id)
    }
}

class FileRepositoryTest {

    private lateinit var fileRepository: FileRepository
    private lateinit var mockDaemonApi: MockDaemonApi

    @Before
    fun setup() {
        mockDaemonApi = MockDaemonApi()
        fileRepository = FileRepository(mockDaemonApi)
    }

    @Test
    fun testOpenProject() {
        val tempDir = File.createTempFile("test", ".txt")
        tempDir.deleteOnExit()

        fileRepository.openProject(tempDir.absolutePath)
        assertEquals(tempDir.absolutePath, fileRepository.currentPath.value)
    }

    @Test
    fun testSearchFiles() {
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
        fileRepository.setFileTree(root)

        // Test search
        val results = fileRepository.searchFiles("main")
        assertEquals(1, results.size)
        assertEquals("main.kt", results.first().getName())
    }

    @Test
    fun testGetFileExtension() {
        assertEquals("kt", fileRepository.getFileExtension("/test/file.kt"))
        assertEquals("java", fileRepository.getFileExtension("/test/file.java"))
        assertEquals("py", fileRepository.getFileExtension("/test/file.py"))
        assertEquals("", fileRepository.getFileExtension("/test/file"))
    }

    @Test
    fun testIsTextFile() {
        assertTrue(fileRepository.isTextFile("/test/file.kt"))
        assertTrue(fileRepository.isTextFile("/test/file.java"))
        assertTrue(fileRepository.isTextFile("/test/file.py"))
        assertTrue(fileRepository.isTextFile("/test/file.json"))
        assertTrue(fileRepository.isTextFile("/test/file.md"))

        assertFalse(fileRepository.isTextFile("/test/file.png"))
        assertFalse(fileRepository.isTextFile("/test/file.exe"))
        assertFalse(fileRepository.isTextFile("/test/file.zip"))
    }
}

// Mock classes for testing
class MockDaemonApi : DaemonApi(MockWebSocketClient()) {
    // Mock implementation
}

class MockWebSocketClient : WebSocketClient() {
    var connected = false
        private set

    override fun connect(url: String) {
        connected = true
    }

    override fun disconnect() {
        connected = false
    }
}