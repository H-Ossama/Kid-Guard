package com.parentalguard.parent.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.parentalguard.parent.ui.neumorphic.Nm

/** True when the AURA night scheme is active. Components read glass tints from it. */
val LocalAuraDark = compositionLocalOf { true }

/**
 * Arabic (and other RTL scripts) must never be letter-spaced: the line
 * breaker gains mid-word break opportunities and connected letters split
 * ("النشاط" renders as "النشا ط"). Returns zero tracking for RTL locales,
 * the design value otherwise.
 */
@Composable
fun trackingFor(design: androidx.compose.ui.unit.TextUnit): androidx.compose.ui.unit.TextUnit {
    val locale = androidx.compose.ui.platform.LocalConfiguration.current
        .locales[0] ?: java.util.Locale.getDefault()
    val rtl = androidx.core.text.TextUtilsCompat.getLayoutDirectionFromLocale(locale) ==
        android.view.View.LAYOUT_DIRECTION_RTL
    return if (rtl) 0.sp else design
}

private val DarkColorScheme = darkColorScheme(
    primary = AuroraIndigo,
    onPrimary = Color(0xFF0B0A1E),
    primaryContainer = Color(0xFF2B2454),
    onPrimaryContainer = Color(0xFFD8D2FF),
    secondary = AuroraCyan,
    onSecondary = Color(0xFF03252B),
    secondaryContainer = Color(0xFF0B3540),
    onSecondaryContainer = Color(0xFFBDEFF9),
    tertiary = AuroraViolet,
    onTertiary = Color(0xFF211038),
    error = AuroraDanger,
    onError = Color(0xFF3B0A14),
    errorContainer = Color(0xFF4A1420),
    onErrorContainer = Color(0xFFFFD9DE),
    background = AuraBgBase,
    onBackground = AuraInk,
    surface = AuraBgBase,
    onSurface = AuraInk,
    surfaceVariant = AuraBgRaise,
    onSurfaceVariant = AuraInkMid,
    outline = AuraInkDim,
    outlineVariant = Color(0xFF1C2637),
    scrim = Color(0xCC02040A),
    surfaceTint = AuroraIndigo
)

private val LightColorScheme = lightColorScheme(
    primary = PorcelainIndigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5E0FF),
    onPrimaryContainer = Color(0xFF1B0658),
    secondary = PorcelainCyan,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC4F0F8),
    onSecondaryContainer = Color(0xFF03303A),
    tertiary = PorcelainViolet,
    onTertiary = Color.White,
    error = Color(0xFFE11D48),
    onError = Color.White,
    errorContainer = Color(0xFFFFE0E5),
    onErrorContainer = Color(0xFF5A0E1E),
    background = AuraPaperBg,
    onBackground = AuraPaperInk,
    surface = AuraPaperBg,
    onSurface = AuraPaperInk,
    surfaceVariant = AuraPaperRaise,
    onSurfaceVariant = AuraPaperInkMid,
    outline = AuraPaperInkDim,
    outlineVariant = Color(0xFFDDE3EE),
    scrim = Color(0x990B1220),
    surfaceTint = PorcelainIndigo
)

@Composable
fun ParentalGuardTheme(
    themeMode: ThemePreferences.ThemeMode? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val darkTheme = when (themeMode ?: ThemePreferences.getThemeMode(context)) {
        ThemePreferences.ThemeMode.LIGHT -> false
        ThemePreferences.ThemeMode.DARK -> true
        ThemePreferences.ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity
            activity?.window?.let { window ->
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
                WindowCompat.setDecorFitsSystemWindows(window, false)
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    // Display titles carry tight (negative) tracking that splits Arabic
    // words — neutralize it for RTL locales (see trackingFor).
    val locale = androidx.compose.ui.platform.LocalConfiguration.current
        .locales[0] ?: java.util.Locale.getDefault()
    val isRtl = androidx.core.text.TextUtilsCompat.getLayoutDirectionFromLocale(locale) ==
        android.view.View.LAYOUT_DIRECTION_RTL
    val typography = if (isRtl) {
        ParentalGuardTypography.copy(
            displayLarge = ParentalGuardTypography.displayLarge.copy(letterSpacing = 0.sp),
            displayMedium = ParentalGuardTypography.displayMedium.copy(letterSpacing = 0.sp)
        )
    } else {
        ParentalGuardTypography
    }
    // Keep the legacy Neumorphic (Nm) kit in sync with Material dark state.
    // Nm.isDark is SnapshotState so every Nm.* reader recomposes on change,
    // guaranteeing text stays on a contrasting surface in both modes.
    Nm.isDark = darkTheme
    CompositionLocalProvider(LocalAuraDark provides darkTheme) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = typography,
            shapes = ParentalGuardShapes,
            content = content
        )
    }
}

fun getCategoryColor(category: com.parentalguard.common.model.AppCategory): Color {
    return when (category) {
        com.parentalguard.common.model.AppCategory.SOCIAL -> CategorySocial
        com.parentalguard.common.model.AppCategory.GAMES -> CategoryGames
        com.parentalguard.common.model.AppCategory.EDUCATION -> CategoryEducation
        com.parentalguard.common.model.AppCategory.PRODUCTIVITY -> CategoryProductivity
        com.parentalguard.common.model.AppCategory.ENTERTAINMENT -> CategoryEntertainment
        com.parentalguard.common.model.AppCategory.SYSTEM -> CategorySystem
        com.parentalguard.common.model.AppCategory.OTHER -> CategoryOther
    }
}
