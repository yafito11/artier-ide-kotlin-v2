package com.artier.ide.ui.fileexplorer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artier.ide.data.model.FileNode

data class FlatNode(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val level: Int,
    val isExpanded: Boolean,
    val extension: String = ""
)

fun flattenFileTree(node: FileNode?, level: Int = 0): List<FlatNode> {
    if (node == null) return emptyList()
    val result = mutableListOf<FlatNode>()
    when (node) {
        is FileNode.FileEntry -> {
            result.add(
                FlatNode(
                    path = node.path,
                    name = node.name,
                    isDirectory = false,
                    level = level,
                    isExpanded = false,
                    extension = node.extension
                )
            )
        }
        is FileNode.Directory -> {
            result.add(
                FlatNode(
                    path = node.path,
                    name = node.name,
                    isDirectory = true,
                    level = level,
                    isExpanded = node.isExpanded
                )
            )
            if (node.isExpanded) {
                for (child in node.children) {
                    result.addAll(flattenFileTree(child, level + 1))
                }
            }
        }
    }
    return result
}

@Composable
fun FileExplorer(
    fileTree: FileNode?,
    onFileClick: (String) -> Unit,
    onDirectoryClick: (String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val flatNodes = remember(fileTree) { flattenFileTree(fileTree) }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(250.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        FileExplorerHeader(onRefresh = onRefresh)

        if (flatNodes.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp)
            ) {
                items(
                    items = flatNodes,
                    key = { it.path }
                ) { flatNode ->
                    FlatNodeItem(
                        node = flatNode,
                        onClick = {
                            if (flatNode.isDirectory) {
                                onDirectoryClick(flatNode.path)
                            } else {
                                onFileClick(flatNode.path)
                            }
                        }
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No project open",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun FileExplorerHeader(
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "EXPLORER",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row {
            IconButton(
                onClick = onRefresh,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun FlatNodeItem(
    node: FlatNode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .padding(start = (node.level * 16).dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (node.isDirectory) {
            Icon(
                imageVector = if (node.isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = if (node.isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        } else {
            Spacer(modifier = Modifier.width(20.dp))
            Icon(
                imageVector = getFileIcon(node.extension),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = getFileIconColor(node.extension)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = node.name,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

fun getFileIcon(extension: String): ImageVector {
    return when (extension.lowercase()) {
        "kt", "kts" -> Icons.Default.Code
        "java" -> Icons.Default.Code
        "py" -> Icons.Default.Code
        "js", "ts", "tsx", "jsx" -> Icons.Default.Code
        "html", "css", "scss", "less" -> Icons.Default.Web
        "json", "xml", "yaml", "yml" -> Icons.Default.DataObject
        "md", "txt" -> Icons.Default.Description
        "sql" -> Icons.Default.Storage
        "sh", "bash" -> Icons.Default.Terminal
        "png", "jpg", "jpeg", "gif", "svg" -> Icons.Default.InsertDriveFile
        "pdf" -> Icons.Default.Description
        "zip", "tar", "gz" -> Icons.Default.InsertDriveFile
        else -> Icons.Default.InsertDriveFile
    }
}

fun getFileIconColor(extension: String): Color {
    return when (extension.lowercase()) {
        "kt", "kts" -> Color(0xFF7C4DFF)
        "java" -> Color(0xFFFF5722)
        "py" -> Color(0xFF4CAF50)
        "js" -> Color(0xFFFFEB3B)
        "ts" -> Color(0xFF2196F3)
        "tsx", "jsx" -> Color(0xFF00BCD4)
        "html" -> Color(0xFFFF5722)
        "css" -> Color(0xFF2196F3)
        "json" -> Color(0xFF4CAF50)
        "md" -> Color(0xFF9E9E9E)
        "sql" -> Color(0xFFFF9800)
        "sh", "bash" -> Color(0xFF4CAF50)
        else -> Color(0xFF9E9E9E)
    }
}
