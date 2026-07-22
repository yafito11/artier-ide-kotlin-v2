package com.artier.ide.ui.fileexplorer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artier.ide.data.model.FileNode
import com.artier.ide.data.repository.FileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FileExplorerViewModel @Inject constructor(
    private val fileRepository: FileRepository
) : ViewModel() {

    val fileTree: StateFlow<FileNode?> = fileRepository.fileTree
    val currentPath: StateFlow<String?> = fileRepository.currentPath

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<FileNode>>(emptyList())
    val searchResults: StateFlow<List<FileNode>> = _searchResults.asStateFlow()

    fun openProject(path: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                fileRepository.openProject(path)
            } catch (e: Exception) {
                _error.value = "Failed to open project: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refresh() {
        val path = currentPath.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                fileRepository.openProject(path)
            } catch (e: Exception) {
                _error.value = "Failed to refresh: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleDirectory(path: String) {
        fileRepository.toggleDirectory(path)
    }

    fun createFile(path: String) {
        viewModelScope.launch {
            try {
                fileRepository.createFile(path)
            } catch (e: Exception) {
                _error.value = "Failed to create file: ${e.message}"
            }
        }
    }

    fun createDirectory(path: String) {
        viewModelScope.launch {
            try {
                fileRepository.createDirectory(path)
            } catch (e: Exception) {
                _error.value = "Failed to create directory: ${e.message}"
            }
        }
    }

    fun deleteFile(path: String) {
        viewModelScope.launch {
            try {
                fileRepository.deleteFile(path)
            } catch (e: Exception) {
                _error.value = "Failed to delete file: ${e.message}"
            }
        }
    }

    fun renameFile(oldPath: String, newPath: String) {
        viewModelScope.launch {
            try {
                fileRepository.renameFile(oldPath, newPath)
            } catch (e: Exception) {
                _error.value = "Failed to rename file: ${e.message}"
            }
        }
    }

    fun search(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        val results = fileRepository.searchFiles(query)
        _searchResults.value = results
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
    }

    fun clearError() {
        _error.value = null
    }

    fun getFileExtension(path: String): String {
        return fileRepository.getFileExtension(path)
    }

    fun isTextFile(path: String): Boolean {
        return fileRepository.isTextFile(path)
    }

    fun getFileNodeByPath(path: String): FileNode? {
        return findNodeByPath(fileTree.value, path)
    }

    private fun findNodeByPath(node: FileNode?, targetPath: String): FileNode? {
        if (node == null) return null

        return when (node) {
            is FileNode.FileEntry -> {
                if (node.path == targetPath) node else null
            }
            is FileNode.Directory -> {
                if (node.path == targetPath) {
                    node
                } else {
                    node.children.firstNotNullOfOrNull { child ->
                        findNodeByPath(child, targetPath)
                    }
                }
            }
        }
    }

    fun getParentPath(path: String): String? {
        val parts = path.split("/")
        return if (parts.size > 1) {
            parts.dropLast(1).joinToString("/")
        } else {
            null
        }
    }

    fun getFileName(path: String): String {
        return path.substringAfterLast("/")
    }

    fun isDirectory(path: String): Boolean {
        val node = getFileNodeByPath(path)
        return node is FileNode.Directory
    }
}