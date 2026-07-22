package com.artier.ide.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.artier.ide.ui.theme.ArtierColors
import com.artier.ide.ui.theme.InterFontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun TopBar(
    onMenuClick: () -> Unit,
    onAiClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onMinimizeClick: () -> Unit = {},
    onMaximizeClick: () -> Unit = {},
    onCloseClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(ArtierColors.surfaceCharcoal)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Hamburger menu
        IconButton(
            onClick = onMenuClick,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Menu,
                contentDescription = "Menu",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // App name
        Text(
            text = "Artier IDE",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 4.dp)
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        // AI Button
        IconButton(
            onClick = onAiClick,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.SmartToy,
                contentDescription = "AI Assistant",
                modifier = Modifier.size(18.dp),
                tint = ArtierColors.routerPurple
            )
        }
        
        // Settings Button
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Settings",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Window controls
        Spacer(modifier = Modifier.width(8.dp))
        
        WindowControlButton(
            onClick = onMinimizeClick,
            icon = Icons.Filled.Minimize,
            contentDescription = "Minimize"
        )
        
        WindowControlButton(
            onClick = onMaximizeClick,
            icon = Icons.Filled.CropSquare,
            contentDescription = "Maximize"
        )
        
        WindowControlButton(
            onClick = onCloseClick,
            icon = Icons.Filled.Close,
            contentDescription = "Close",
            tint = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun WindowControlButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(28.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(14.dp),
            tint = tint
        )
    }
}
