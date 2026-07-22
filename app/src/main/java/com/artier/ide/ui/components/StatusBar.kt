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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artier.ide.ui.theme.ArtierColors
import com.artier.ide.ui.theme.InterFontFamily

@Composable
fun StatusBar(
    activeFileName: String?,
    cursorLine: Int,
    cursorColumn: Int,
    language: String?,
    isDaemonConnected: Boolean,
    onDaemonClick: () -> Unit = {},
    onTerminalClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
            .background(ArtierColors.primaryContainer)
            .drawBehind {
                // Top border
                drawLine(
                    color = ArtierColors.surfaceEdge,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left section
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Branch indicator
            StatusBarItem(
                icon = Icons.Filled.GitBranch,
                label = "main",
                onClick = { }
            )
            
            // Sync indicator
            StatusBarItem(
                icon = Icons.Filled.Sync,
                label = "0↓ 0↑",
                onClick = { }
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Center section - cursor position
        if (activeFileName != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cursor position
                StatusBarItem(
                    label = "Ln $cursorLine, Col $cursorColumn",
                    onClick = { }
                )
                
                // Spacing
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Right section
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Language indicator
            if (language != null) {
                StatusBarItem(
                    label = language.uppercase(),
                    onClick = { }
                )
            }
            
            // Line ending
            StatusBarItem(
                label = "LF",
                onClick = { }
            )
            
            // Encoding
            StatusBarItem(
                label = "UTF-8",
                onClick = { }
            )
            
            // Daemon status
            StatusBarItem(
                icon = if (isDaemonConnected) Icons.Filled.CheckCircle else Icons.Filled.Error,
                label = if (isDaemonConnected) "Daemon" else "Offline",
                onClick = onDaemonClick,
                iconTint = if (isDaemonConnected) ArtierColors.success else ArtierColors.error
            )
            
            // Terminal shortcut
            IconButton(
                onClick = onTerminalClick,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Terminal,
                    contentDescription = "Terminal",
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            
            // Settings shortcut
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Settings",
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun StatusBarItem(
    label: String,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onPrimary
) {
    Row(
        modifier = Modifier
            .height(24.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = iconTint
            )
        }
        
        Text(
            text = label,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onPrimary,
            letterSpacing = 0.3.sp
        )
    }
}
