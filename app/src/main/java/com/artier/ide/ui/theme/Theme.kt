package com.artier.ide.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ==================== OBSIDIAN LOGIC COLOR SCHEME ====================

private val DarkColorScheme = darkColorScheme(
    primary = RouterPurple,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    secondary = KotlinOrange,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    tertiary = JetpackBlue,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryContainer,
    error = ErrorRed,
    onError = OnError,
    background = Surface,
    onBackground = OnSurface,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceEdge,
    onSurfaceVariant = OnSurfaceVariant,
    surfaceContainerLow = SurfaceContainer,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
    outline = SurfaceEdge,
    outlineVariant = SurfaceContainerHigh
)

// ==================== CUSTOM COLORS (Not in Material3) ====================

data class ArtierCustomColors(
    val surfaceObsidian: Color,
    val surfaceCharcoal: Color,
    val surfaceEdge: Color,
    val textMuted: Color,
    val textHighContrast: Color,
    val accentRouterPurple: Color,
    val accentKotlinOrange: Color,
    val accentJetpackBlue: Color,
    val aiPanelBackground: Color,
    val success: Color,
    val warning: Color,
    val info: Color
)

val LocalArtierCustomColors = staticCompositionLocalOf {
    ArtierCustomColors(
        surfaceObsidian = SurfaceObsidian,
        surfaceCharcoal = SurfaceCharcoal,
        surfaceEdge = SurfaceEdge,
        textMuted = TextMuted,
        textHighContrast = TextHighContrast,
        accentRouterPurple = AccentRouterPurple,
        accentKotlinOrange = AccentKotlinOrange,
        accentJetpackBlue = AccentJetpackBlue,
        aiPanelBackground = AiPanelBackground,
        success = SuccessGreen,
        warning = WarningYellow,
        info = InfoBlue
    )
}

// ==================== THEME ====================

@Composable
fun ArtierTheme(
    content: @Composable () -> Unit
) {
    // Artier IDE is always dark theme (Obsidian Logic)
    val colorScheme = DarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Surface.toArgb()
            window.navigationBarColor = SurfaceObsidian.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalArtierCustomColors provides ArtierCustomColors(
            surfaceObsidian = SurfaceObsidian,
            surfaceCharcoal = SurfaceCharcoal,
            surfaceEdge = SurfaceEdge,
            textMuted = TextMuted,
            textHighContrast = TextHighContrast,
            accentRouterPurple = AccentRouterPurple,
            accentKotlinOrange = AccentKotlinOrange,
            accentJetpackBlue = AccentJetpackBlue,
            aiPanelBackground = AiPanelBackground,
            success = SuccessGreen,
            warning = WarningYellow,
            info = InfoBlue
        )
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = UiTypography,
            content = content
        )
    }
}

// ==================== EXTENSION PROPERTIES ====================

object ArtierColors {
    val surfaceObsidian get() = SurfaceObsidian
    val surfaceCharcoal get() = SurfaceCharcoal
    val surfaceEdge get() = SurfaceEdge
    val textMuted get() = TextMuted
    val textHighContrast get() = TextHighContrast
    val routerPurple get() = AccentRouterPurple
    val kotlinOrange get() = AccentKotlinOrange
    val jetpackBlue get() = AccentJetpackBlue
    val success get() = SuccessGreen
    val warning get() = WarningYellow
    val info get() = InfoBlue
    val editorBackground get() = EditorBackground
    val terminalBackground get() = TerminalBackground
}
