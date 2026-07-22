package com.artier.ide.ui.canvas

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import com.artier.ide.data.model.CanvasState
import com.artier.ide.data.model.FileNode
import com.artier.ide.data.model.GraphEdge
import com.artier.ide.data.model.GraphNode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class CanvasViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(CanvasState())
    val state: StateFlow<CanvasState> = _state.asStateFlow()

    private val nodeWidth = 140f
    private val nodeHeight = 32f
    private val levelGapX = 180f
    private val siblingGapY = 44f
    private val rootOffsetX = 60f
    private val rootOffsetY = 40f

    fun buildFromTree(fileTree: FileNode?) {
        if (fileTree == null) {
            _state.update { it.copy(nodes = emptyList(), edges = emptyList()) }
            return
        }
        val nodes = mutableListOf<GraphNode>()
        val edges = mutableListOf<GraphEdge>()
        layoutTree(fileTree, 0, 0f, nodes, edges)
        _state.update {
            it.copy(
                nodes = nodes,
                edges = edges,
                panOffset = Offset.Zero,
                zoom = 1f,
                selectedNodeId = null
            )
        }
    }

    private fun layoutTree(
        node: FileNode,
        level: Int,
        startY: Float,
        nodes: MutableList<GraphNode>,
        edges: MutableList<GraphEdge>
    ): Float {
        val id = when (node) {
            is FileNode.FileEntry -> node.path
            is FileNode.Directory -> node.path
        }
        val label = when (node) {
            is FileNode.FileEntry -> node.name
            is FileNode.Directory -> node.name
        }
        val isDir = node is FileNode.Directory
        val children = when (node) {
            is FileNode.FileEntry -> emptyList()
            is FileNode.Directory -> node.children
        }

        if (children.isEmpty()) {
            val x = rootOffsetX + level * levelGapX
            val y = startY
            nodes.add(
                GraphNode(
                    id = id,
                    label = label,
                    path = id,
                    isDirectory = isDir,
                    level = level,
                    position = Offset(x, y),
                    width = nodeWidth,
                    height = nodeHeight
                )
            )
            return y + siblingGapY
        }

        var currentY = startY
        val childIds = mutableListOf<String>()
        for (child in children) {
            currentY = layoutTree(child, level + 1, currentY, nodes, edges)
            val childId = when (child) {
                is FileNode.FileEntry -> child.path
                is FileNode.Directory -> child.path
            }
            childIds.add(childId)
        }

        val firstChildY = nodes.find { it.id == childIds.firstOrNull() }?.position?.y ?: startY
        val lastChildY = nodes.find { it.id == childIds.lastOrNull() }?.position?.y ?: startY
        val centerY = (firstChildY + lastChildY) / 2f

        val x = rootOffsetX + level * levelGapX
        nodes.add(
            GraphNode(
                id = id,
                label = label,
                path = id,
                isDirectory = isDir,
                level = level,
                position = Offset(x, centerY),
                width = nodeWidth,
                height = nodeHeight
            )
        )

        val parentPos = Offset(x + nodeWidth, centerY + nodeHeight / 2f)
        for (childId in childIds) {
            val childNode = nodes.find { it.id == childId } ?: continue
            val childPos = Offset(childNode.position.x, childNode.position.y + nodeHeight / 2f)
            edges.add(
                GraphEdge(
                    fromId = id,
                    toId = childId,
                    fromPos = parentPos,
                    toPos = childPos
                )
            )
        }

        return currentY
    }

    fun updatePan(offset: Offset) {
        _state.update { it.copy(panOffset = it.panOffset + offset) }
    }

    fun updateZoom(zoom: Float) {
        _state.update { it.copy(zoom = zoom.coerceIn(0.2f, 3f)) }
    }

    fun selectNode(nodeId: String?) {
        _state.update { it.copy(selectedNodeId = nodeId) }
    }

    fun resetView() {
        _state.update { it.copy(panOffset = Offset.Zero, zoom = 1f) }
    }

    fun getNodeAtPosition(screenX: Float, screenY: Float): GraphNode? {
        val s = _state.value
        val worldX = (screenX - s.panOffset.x) / s.zoom
        val worldY = (screenY - s.panOffset.y) / s.zoom
        return s.nodes.find { node ->
            worldX >= node.position.x &&
                    worldX <= node.position.x + node.width &&
                    worldY >= node.position.y &&
                    worldY <= node.position.y + node.height
        }
    }
}
