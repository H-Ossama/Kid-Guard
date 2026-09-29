package com.parentalguard.child.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = NeumorphicPrimary,
    onPrimary = Color.White,
    primaryContainer = NeumorphicPrimarySoft,
    onPrimaryContainer = NeumorphicPrimaryDeep,
    
    secondary = NeumorphicPrimary,
    onSecondary = Color.White,
    secondaryContainer = NeumorphicPrimarySoft,
    onSecondaryContainer = NeumorphicPrimaryDeep,
    
    tertiary = NeumorphicPrimaryDeep,
    onTertiary = Color.White,
    tertiaryContainer = NeumorphicPrimarySoft,
    onTertiaryContainer = NeumorphicPrimaryDeep,
    
    error = NeumorphicError,
    onError = Color.White,
    errorContainer = Color(0xFFFDEBEF),
    onErrorContainer = NeumorphicError,
    
    background = NeumorphicBackgroundColor,
    onBackground = NeumorphicOnSurface,
    
    surface = NeumorphicSurface,
    onSurface = NeumorphicOnSurface,
    surfaceVariant = NeumorphicSurfaceInset,
    onSurfaceVariant = NeumorphicOnSurfaceMuted,
    
    outline = NeumorphicDarkShadow,
    outlineVariant = Color(0xFFD5DCEA),
    
    inverseSurface = NeumorphicOnSurface,
    inverseOnSurface = NeumorphicSurface,
    inversePrimary = NeumorphicPrimary,
    
    surfaceTint = NeumorphicPrimary,
    scrim = Color.Black
)

private val DarkColorScheme = darkColorScheme(
    // Professional slate-navy dark, aligned with the Neumorphic dark palette.
    // All on-colors are near-white for WCAG AA contrast on navy.
    primary = Color(0xFF8F83FF),
    onPrimary = Color(0xFF0B0A1E),
    primaryContainer = Color(0xFF2B2454),
    onPrimaryContainer = Color(0xFFD8D2FF),

    secondary = Color(0xFF22D3EE),
    onSecondary = Color(0xFF03252B),
    secondaryContainer = Color(0xFF0B3540),
    onSecondaryContainer = Color(0xFFBDEFF9),

    tertiary = Color(0xFFA78BFA),
    onTertiary = Color(0xFF211038),
    tertiaryContainer = Color(0xFF2B2454),
    onTertiaryContainer = Color(0xFFEDE7F6),

    error = Color(0xFFFB7185),
    onError = Color(0xFF3B0A14),
    errorContainer = Color(0xFF4A1420),
    onErrorContainer = Color(0xFFFFD9DE),

    background = Color(0xFF080C16),
    onBackground = Color(0xFFF1F5F9),

    surface = Color(0xFF080C16),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF131B31),
    onSurfaceVariant = Color(0xFF9AA9C7),

    outline = Color(0xFF5B6B84),
    outlineVariant = Color(0xFF1C2637),

    inverseSurface = Color(0xFFF4F6FB),
    inverseOnSurface = Color(0xFF0B1220),
    inversePrimary = Color(0xFF5B6BD6),

    surfaceTint = Color(0xFF8F83FF),
    scrim = Color(0xCC02040A)
)

// Premium gradient brush for backgrounds
val PremiumGradient = Brush.verticalGradient(
    colors = listOf(GradientStart, GradientMiddle, GradientEnd)
)

val PremiumHorizontalGradient = Brush.horizontalGradient(
    colors = listOf(GradientStart, GradientEnd)
)

val CardGradient = Brush.linearGradient(
    colors = listOf(
        Primary.copy(alpha = 0.9f),
        Secondary.copy(alpha = 0.9f)
    )
)

@Composable
fun ParentalGuardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    // Keep the Neumorphic kit in sync: every Neumorphic* val reads this state,
    // so the whole child UI flips to the dark slate palette automatically.
    ChildNmTheme.isDark = darkTheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val context = view.context
            if (context is Activity) {
                val window = context.window
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
                WindowCompat.setDecorFitsSystemWindows(window, false)
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ParentalGuardTypography,
        shapes = ParentalGuardShapes,
        content = content
    )
}

// Extension function to get category color
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
