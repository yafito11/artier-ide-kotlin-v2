package com.artier.ide.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artier.ide.ui.theme.ArtierColors
import com.artier.ide.ui.theme.InterFontFamily

enum class BottomPanelTab {
    TERMINAL, OUTPUT, PROBLEMS, TUNNEL
}

@Composable
fun BottomPanel(
    activeTab: BottomPanelTab,
    onTabClick: (BottomPanelTab) -> Unit,
    onNewTerminal: () -> Unit = {},
    modifier: Modifier = Modifier,
    headerActions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ArtierColors.surfaceObsidian)
            .drawBehind {
                // Top border
                drawLine(
                    color = ArtierColors.surfaceEdge,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }
    ) {
        // Tab bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(35.dp)
                .background(ArtierColors.surfaceCharcoal)
                .drawBehind {
                    // Bottom border
                    drawLine(
                        color = ArtierColors.surfaceEdge,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tabs
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                BottomTabItem(
                    title = "TERMINAL",
                    icon = Icons.Filled.Terminal,
                    isActive = activeTab == BottomPanelTab.TERMINAL,
                    onClick = { onTabClick(BottomPanelTab.TERMINAL) },
                    badge = null
                )
                
                BottomTabItem(
                    title = "OUTPUT",
                    icon = Icons.Filled.Output,
                    isActive = activeTab == BottomPanelTab.OUTPUT,
                    onClick = { onTabClick(BottomPanelTab.OUTPUT) },
                    badge = null
                )
                
                BottomTabItem(
                    title = "PROBLEMS",
                    icon = Icons.Filled.Warning,
                    isActive = activeTab == BottomPanelTab.PROBLEMS,
                    onClick = { onTabClick(BottomPanelTab.PROBLEMS) },
                    badge = null
                )
                
                BottomTabItem(
                    title = "TUNNEL",
                    icon = Icons.Filled.Public,
                    isActive = activeTab == BottomPanelTab.TUNNEL,
                    onClick = { onTabClick(BottomPanelTab.TUNNEL) },
                    badge = null
                )
            }
            
            // Header actions
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(end = 8.dp),
                content = headerActions
            )
        }
        
        // Panel content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            content = content
        )
    }
}

@Composable
private fun BottomTabItem(
    title: String,
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit,
    badge: Int?
) {
    val textColor = if (isActive) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Row(
        modifier = Modifier
            .height(35.dp)
            .clickable(onClick = onClick)
            .drawBehind {
                if (isActive) {
                    // Bottom accent border
                    drawLine(
                        color = ArtierColors.routerPurple,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = textColor
        )
        
        Text(
            text = title,
            fontFamily = InterFontFamily,
            fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
            fontSize = 11.sp,
            color = textColor,
            letterSpacing = 0.5.sp
        )
        
        // Badge
        if (badge != null && badge > 0) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(
                        color = if (title == "PROBLEMS") ArtierColors.warning else ArtierColors.routerPurple,
                        shape = MaterialTheme.shapes.small
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badge.toString(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = ArtierColors.surfaceObsidian
                )
            }
        }
    }
}

@Composable
fun TerminalPanelHeader(
    onNewTerminal: () -> Unit,
    onClear: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = onNewTerminal,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "New Terminal",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            IconButton(
                onClick = onClear,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Clear",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        IconButton(
            onClick = onClose,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Close Panel",
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
