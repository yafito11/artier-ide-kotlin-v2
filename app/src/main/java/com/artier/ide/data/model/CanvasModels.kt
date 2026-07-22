package com.artier.ide.data.model

import androidx.compose.ui.geometry.Offset

data class GraphNode(
    val id: String,
    val label: String,
    val path: String,
    val isDirectory: Boolean,
    val level: Int,
    val position: Offset = Offset.Zero,
    val width: Float = 0f,
    val height: Float = 0f
)

data class GraphEdge(
    val fromId: String,
    val toId: String,
    val fromPos: Offset = Offset.Zero,
    val toPos: Offset = Offset.Zero
)

data class CanvasState(
    val nodes: List<GraphNode> = emptyList(),
    val edges: List<GraphEdge> = emptyList(),
    val panOffset: Offset = Offset.Zero,
    val zoom: Float = 1f,
    val selectedNodeId: String? = null,
    val isLoading: Boolean = false
)
