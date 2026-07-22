package com.artier.ide

import com.artier.ide.data.model.CursorPosition
import com.artier.ide.data.model.EditorEvent
import com.artier.ide.data.model.EditorTab
import com.artier.ide.data.model.FileNode
import com.artier.ide.data.model.TerminalSession
import com.artier.ide.data.model.TerminalState
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

class EditorTabTest {

    private lateinit var editorTab: EditorTab

    @Before
    fun setup() {
        editorTab = EditorTab(
            id = UUID.randomUUID().toString(),
            filePath = "/test/file.kt",
            fileName = "file.kt",
            content = "fun main() {}",
            isModified = false,
            cursorPosition = CursorPosition(0, 0),
            scrollPosition = 0
        )
    }

    @Test
    fun testEditorTabCreation() {
        assertNotNull(editorTab.id)
        assertEquals("/test/file.kt", editorTab.filePath)
        assertEquals("file.kt", editorTab.fileName)
        assertEquals("fun main() {}", editorTab.content)
        assertFalse(editorTab.isModified)
        assertEquals(CursorPosition(0, 0), editorTab.cursorPosition)
        assertEquals(0, editorTab.scrollPosition)
    }

    @Test
    fun testEditorTabUpdate() {
        val updatedTab = editorTab.copy(
            content = "fun main() { println() }",
            isModified = true
        )

        assertEquals("fun main() { println() }", updatedTab.content)
        assertTrue(updatedTab.isModified)
    }

    @Test
    fun testCursorPosition() {
        val position = CursorPosition(line = 10, column = 5)
        assertEquals(10, position.line)
        assertEquals(5, position.column)
    }

    @Test
    fun testEditorEvents() {
        val fileOpenedEvent = EditorEvent.FileOpened(editorTab)
        val fileSavedEvent = EditorEvent.FileSaved(editorTab.id)
        val contentChangedEvent = EditorEvent.ContentChanged(editorTab.id, "new content")
        val cursorChangedEvent = EditorEvent.CursorChanged(editorTab.id, CursorPosition(5, 10))
        val tabClosedEvent = EditorEvent.TabClosed(editorTab.id)
        val tabSwitchedEvent = EditorEvent.TabSwitched(editorTab.id)
        val errorEvent = EditorEvent.Error("Test error")

        assertTrue(fileOpenedEvent is EditorEvent.FileOpened)
        assertTrue(fileSavedEvent is EditorEvent.FileSaved)
        assertTrue(contentChangedEvent is EditorEvent.ContentChanged)
        assertTrue(cursorChangedEvent is EditorEvent.CursorChanged)
        assertTrue(tabClosedEvent is EditorEvent.TabClosed)
        assertTrue(tabSwitchedEvent is EditorEvent.TabSwitched)
        assertTrue(errorEvent is EditorEvent.Error)
    }
}

class FileNodeTest {

    @Test
    fun testFileNodeCreation() {
        val file = FileNode.FileEntry(
            name = "test.kt",
            path = "/test/test.kt",
            extension = "kt",
            size = 1024,
            lastModified = System.currentTimeMillis()
        )

        assertEquals("test.kt", file.name)
        assertEquals("/test/test.kt", file.path)
        assertEquals("kt", file.extension)
        assertEquals(1024, file.size)
    }

    @Test
    fun testDirectoryNodeCreation() {
        val directory = FileNode.Directory(
            name = "src",
            path = "/test/src",
            children = emptyList(),
            isExpanded = false
        )

        assertEquals("src", directory.name)
        assertEquals("/test/src", directory.path)
        assertTrue(directory.children.isEmpty())
        assertFalse(directory.isExpanded)
    }

    @Test
    fun testDirectoryToggle() {
        val directory = FileNode.Directory(
            name = "src",
            path = "/test/src",
            children = emptyList(),
            isExpanded = false
        )

        val expanded = directory.toggleExpanded()
        assertTrue(expanded.isExpanded)

        val collapsed = expanded.toggleExpanded()
        assertFalse(collapsed.isExpanded)
    }

