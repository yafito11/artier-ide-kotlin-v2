package com.artier.ide.data.model

import java.io.File as JavaFile

sealed class FileNode {
    data class FileEntry(
        val name: String,
        val path: String,
        val extension: String,
        val size: Long,
        val lastModified: Long,
        val isHidden: Boolean = false
    ) : FileNode()

    data class Directory(
        val name: String,
        val path: String,
        val children: List<FileNode>,
        val isExpanded: Boolean = false,
        val isHidden: Boolean = false
    ) : FileNode() {
        fun toggleExpanded(): Directory = copy(isExpanded = !isExpanded)
    }

    companion object {
        fun fromFile(file: JavaFile, includeHidden: Boolean = false): FileNode? {
            if (!includeHidden && file.name.startsWith(".")) {
                return null
            }

            return if (file.isDirectory) {
                val children = file.listFiles()
                    ?.mapNotNull { fromFile(it, includeHidden) }
                    ?.sortedWith(compareBy<FileNode> {
                        it is Directory
                    }.thenBy {
                        it.getName().lowercase()
                    }) ?: emptyList()

                Directory(
                    name = file.name,
                    path = file.absolutePath,
                    children = children
                )
            } else {
                FileEntry(
                    name = file.name,
                    path = file.absolutePath,
                    extension = file.extension.lowercase(),
                    size = file.length(),
                    lastModified = file.lastModified()
                )
            }
        }

        fun FileNode.getName(): String = when (this) {
            is FileEntry -> name
            is Directory -> name
        }

        fun FileNode.getPath(): String = when (this) {
            is FileEntry -> path
            is Directory -> path
        }

        fun FileNode.isExpanded(): Boolean = when (this) {
            is FileEntry -> false
            is Directory -> isExpanded
        }

        fun FileNode.getChildren(): List<FileNode> = when (this) {
            is FileEntry -> emptyList()
            is Directory -> children
        }
    }
}