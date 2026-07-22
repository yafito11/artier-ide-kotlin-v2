package com.artier.ide.ui.workspace

import androidx.compose.runtime.*
import com.artier.ide.ui.components.ActivityBarSection
import com.artier.ide.ui.components.BottomPanelTab

@Composable
fun rememberWorkspaceState(): WorkspaceState {
    return remember { WorkspaceState() }
}

class WorkspaceState {
    // Left zone
    var activeActivitySection by mutableStateOf<ActivityBarSection?>(ActivityBarSection.FILES)
        private set
    
    var showSidePanel by mutableStateOf(true)
        private set
    
    // Center zone
    var activeBottomTab by mutableStateOf<BottomPanelTab?>(BottomPanelTab.TERMINAL)
        private set
    
    var showBottomPanel by mutableStateOf(false)
        private set
    
    var bottomPanelHeight by mutableStateOf(200)
        private set
    
    // Right zone
    var showRightPanel by mutableStateOf(false)
        private set
    
    var rightPanelType by mutableStateOf<RightPanelType>(RightPanelType.AI)
        private set
    
    var rightPanelWidth by mutableStateOf(320)
        private set
    
    // Responsive
    var isCompactMode by mutableStateOf(false)
        private set
    
    // Activity bar actions
    fun onActivitySectionClick(section: ActivityBarSection) {
        if (activeActivitySection == section && showSidePanel) {
            showSidePanel = false
        } else {
            activeActivitySection = section
            showSidePanel = true
        }
    }
    
    fun toggleSidePanel() {
        showSidePanel = !showSidePanel
    }
    
    // Bottom panel actions
    fun onBottomTabClick(tab: BottomPanelTab) {
        if (activeBottomTab == tab && showBottomPanel) {
            showBottomPanel = false
        } else {
            activeBottomTab = tab
            showBottomPanel = true
        }
    }
    
    fun toggleBottomPanel() {
        showBottomPanel = !showBottomPanel
    }
    
    fun setBottomPanelHeight(height: Int) {
        bottomPanelHeight = height.coerceIn(100, 500)
    }
    
    // Right panel actions
    fun toggleRightPanel(type: RightPanelType) {
        if (rightPanelType == type && showRightPanel) {
            showRightPanel = false
        } else {
            rightPanelType = type
            showRightPanel = true
        }
    }
    
    fun closeRightPanel() {
        showRightPanel = false
    }
    
    fun setRightPanelWidth(width: Int) {
        rightPanelWidth = width.coerceIn(240, 500)
    }
    
    // Responsive
    fun updateScreenMode(screenWidthDp: Int) {
        val newIsCompact = screenWidthDp < TABLET_BREAKPOINT_DP
        if (isCompactMode != newIsCompact) {
            isCompactMode = newIsCompact
            if (newIsCompact) {
                // In compact mode, close panels that don't fit
                showSidePanel = false
                showRightPanel = false
            }
        }
    }
    
    // Close all panels
    fun closeAllPanels() {
        showSidePanel = false
        showBottomPanel = false
        showRightPanel = false
    }
}

enum class RightPanelType {
    AI, SKILLS, DATABASE, CANVAS
}

private const val TABLET_BREAKPOINT_DP = 840