    @Test
    fun testFileNodeGetName() {
        val file = FileNode.FileEntry(
            name = "test.kt",
            path = "/test/test.kt",
            extension = "kt",
            size = 1024,
            lastModified = System.currentTimeMillis()
        )

        with(FileNode) {
            assertEquals("test.kt", file.getName())
        }
    }

    @Test
    fun testDirectoryNodeGetName() {
        val directory = FileNode.Directory(
            name = "src",
            path = "/test/src",
            children = emptyList()
        )

        with(FileNode) {
            assertEquals("src", directory.getName())
        }
    }

    @Test
    fun testFileNodeGetPath() {
        val file = FileNode.FileEntry(
            name = "test.kt",
            path = "/test/test.kt",
            extension = "kt",
            size = 1024,
            lastModified = System.currentTimeMillis()
        )

        with(FileNode) {
            assertEquals("/test/test.kt", file.getPath())
        }
    }

    @Test
    fun testDirectoryNodeGetPath() {
        val directory = FileNode.Directory(
            name = "src",
            path = "/test/src",
            children = emptyList()
        )

        with(FileNode) {
            assertEquals("/test/src", directory.getPath())
        }
    }

    @Test
    fun testFileNodeIsExpanded() {
        val file = FileNode.FileEntry(
            name = "test.kt",
            path = "/test/test.kt",
            extension = "kt",
            size = 1024,
            lastModified = System.currentTimeMillis()
        )

        with(FileNode) {
            assertFalse(file.isExpanded())
        }
    }

    @Test
    fun testDirectoryNodeIsExpanded() {
        val directory = FileNode.Directory(
            name = "src",
            path = "/test/src",
            children = emptyList(),
            isExpanded = true
        )

        with(FileNode) {
            assertTrue(directory.isExpanded())
        }
    }

    @Test
    fun testFileNodeGetChildren() {
        val file = FileNode.FileEntry(
            name = "test.kt",
            path = "/test/test.kt",
            extension = "kt",
            size = 1024,
            lastModified = System.currentTimeMillis()
        )

        with(FileNode) {
            assertTrue(file.getChildren().isEmpty())
        }
    }

    @Test
    fun testDirectoryNodeGetChildren() {
        val child = FileNode.FileEntry(
            name = "test.kt",
            path = "/test/src/test.kt",
            extension = "kt",
            size = 1024,
            lastModified = System.currentTimeMillis()
        )

        val directory = FileNode.Directory(
            name = "src",
            path = "/test/src",
            children = listOf(child)
        )

        with(FileNode) {
            assertEquals(1, directory.getChildren().size)
            assertEquals("test.kt", directory.getChildren().first().getName())
        }
    }
}

class TerminalSessionTest {

    private lateinit var terminalSession: TerminalSession

    @Before
    fun setup() {
        terminalSession = TerminalSession(
            id = UUID.randomUUID().toString(),
            title = "Terminal 1",
            workingDirectory = "/home/user",
            isActive = true,
            createdAt = System.currentTimeMillis()
        )
    }

    @Test
    fun testTerminalSessionCreation() {
        assertNotNull(terminalSession.id)
        assertEquals("Terminal 1", terminalSession.title)
        assertEquals("/home/user", terminalSession.workingDirectory)
        assertTrue(terminalSession.isActive)
    }

    @Test
    fun testTerminalState() {
        val state = TerminalState(
            sessions = listOf(terminalSession),
            activeSessionId = terminalSession.id,
            outputBuffer = mapOf(terminalSession.id to listOf("output"))
        )

        assertEquals(1, state.sessions.size)
        assertEquals(terminalSession.id, state.activeSessionId)
        assertEquals(terminalSession, state.activeSession)
        assertEquals("output", state.getOutput(terminalSession.id))
    }

    @Test
    fun testTerminalStateEmpty() {
        val state = TerminalState()

        assertTrue(state.sessions.isEmpty())
        assertNull(state.activeSessionId)
        assertNull(state.activeSession)
        assertEquals("", state.getOutput("nonexistent"))
    }
}