package com.artier.ide.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.artier.ide.ui.components.*
import com.artier.ide.ui.editor.EditorViewModel
import com.artier.ide.ui.editor.SoraEditorWrapper
import com.artier.ide.ui.fileexplorer.FileExplorer
import com.artier.ide.ui.fileexplorer.FileExplorerViewModel
import com.artier.ide.ui.terminal.TerminalViewModel
import com.artier.ide.ui.terminal.TerminalWrapper
import com.artier.ide.ui.tunnel.TunnelPanel
import com.artier.ide.ui.tunnel.TunnelViewModel
import com.artier.ide.ui.router.RouterPanel
import com.artier.ide.ui.router.RouterViewModel
import com.artier.ide.ui.ai.AiAssistantPanel
import com.artier.ide.ui.skills.SkillPanel
import com.artier.ide.ui.database.DatabasePanel
import com.artier.ide.ui.canvas.CanvasViewModel
import com.artier.ide.ui.canvas.WorkspaceCanvas
import com.artier.ide.data.model.RouterStatus
import com.artier.ide.ui.theme.ArtierColors
import com.artier.ide.ui.theme.InterFontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private const val SIDEBAR_WIDTH_DP = 240
private const val AI_PANEL_WIDTH_DP = 320
private const val SKILLS_PANEL_WIDTH_DP = 360
private const val DATABASE_PANEL_WIDTH_DP = 400
private const val DEFAULT_BOTTOM_PANEL_HEIGHT = 200

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceScreen(
    editorViewModel: EditorViewModel = hiltViewModel(),
    fileExplorerViewModel: FileExplorerViewModel = hiltViewModel(),
    terminalViewModel: TerminalViewModel = hiltViewModel(),
    tunnelViewModel: TunnelViewModel = hiltViewModel(),
    routerViewModel: RouterViewModel = hiltViewModel(),
    canvasViewModel: CanvasViewModel = hiltViewModel()
) {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    
    val state = rememberWorkspaceState()
    
    LaunchedEffect(screenWidthDp) {
        state.updateScreenMode(screenWidthDp)
    }
    
    val editorTabs by editorViewModel.tabs.collectAsState()
    val activeTabId by editorViewModel.activeTabId.collectAsState()
    val fileTree by fileExplorerViewModel.fileTree.collectAsState()
    val terminalState by terminalViewModel.state.collectAsState()
    val tunnelState by tunnelViewModel.state.collectAsState()
    val routerState by routerViewModel.status.collectAsState()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ArtierColors.surfaceObsidian)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(
                onMenuClick = { state.toggleSidePanel() },
                onAiClick = { state.toggleRightPanel(RightPanelType.AI) },
                onSettingsClick = { /* Open settings */ }
            )
            
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                ActivityBar(
                    activeSection = state.activeActivitySection,
                    onSectionClick = { state.onActivitySectionClick(it) }
                )
                
                if (state.showSidePanel && state.activeActivitySection != null) {
                    SidePanelSection(
                        section = state.activeActivitySection!!,
                        fileTree = fileTree,
                        onFileClick = { path ->
                            if (fileExplorerViewModel.isTextFile(path)) {
                                editorViewModel.openFile(path)
                            }
                        },
                        onDirectoryClick = { path ->
                            fileExplorerViewModel.toggleDirectory(path)
                        },
                        onRefresh = { fileExplorerViewModel.refresh() },
                        modifier = Modifier.width(SIDEBAR_WIDTH_DP.dp)
                    )
                }
                
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    EditorTabs(
                        tabs = editorTabs,
                        activeTabId = activeTabId,
                        onTabClick = { editorViewModel.switchTab(it) },
                        onTabClose = { editorViewModel.closeTab(it) },
                        onNewTab = { }
                    )
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        val activeTab = editorTabs.find { it.id == activeTabId }
                        val completionItems by editorViewModel.completionItems.collectAsState()
                        
                        if (activeTab != null) {
                            SoraEditorWrapper(
                                content = activeTab.content,
                                language = activeTab.filePath.substringAfterLast('.', "txt"),
                                filePath = activeTab.filePath,
                                modifier = Modifier.fillMaxSize(),
                                onContentChanged = { content ->
                                    editorViewModel.updateContent(activeTab.id, content)
                                },
                                onCursorChanged = { line, column ->
                                    editorViewModel.updateCursorPosition(activeTab.id, line, column)
                                },
                                onCompletionRequested = { line, column ->
                                    editorViewModel.requestCompletion(activeTab.id, line, column)
                                },
                                completionItems = completionItems,
                                onClearCompletions = { editorViewModel.clearCompletions() }
                            )
                        } else {
                            EditorEmptyState(
                                onOpenFile = { state.onActivitySectionClick(ActivityBarSection.FILES) }
                            )
                        }
                    }
                    
                    if (state.showBottomPanel) {
                        BottomPanel(
                            activeTab = state.activeBottomTab ?: BottomPanelTab.TERMINAL,
                            onTabClick = { state.onBottomTabClick(it) },
                            modifier = Modifier.height(DEFAULT_BOTTOM_PANEL_HEIGHT.dp)
                        ) {
                            when (state.activeBottomTab) {
                                BottomPanelTab.TERMINAL -> {
                                    val activeSessionId = terminalState.activeSessionId
                                    if (activeSessionId != null) {
                                        TerminalWrapper(
                                            sessionId = activeSessionId,
                                            modifier = Modifier.fillMaxSize(),
                                            terminalManager = terminalViewModel.terminalManager,
                                            onOutput = { },
                                            onExit = { }
                                        )
                                    } else {
                                        TerminalEmptyState(
                                            onNewTerminal = { terminalViewModel.createSession() }
                                        )
                                    }
                                }
                                BottomPanelTab.OUTPUT -> {
                                    OutputPanel()
                                }
                                BottomPanelTab.PROBLEMS -> {
                                    ProblemsPanel()
                                }
                                BottomPanelTab.TUNNEL -> {
                                    TunnelPanel(
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                null -> {}
                            }
                        }
                    }
                }
                
                if (state.showRightPanel) {
                    RightPanelSection(
                        type = state.rightPanelType,
                        onClose = { state.closeRightPanel() },
                        modifier = Modifier.width(state.rightPanelWidth.dp)
                    )
                }
            }
            
            StatusBar(
                activeFileName = editorTabs.find { it.id == activeTabId }?.filePath,
                cursorLine = editorTabs.find { it.id == activeTabId }?.cursorPosition?.line ?: 1,
                cursorColumn = editorTabs.find { it.id == activeTabId }?.cursorPosition?.column ?: 1,
                language = editorTabs.find { it.id == activeTabId }?.filePath
                    ?.substringAfterLast('.', "txt")?.uppercase(),
                isDaemonConnected = routerState.isRunning,
                onDaemonClick = { state.toggleRightPanel(RightPanelType.AI) },
                onTerminalClick = { state.onBottomTabClick(BottomPanelTab.TERMINAL) },
                onSettingsClick = { /* Open settings */ }
            )
        }
    }
}

