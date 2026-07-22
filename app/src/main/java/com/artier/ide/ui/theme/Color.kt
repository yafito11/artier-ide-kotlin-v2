package com.artier.ide.ui.theme

import androidx.compose.ui.graphics.Color

// ==================== OBSIDIAN LOGIC DESIGN SYSTEM ====================

// Primary: 9Router Purple
val RouterPurple = Color(0xFFD4BBFF)
val OnPrimary = Color(0xFF3F0F81)
val PrimaryContainer = Color(0xFF7E57C2)

// Secondary: Kotlin Orange
val KotlinOrange = Color(0xFFFFB875)
val OnSecondary = Color(0xFF4B2800)
val SecondaryContainer = Color(0xFFE08100)

// Tertiary: Jetpack Blue
val JetpackBlue = Color(0xFF9FCAFF)
val OnTertiary = Color(0xFF003258)
val TertiaryContainer = Color(0xFF0070BC)

// Error
val ErrorRed = Color(0xFFFFB4AB)
val OnError = Color(0xFF690005)

// Surface Hierarchy
val SurfaceObsidian = Color(0xFF0A0A0A)   // Editor, Terminal bg
val SurfaceCharcoal = Color(0xFF1E1E1E)   // Sidebars
val SurfaceEdge = Color(0xFF2D2D2D)       // Headers, Borders
val Surface = Color(0xFF131313)           // General bg
val SurfaceContainer = Color(0xFF201F1F)  // Cards
val SurfaceContainerHigh = Color(0xFF2A2A2A)   // Elevated
val SurfaceContainerHighest = Color(0xFF353534)

// Text Colors
val OnSurface = Color(0xFFE5E2E1)         // High contrast
val OnSurfaceVariant = Color(0xFFCCC3D3)  // Secondary
val TextMuted = Color(0xFFA0A0A0)         // Dimmed
val TextHighContrast = Color(0xFFF5F5F5)

// Accent Colors (for direct usage)
val AccentRouterPurple = Color(0xFF9C27B0)
val AccentKotlinOrange = Color(0xFFF88909)
val AccentJetpackBlue = Color(0xFF4285F4)

// AI Panel overlay (5% purple)
val AiPanelBackground = Color(0x0D9C27B0)

// Semantic Colors
val SuccessGreen = Color(0xFF4CAF50)
val WarningYellow = Color(0xFFFFEB3B)
val InfoBlue = Color(0xFF2196F3)

// Editor Specific
val EditorBackground = SurfaceObsidian
val EditorLineNumber = Color(0xFF858585)
val EditorSelection = Color(0xFF264F78)
val EditorCurrentLine = Color(0xFF2A2D2E)
val EditorBracketMatch = Color(0xFF3E3D32)

// Terminal Specific
val TerminalBackground = SurfaceObsidian
val TerminalText = OnSurface
val TerminalCursor = OnSurface
val TerminalSuccess = SuccessGreen
val TerminalError = ErrorRed

// File Icon Colors
val FileIconKotlin = Color(0xFF7C4DFF)
val FileIconJava = Color(0xFFFF5722)
val FileIconPython = Color(0xFF4CAF50)
val FileIconJavascript = Color(0xFFFFEB3B)
val FileIconTypescript = Color(0xFF2196F3)
val FileIconHtml = Color(0xFFFF5722)
val FileIconCss = Color(0xFF2196F3)
val FileIconXml = Color(0xFFFF5722)
val FileIconJson = Color(0xFF4CAF50)
val FileIconMarkdown = Color(0xFF9E9E9E)
val FileIconSql = Color(0xFFFF9800)
val FileIconShell = Color(0xFF4CAF50)
