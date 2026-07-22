package com.artier.ide.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artier.ide.ui.theme.ArtierColors
import com.artier.ide.ui.theme.InterFontFamily

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ArtierColors.surfaceObsidian)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            
            // Title
            Text(
                text = title,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            // Subtitle
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
            
            // Action
            if (action != null) {
                Spacer(modifier = Modifier.height(8.dp))
                action()
            }
        }
    }
}

@Composable
fun EditorEmptyState(
    onOpenFile: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyState(
        icon = androidx.compose.material.icons.Icons.Filled.Code,
        title = "No file open",
        subtitle = "Open a file from the explorer to start editing",
        modifier = modifier,
        action = {
            Button(
                onClick = onOpenFile,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ArtierColors.primaryContainer
                )
            ) {
                Text(
                    text = "Open File",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp
                )
            }
        }
    )
}

@Composable
fun TerminalEmptyState(
    onNewTerminal: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyState(
        icon = androidx.compose.material.icons.Icons.Filled.Terminal,
        title = "No terminal session",
        subtitle = "Click the + button to start a new terminal",
        modifier = modifier,
        action = {
            OutlinedButton(
                onClick = onNewTerminal,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = ArtierColors.routerPurple
                )
            ) {
                Text(
                    text = "New Terminal",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp
                )
            }
        }
    )
}

@Composable
fun SearchEmptyState(
    modifier: Modifier = Modifier
) {
    EmptyState(
        icon = androidx.compose.material.icons.Icons.Filled.Search,
        title = "Search",
        subtitle = "Type in the search bar to find files or text",
        modifier = modifier
    )
}

@Composable
fun GitEmptyState(
    modifier: Modifier = Modifier
) {
    EmptyState(
        icon = androidx.compose.material.icons.Icons.Filled.GitBranch,
        title = "No Git repository",
        subtitle = "Open a folder with a Git repository to view changes",
        modifier = modifier
    )
}

@Composable
fun AiEmptyState(
    modifier: Modifier = Modifier
) {
    EmptyState(
        icon = androidx.compose.material.icons.Icons.Filled.SmartToy,
        title = "AI Assistant",
        subtitle = "Ask me anything about your code",
        modifier = modifier
    )
}
