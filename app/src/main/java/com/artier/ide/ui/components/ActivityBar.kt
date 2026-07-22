package com.artier.ide.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.artier.ide.ui.theme.ArtierColors
import com.artier.ide.R

enum class ActivityBarSection {
    FILES, SEARCH, GIT, EXTENSIONS, SETTINGS
}

@Composable
fun ActivityBar(
    activeSection: ActivityBarSection?,
    onSectionClick: (ActivityBarSection) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(48.dp)
            .fillMaxHeight()
            .background(ArtierColors.surfaceCharcoal)
            .drawBehind {
                // Right border
                drawLine(
                    color = ArtierColors.surfaceEdge,
                    start = Offset(size.width, 0f),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top section - main navigation
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            ActivityBarItem(
                iconRes = R.drawable.ic_folder,
                contentDescription = "Explorer",
                isSelected = activeSection == ActivityBarSection.FILES,
                onClick = { onSectionClick(ActivityBarSection.FILES) }
            )
            
            ActivityBarItem(
                iconRes = R.drawable.ic_search,
                contentDescription = "Search",
                isSelected = activeSection == ActivityBarSection.SEARCH,
                onClick = { onSectionClick(ActivityBarSection.SEARCH) }
            )
            
            ActivityBarItem(
                iconRes = R.drawable.ic_git_branch,
                contentDescription = "Source Control",
                isSelected = activeSection == ActivityBarSection.GIT,
                onClick = { onSectionClick(ActivityBarSection.GIT) }
            )
            
            ActivityBarItem(
                iconRes = R.drawable.ic_blocks,
                contentDescription = "Extensions",
                isSelected = activeSection == ActivityBarSection.EXTENSIONS,
                onClick = { onSectionClick(ActivityBarSection.EXTENSIONS) }
            )
        }
        
        // Bottom section - settings
        ActivityBarItem(
            iconRes = R.drawable.ic_settings,
            contentDescription = "Settings",
            isSelected = activeSection == ActivityBarSection.SETTINGS,
            onClick = { onSectionClick(ActivityBarSection.SETTINGS) }
        )
    }
}

@Composable
private fun ActivityBarItem(
    iconRes: Int,
    contentDescription: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clickable(onClick = onClick)
            .drawBehind {
                if (isSelected) {
                    // Left accent bar
                    drawLine(
                        color = ArtierColors.routerPurple,
                        start = Offset(0f, 0f),
                        end = Offset(0f, size.height),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = androidx.compose.ui.res.painterResource(id = iconRes),
            contentDescription = contentDescription,
            modifier = Modifier.size(22.dp),
            tint = if (isSelected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}
