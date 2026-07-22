package com.artier.ide.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artier.ide.ui.theme.ArtierColors
import com.artier.ide.ui.theme.InterFontFamily
import com.artier.ide.data.model.EditorTab

@Composable
fun EditorTabs(
    tabs: List<EditorTab>,
    activeTabId: String?,
    onTabClick: (String) -> Unit,
    onTabClose: (String) -> Unit,
    onNewTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(ArtierColors.surfaceCharcoal)
            .drawBehind {
                // Bottom border
                drawLine(
                    color = ArtierColors.surfaceEdge,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
    ) {
        // Tabs
        LazyRow(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(tabs) { tab ->
                EditorTabItem(
                    tab = tab,
                    isActive = tab.id == activeTabId,
                    onClick = { onTabClick(tab.id) },
                    onClose = { onTabClose(tab.id) }
                )
            }
        }
        
        // New tab button
        IconButton(
            onClick = onNewTab,
            modifier = Modifier
                .size(40.dp)
                .fillMaxHeight()
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "New Tab",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EditorTabItem(
    tab: EditorTab,
    isActive: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit
) {
    val backgroundColor = if (isActive) {
        ArtierColors.surfaceObsidian
    } else {
        ArtierColors.surfaceCharcoal
    }
    
    val textColor = if (isActive) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Row(
        modifier = Modifier
            .height(40.dp)
            .widthIn(min = 100.dp, max = 200.dp)
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .drawBehind {
                if (isActive) {
                    // Top accent border
                    drawLine(
                        color = ArtierColors.routerPurple,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 2.dp.toPx()
                    )
                }
                // Right border
                drawLine(
                    color = ArtierColors.surfaceEdge,
                    start = Offset(size.width, 0f),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // File icon
        Icon(
            painter = androidx.compose.ui.res.painterResource(
                id = getFileIconRes(tab.filePath)
            ),
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = getFileIconColor(tab.filePath)
        )
        
        // File name
        Text(
            text = tab.filePath.substringAfterLast('/').substringAfterLast('\\'),
            fontFamily = InterFontFamily,
            fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
            fontSize = 12.sp,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        )
        
        // Modified indicator
        if (tab.isModified) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(
                        color = ArtierColors.kotlinOrange,
                        shape = MaterialTheme.shapes.small
                    )
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        
        // Close button
        IconButton(
            onClick = onClose,
            modifier = Modifier.size(20.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Close",
                modifier = Modifier.size(12.dp),
                tint = textColor.copy(alpha = 0.7f)
            )
        }
    }
}

private fun getFileIconRes(filePath: String): Int {
    val ext = filePath.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "kt" -> com.artier.ide.R.drawable.ic_file_code
        "java" -> com.artier.ide.R.drawable.ic_file_code
        "xml" -> com.artier.ide.R.drawable.ic_file_code
        "json" -> com.artier.ide.R.drawable.ic_file_code
        "js", "ts" -> com.artier.ide.R.drawable.ic_file_code
        "py" -> com.artier.ide.R.drawable.ic_file_code
        "html", "css" -> com.artier.ide.R.drawable.ic_file_code
        "md" -> com.artier.ide.R.drawable.ic_file
        else -> com.artier.ide.R.drawable.ic_file
    }
}

private fun getFileIconColor(filePath: String): androidx.compose.ui.graphics.Color {
    val ext = filePath.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "kt" -> com.artier.ide.ui.theme.FileIconKotlin
        "java" -> com.artier.ide.ui.theme.FileIconJava
        "xml" -> com.artier.ide.ui.theme.FileIconXml
        "json" -> com.artier.ide.ui.theme.FileIconJson
        "js" -> com.artier.ide.ui.theme.FileIconJavascript
        "ts" -> com.artier.ide.ui.theme.FileIconTypescript
        "py" -> com.artier.ide.ui.theme.FileIconPython
        "html" -> com.artier.ide.ui.theme.FileIconHtml
        "css" -> com.artier.ide.ui.theme.FileIconCss
        "md" -> com.artier.ide.ui.theme.FileIconMarkdown
        else -> androidx.compose.ui.graphics.Color.Gray
    }
}
