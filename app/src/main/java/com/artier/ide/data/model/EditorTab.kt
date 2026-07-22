package com.artier.ide.data.model

import java.util.UUID

data class EditorTab(
    val id: String = UUID.randomUUID().toString(),
    val filePath: String,
    val fileName: String,
    val content: String = "",
    val isModified: Boolean = false,
    val cursorPosition: CursorPosition = CursorPosition(0, 0),
    val scrollPosition: Int = 0
)

data class CursorPosition(
    val line: Int,
    val column: Int
)

sealed class EditorEvent {
    data class FileOpened(val tab: EditorTab) : EditorEvent()
    data class FileSaved(val tabId: String) : EditorEvent()
    data class ContentChanged(val tabId: String, val content: String) : EditorEvent()
    data class CursorChanged(val tabId: String, val position: CursorPosition) : EditorEvent()
    data class TabClosed(val tabId: String) : EditorEvent()
    data class TabSwitched(val tabId: String) : EditorEvent()
    data class Error(val message: String) : EditorEvent()
}