@Composable
private fun SidePanelSection(
    section: ActivityBarSection,
    fileTree: com.artier.ide.data.model.FileNode?,
    onFileClick: (String) -> Unit,
    onDirectoryClick: (String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (section) {
        ActivityBarSection.FILES -> {
            SidePanel(title = "Explorer") {
                FileExplorer(
                    fileTree = fileTree,
                    onFileClick = onFileClick,
                    onDirectoryClick = onDirectoryClick,
                    onRefresh = onRefresh
                )
            }
        }
        ActivityBarSection.SEARCH -> {
            SidePanel(title = "Search") {
                SearchEmptyState()
            }
        }
        ActivityBarSection.GIT -> {
            SidePanel(title = "Source Control") {
                GitEmptyState()
            }
        }
        ActivityBarSection.EXTENSIONS -> {
            SidePanel(title = "Extensions") {
                ExtensionsPanel()
            }
        }
        ActivityBarSection.SETTINGS -> {
            SidePanel(title = "Settings") {
                SettingsPanel()
            }
        }
    }
}

@Composable
private fun RightPanelSection(
    type: RightPanelType,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (type) {
        RightPanelType.AI -> {
            AiAssistantPanel(
                modifier = modifier.fillMaxHeight()
            )
        }
        RightPanelType.SKILLS -> {
            SkillPanel(
                modifier = modifier.fillMaxHeight(),
                onClose = onClose
            )
        }
        RightPanelType.DATABASE -> {
            DatabasePanel(
                modifier = modifier.fillMaxHeight(),
                onClose = onClose
            )
        }
        RightPanelType.CANVAS -> {
            WorkspaceCanvas(
                modifier = modifier.fillMaxHeight(),
                viewModel = hiltViewModel(),
                onNodeClick = { },
                onClose = onClose
            )
        }
    }
}

@Composable
private fun OutputPanel(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ArtierColors.surfaceObsidian)
            .padding(16.dp),
        contentAlignment = Alignment.TopStart
    ) {
        Text(
            text = "Output panel - No output yet",
            fontFamily = InterFontFamily,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProblemsPanel(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ArtierColors.surfaceObsidian)
            .padding(16.dp),
        contentAlignment = Alignment.TopStart
    ) {
        Text(
            text = "Problems panel - No problems found",
            fontFamily = InterFontFamily,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ExtensionsPanel(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp),
        contentAlignment = Alignment.TopStart
    ) {
        Text(
            text = "Extensions coming soon...",
            fontFamily = InterFontFamily,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsPanel(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp),
        contentAlignment = Alignment.TopStart
    ) {
        Text(
            text = "Settings coming soon...",
            fontFamily = InterFontFamily,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
