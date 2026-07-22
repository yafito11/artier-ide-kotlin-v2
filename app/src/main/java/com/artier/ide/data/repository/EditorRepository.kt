package com.artier.ide.data.repository

import com.artier.ide.data.model.CursorPosition
import com.artier.ide.data.model.EditorEvent
import com.artier.ide.data.model.EditorTab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EditorRepository @Inject constructor() {
    private val _tabs = MutableStateFlow<List<EditorTab>>(emptyList())
    val tabs: StateFlow<List<EditorTab>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow<String?>(null)
    val activeTabId: StateFlow<String?> = _activeTabId.asStateFlow()

    private val _events = MutableStateFlow<List<EditorEvent>>(emptyList())
    val events: StateFlow<List<EditorEvent>> = _events.asStateFlow()

    val activeTab: EditorTab?
        get() = _tabs.value.find { it.id == _activeTabId.value }

    fun openFile(path: String, fileName: String, content: String = "") {
        // Check if file is already open
        val existingTab = _tabs.value.find { it.filePath == path }
        if (existingTab != null) {
            switchToTab(existingTab.id)
            return
        }

        val newTab = EditorTab(
            filePath = path,
            fileName = fileName,
            content = content
        )

        _tabs.value = _tabs.value + newTab
        _activeTabId.value = newTab.id

        addEvent(EditorEvent.FileOpened(newTab))
    }

    fun closeTab(tabId: String) {
        val tab = _tabs.value.find { it.id == tabId } ?: return

        // Check if tab has unsaved changes
        if (tab.isModified) {
            // In real implementation, show save dialog
            // For now, just close without saving
        }

        _tabs.value = _tabs.value.filter { it.id != tabId }

        // Switch to another tab if the closed tab was active
        if (_activeTabId.value == tabId) {
            _activeTabId.value = _tabs.value.firstOrNull()?.id
        }

        addEvent(EditorEvent.TabClosed(tabId))
    }

    fun switchToTab(tabId: String) {
        val tab = _tabs.value.find { it.id == tabId } ?: return
        _activeTabId.value = tabId
        addEvent(EditorEvent.TabSwitched(tabId))
    }

    fun updateContent(tabId: String, content: String) {
        _tabs.value = _tabs.value.map { tab ->
            if (tab.id == tabId) {
                tab.copy(content = content, isModified = true)
            } else {
                tab
            }
        }

        addEvent(EditorEvent.ContentChanged(tabId, content))
    }

    fun updateCursorPosition(tabId: String, position: CursorPosition) {
        _tabs.value = _tabs.value.map { tab ->
            if (tab.id == tabId) {
                tab.copy(cursorPosition = position)
            } else {
                tab
            }
        }

        addEvent(EditorEvent.CursorChanged(tabId, position))
    }

    fun saveFile(tabId: String) {
        val tab = _tabs.value.find { it.id == tabId } ?: return

        // Mark as saved
        _tabs.value = _tabs.value.map { t ->
            if (t.id == tabId) {
                t.copy(isModified = false)
            } else {
                t
            }
        }

        addEvent(EditorEvent.FileSaved(tabId))
    }

    fun getTabById(tabId: String): EditorTab? {
        return _tabs.value.find { it.id == tabId }
    }

    fun getTabByPath(path: String): EditorTab? {
        return _tabs.value.find { it.filePath == path }
    }

    fun moveTab(fromIndex: Int, toIndex: Int) {
        val currentTabs = _tabs.value.toMutableList()
        if (fromIndex in currentTabs.indices && toIndex in currentTabs.indices) {
            val tab = currentTabs.removeAt(fromIndex)
            currentTabs.add(toIndex, tab)
            _tabs.value = currentTabs
        }
    }

    fun closeAllTabs() {
        _tabs.value = emptyList()
        _activeTabId.value = null
    }

    fun closeOtherTabs(keepTabId: String) {
        _tabs.value = _tabs.value.filter { it.id == keepTabId }
        _activeTabId.value = keepTabId
    }

    fun closeTabsToRight(tabId: String) {
        val index = _tabs.value.indexOfFirst { it.id == tabId }
        if (index >= 0) {
            _tabs.value = _tabs.value.take(index + 1)
            if (_activeTabId.value !in _tabs.value.map { it.id }) {
                _activeTabId.value = _tabs.value.lastOrNull()?.id
            }
        }
    }

    fun closeTabsToLeft(tabId: String) {
        val index = _tabs.value.indexOfFirst { it.id == tabId }
        if (index >= 0) {
            _tabs.value = _tabs.value.drop(index)
            if (_activeTabId.value !in _tabs.value.map { it.id }) {
                _activeTabId.value = _tabs.value.firstOrNull()?.id
            }
        }
    }

    private fun addEvent(event: EditorEvent) {
        _events.value = _events.value + event
        // Keep only last 100 events
        if (_events.value.size > 100) {
            _events.value = _events.value.takeLast(100)
        }
    }

    fun clearEvents() {
        _events.value = emptyList()
    }
}