package com.artier.ide.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artier.ide.data.model.CursorPosition
import com.artier.ide.data.model.EditorTab
import com.artier.ide.data.remote.LspClient
import com.artier.ide.data.repository.EditorRepository
import com.artier.ide.data.repository.FileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val editorRepository: EditorRepository,
    private val fileRepository: FileRepository,
    private val lspClient: LspClient
) : ViewModel() {

    val tabs: StateFlow<List<EditorTab>> = editorRepository.tabs
    val activeTabId: StateFlow<String?> = editorRepository.activeTabId

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _completionItems = MutableStateFlow<List<LspClient.CompletionItem>>(emptyList())
    val completionItems: StateFlow<List<LspClient.CompletionItem>> = _completionItems.asStateFlow()

    private val _hoverText = MutableStateFlow<String?>(null)
    val hoverText: StateFlow<String?> = _hoverText.asStateFlow()

    private val _lspStatus = MutableStateFlow("disconnected")
    val lspStatus: StateFlow<String> = _lspStatus.asStateFlow()

    private var changeDebounceJob: Job? = null
    private var rootUri: String = "file:///"

    init {
        viewModelScope.launch {
            fileRepository.fileContent.collect { contentMap ->
                contentMap.forEach { (path, content) ->
                    val tab = editorRepository.getTabByPath(path)
                    if (tab != null) {
                        editorRepository.updateContent(tab.id, content)
                    }
                }
            }
        }

        viewModelScope.launch {
            lspClient.connectionState.collect { state ->
                _lspStatus.value = when (state) {
                    is LspClient.LspConnectionState.Disconnected -> "disconnected"
                    is LspClient.LspConnectionState.Connecting -> "connecting"
                    is LspClient.LspConnectionState.Connected -> "connected (${state.serverCount})"
                    is LspClient.LspConnectionState.Error -> "error: ${state.message}"
                }
            }
        }

        viewModelScope.launch {
            lspClient.completions.collect { event ->
                _completionItems.value = event.items
            }
        }

        viewModelScope.launch {
            lspClient.hover.collect { event ->
                _hoverText.value = event.contents
            }
        }
    }

    fun setProjectRoot(path: String) {
        rootUri = lspClient.pathToUri(path)
    }

    fun openFile(path: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val fileName = path.substringAfterLast("/")
                fileRepository.loadFile(path)
                val content = fileRepository.getFileContent(path) ?: ""
                editorRepository.openFile(path, fileName, content)

                // Wire LSP for this language
                val language = lspClient.languageFromPath(path)
                if (language != "plaintext") {
                    val parent = path.substringBeforeLast("/", "/")
                    setProjectRoot(parent)
                    lspClient.ensureLanguage(language, rootUri)
                    lspClient.didOpen(language, lspClient.pathToUri(path), content)
                }
            } catch (e: Exception) {
                _error.value = "Failed to open file: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun closeTab(tabId: String) {
        val tab = editorRepository.getTabById(tabId)
        if (tab != null) {
            val language = lspClient.languageFromPath(tab.filePath)
            if (language != "plaintext") {
                lspClient.didClose(language, lspClient.pathToUri(tab.filePath))
            }
        }
        editorRepository.closeTab(tabId)
    }

    fun switchTab(tabId: String) {
        editorRepository.switchToTab(tabId)
    }

    fun updateContent(tabId: String, content: String) {
        editorRepository.updateContent(tabId, content)

        val tab = editorRepository.getTabById(tabId) ?: return
        val language = lspClient.languageFromPath(tab.filePath)
        if (language == "plaintext") return

        changeDebounceJob?.cancel()
        changeDebounceJob = viewModelScope.launch {
            delay(400)
            lspClient.didChange(language, lspClient.pathToUri(tab.filePath), content)
        }
    }

    fun requestCompletion(tabId: String, line: Int, column: Int) {
        val tab = editorRepository.getTabById(tabId) ?: return
        val language = lspClient.languageFromPath(tab.filePath)
        if (language == "plaintext") return
        lspClient.getCompletions(
            language,
            lspClient.pathToUri(tab.filePath),
            line,
            column
        )
    }

    fun requestHover(tabId: String, line: Int, column: Int) {
        val tab = editorRepository.getTabById(tabId) ?: return
        val language = lspClient.languageFromPath(tab.filePath)
        if (language == "plaintext") return
        lspClient.getHover(
            language,
            lspClient.pathToUri(tab.filePath),
            line,
            column
        )
    }

    fun clearCompletions() {
        _completionItems.value = emptyList()
    }

    fun saveCurrentFile() {
        val activeTab = editorRepository.activeTab ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                fileRepository.saveFile(activeTab.filePath, activeTab.content)
                editorRepository.saveFile(activeTab.id)
                val language = lspClient.languageFromPath(activeTab.filePath)
                if (language != "plaintext") {
                    lspClient.didSave(
                        language,
                        lspClient.pathToUri(activeTab.filePath),
                        activeTab.content
                    )
                }
            } catch (e: Exception) {
                _error.value = "Failed to save file: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveFile(tabId: String) {
        val tab = editorRepository.getTabById(tabId) ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                fileRepository.saveFile(tab.filePath, tab.content)
                editorRepository.saveFile(tabId)
                val language = lspClient.languageFromPath(tab.filePath)
                if (language != "plaintext") {
                    lspClient.didSave(language, lspClient.pathToUri(tab.filePath), tab.content)
                }
            } catch (e: Exception) {
                _error.value = "Failed to save file: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateCursorPosition(tabId: String, line: Int, column: Int) {
        editorRepository.updateCursorPosition(tabId, CursorPosition(line, column))
    }

    fun moveTab(fromIndex: Int, toIndex: Int) {
        editorRepository.moveTab(fromIndex, toIndex)
    }

    fun closeAllTabs() {
        tabs.value.forEach { tab ->
            val language = lspClient.languageFromPath(tab.filePath)
            if (language != "plaintext") {
                lspClient.didClose(language, lspClient.pathToUri(tab.filePath))
            }
        }
        editorRepository.closeAllTabs()
    }

    fun closeOtherTabs(keepTabId: String) {
        editorRepository.closeOtherTabs(keepTabId)
    }

    fun closeTabsToRight(tabId: String) {
        editorRepository.closeTabsToRight(tabId)
    }

    fun closeTabsToLeft(tabId: String) {
        editorRepository.closeTabsToLeft(tabId)
    }

    fun clearError() {
        _error.value = null
    }

    fun getCurrentContent(): String {
        return editorRepository.activeTab?.content ?: ""
    }

    fun getCurrentFilePath(): String? {
        return editorRepository.activeTab?.filePath
    }

    fun hasUnsavedChanges(): Boolean {
        return editorRepository.activeTab?.isModified ?: false
    }

    fun getUnsavedTabs(): List<EditorTab> {
        return tabs.value.filter { it.isModified }
    }

    override fun onCleared() {
        super.onCleared()
        changeDebounceJob?.cancel()
    }
}
