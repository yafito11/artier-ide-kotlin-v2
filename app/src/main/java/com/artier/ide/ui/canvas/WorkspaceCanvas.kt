package com.artier.ide.ui.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusWeak
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.artier.ide.data.model.GraphEdge
import com.artier.ide.data.model.GraphNode

@Composable
fun WorkspaceCanvas(
    modifier: Modifier = Modifier,
    viewModel: CanvasViewModel = hiltViewModel(),
    onNodeClick: (String) -> Unit = {},
    onClose: (() -> Unit)? = null
) {
    val state by viewModel.state.collectAsState()

    var graphicPanX by remember { mutableFloatStateOf(0f) }
    var graphicPanY by remember { mutableFloatStateOf(0f) }
    var graphicScale by remember { mutableFloatStateOf(1f) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CenterFocusWeak,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Project Graph",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${state.nodes.size} nodes",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    viewModel.resetView()
                    graphicPanX = 0f
                    graphicPanY = 0f
                    graphicScale = 1f
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CenterFocusWeak,
                    contentDescription = "Reset View",
                    modifier = Modifier.size(18.dp)
                )
            }
            if (onClose != null) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = graphicPanX
                    translationY = graphicPanY
                    scaleX = graphicScale
                    scaleY = graphicScale
                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        graphicPanX += pan.x
                        graphicPanY += pan.y
                        graphicScale = (graphicScale * zoom).coerceIn(0.1f, 5f)
                    }
                }
        ) {
            for (edge in state.edges) {
                drawEdge(edge)
            }

            for (node in state.nodes) {
                drawNode(node, node.id == state.selectedNodeId)
            }
        }
    }
}

private fun DrawScope.drawEdge(edge: GraphEdge) {
    val from = edge.fromPos
    val to = edge.toPos

    val path = Path().apply {
        moveTo(from.x, from.y)
        val midX = (from.x + to.x) / 2f
        cubicTo(midX, from.y, midX, to.y, to.x, to.y)
    }

    drawPath(
        path = path,
        color = Color(0xFF666666),
        style = Stroke(width = 1.5f)
    )
}

private fun DrawScope.drawNode(
    node: GraphNode,
    isSelected: Boolean
) {
    val x = node.position.x
    val y = node.position.y
    val w = node.width
    val h = node.height

    val bgColor = when {
        isSelected -> Color(0xFF2196F3).copy(alpha = 0.3f)
        node.isDirectory -> Color(0xFF4CAF50).copy(alpha = 0.15f)
        else -> Color(0xFF9E9E9E).copy(alpha = 0.1f)
    }

    drawRoundRect(
        color = bgColor,
        topLeft = Offset(x, y),
        size = Size(w, h),
        cornerRadius = CornerRadius(6f, 6f)
    )

    val borderColor = when {
        isSelected -> Color(0xFF2196F3)
        node.isDirectory -> Color(0xFF4CAF50)
        else -> Color(0xFF666666)
    }

    drawRoundRect(
        color = borderColor,
        topLeft = Offset(x, y),
        size = Size(w, h),
        cornerRadius = CornerRadius(6f, 6f),
        style = Stroke(width = if (isSelected) 2f else 1f)
    )

    val iconSize = 12f
    val iconX = x + 8f
    val iconY = y + (h - iconSize) / 2f

    val iconPaint = android.graphics.Paint().apply {
        color = if (node.isDirectory) 0xFF4CAF50.toInt() else 0xFF9E9E9E.toInt()
        textSize = iconSize
        isAntiAlias = true
    }

    val iconChar = if (node.isDirectory) "\uD83D\uDCC1" else "\uD83D\uDCC4"
    drawContext.canvas.nativeCanvas.drawText(
        iconChar,
        iconX,
        iconY + iconSize,
        iconPaint
    )

    val textPaint = android.graphics.Paint().apply {
        color = 0xFFEEEEEE.toInt()
        textSize = 11f
        isAntiAlias = true
        isFakeBoldText = node.isDirectory
    }

    val maxChars = ((w - 30f) / 6f).toInt().coerceAtLeast(3)
    val displayLabel = if (node.label.length > maxChars) {
        node.label.take(maxChars - 1) + "\u2026"
    } else {
        node.label
    }

    drawContext.canvas.nativeCanvas.drawText(
        displayLabel,
        x + 24f,
        iconY + iconSize - 2f,
        textPaint
    )
}
