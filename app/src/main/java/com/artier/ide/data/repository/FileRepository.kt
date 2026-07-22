package com.artier.ide.data.repository

import com.artier.ide.data.model.FileNode
import com.artier.ide.data.remote.DaemonApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileRepository @Inject constructor(
    private val daemonApi: DaemonApi
) {
    private val _currentPath = MutableStateFlow<String?>(null)
    val currentPath: StateFlow<String?> = _currentPath.asStateFlow()

    private val _fileTree = MutableStateFlow<FileNode?>(null)
    val fileTree: StateFlow<FileNode?> = _fileTree.asStateFlow()

    private val _fileContent = MutableStateFlow<Map<String, String>>(emptyMap())
    val fileContent: StateFlow<Map<String, String>> = _fileContent.asStateFlow()

    init {
        // Listen for file events from daemon
        // In real implementation, this would be handled via Flow
    }

    fun openProject(path: String) {
        _currentPath.value = path
        // Load file tree from daemon or local filesystem
        loadFileTree(path)
    }

    private fun loadFileTree(path: String) {
        // For now, use local filesystem
        // In production, this should go through daemon
        val file = File(path)
        if (file.exists() && file.isDirectory) {
            _fileTree.value = FileNode.fromFile(file)
        }
    }

    fun loadFile(path: String) {
        daemonApi.readFile(path)
    }

    fun saveFile(path: String, content: String) {
        daemonApi.writeFile(path, content)
        _fileContent.value = _fileContent.value + (path to content)
    }

    fun createFile(path: String) {
        daemonApi.createFile(path)
        // Refresh file tree
        _currentPath.value?.let { loadFileTree(it) }
    }

    fun createDirectory(path: String) {
        daemonApi.createDirectory(path)
        // Refresh file tree
        _currentPath.value?.let { loadFileTree(it) }
    }

    fun deleteFile(path: String) {
        daemonApi.deleteFile(path)
        // Refresh file tree
        _currentPath.value?.let { loadFileTree(it) }
    }

    fun renameFile(oldPath: String, newPath: String) {
        daemonApi.renameFile(oldPath, newPath)
        // Refresh file tree
        _currentPath.value?.let { loadFileTree(it) }
    }

    fun setFileTree(tree: FileNode) {
        _fileTree.value = tree
    }

    fun getFileContent(path: String): String? {
        return _fileContent.value[path]
    }

    fun updateFileContent(path: String, content: String) {
        _fileContent.value = _fileContent.value + (path to content)
    }

    fun toggleDirectory(path: String) {
        val currentTree = _fileTree.value ?: return
        _fileTree.value = toggleDirectoryNode(currentTree, path)
    }

    private fun toggleDirectoryNode(node: FileNode, targetPath: String): FileNode {
        return when (node) {
            is FileNode.Directory -> {
                if (node.path == targetPath) {
                    node.toggleExpanded()
                } else {
                    node.copy(
                        children = node.children.map { child ->
                            toggleDirectoryNode(child, targetPath)
                        }
                    )
                }
            }
            is FileNode.FileEntry -> node
        }
    }

    fun searchFiles(query: String): List<FileNode> {
        val tree = _fileTree.value ?: return emptyList()
        return searchInNode(tree, query)
    }

    private fun searchInNode(node: FileNode, query: String): List<FileNode> {
        val results = mutableListOf<FileNode>()

        when (node) {
            is FileNode.FileEntry -> {
                if (node.name.contains(query, ignoreCase = true)) {
                    results.add(node)
                }
            }
            is FileNode.Directory -> {
                if (node.name.contains(query, ignoreCase = true)) {
                    results.add(node)
                }
                node.children.forEach { child ->
                    results.addAll(searchInNode(child, query))
                }
            }
        }

        return results
    }

    fun getFileExtension(path: String): String {
        return File(path).extension.lowercase()
    }

    fun isTextFile(path: String): Boolean {
        val textExtensions = setOf(
            "txt", "md", "json", "xml", "yaml", "yml", "toml",
            "kt", "java", "py", "js", "ts", "tsx", "jsx",
            "html", "css", "scss", "less", "sql", "sh", "bash",
            "properties", "gradle", "kts", "rs", "go", "c", "cpp",
            "h", "hpp", "cs", "rb", "php", "swift", "dart"
        )
        return textExtensions.contains(getFileExtension(path))
    }
}