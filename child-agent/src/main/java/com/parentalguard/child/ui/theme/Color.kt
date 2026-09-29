package com.parentalguard.child.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

// Primary Palette - Modern Indigo & Violet
val PrimaryDark = Color(0xFF121858)
val Primary = Color(0xFF3F51B5)
val PrimaryLight = Color(0xFF757DE8)

// Secondary Palette - Soft Cyan & Teal
val SecondaryDark = Color(0xFF00796B)
val Secondary = Color(0xFF00BFA5)
val SecondaryLight = Color(0xFF5DF2D6)

// Accent Colors - Vibrant & Friendly
val AccentGold = Color(0xFFFFD600)
val AccentPurple = Color(0xFF9C27B0)
val AccentPink = Color(0xFFFF4081)
val AccentOrange = Color(0xFFFF6D00)

// Surface Colors - Light Mode
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFF0F1F6)
val BackgroundLight = Color(0xFFF8F9FF)
val CardLight = Color(0xFFFFFFFF)

// Surface Colors - Dark Mode
val SurfaceDark = Color(0xFF0F121A)
val SurfaceVariantDark = Color(0xFF1A1D29)
val BackgroundDark = Color(0xFF080A0F)
val CardDark = Color(0xFF161922)

// Semantic Colors
val Success = Color(0xFF00C853)
val SuccessLight = Color(0xFFE8F5E9)
val Warning = Color(0xFFFFAB00)
val WarningLight = Color(0xFFFFF8E1)
val Error = Color(0xFFFF1744)
val ErrorLight = Color(0xFFFFEBEE)
val Info = Color(0xFF2979FF)
val InfoLight = Color(0xFFE3F2FD)

// Text Colors
val TextPrimaryLight = Color(0xFF1C1E21)
val TextSecondaryLight = Color(0xFF5F6368)
val TextTertiaryLight = Color(0xFF9AA0A6)

val TextPrimaryDark = Color(0xFFE8EAED)
val TextSecondaryDark = Color(0xFFBDC1C6)
val TextTertiaryDark = Color(0xFF80868B)

// Category Colors
val CategorySocial = Color(0xFF1877F2)
val CategoryGames = Color(0xFFFF3D00)
val CategoryEducation = Color(0xFF00C853)
val CategoryProductivity = Color(0xFF2979FF)
val CategoryEntertainment = Color(0xFFFFD600)
val CategorySystem = Color(0xFF607D8B)
val CategoryOther = Color(0xFF78909C)

// Gradient Colors - Deep Space to Midnight
val GradientStart = Color(0xFF0F172A)
val GradientMiddle = Color(0xFF1E293B)
val GradientEnd = Color(0xFF334155)

// Glass Effect Colors
val GlassWhite = Color(0x2BFFFFFF)
val GlassDark = Color(0x2B0F172A)

// Status Colors
val OnlineGreen = Color(0xFF10B981)
val OfflineRed = Color(0xFFF43F5E)
val WarningOrange = Color(0xFFF59E0B)

// ============================================================================
// Neumorphism palette — theme-aware. Light keeps the soft periwinkle voice;
// dark uses a professional slate-navy with WCAG AA text contrast.
// Every screen reads these vals, so dark mode flips automatically once
// ParentalGuardTheme syncs ChildNmTheme.isDark.
// ============================================================================

object ChildNmTheme {
    var isDark by androidx.compose.runtime.mutableStateOf(false)
}

// --- Light (porcelain pastel) ---
private val LightBg = Color(0xFFE4E9F5)
private val LightSurface = Color(0xFFEAEFFB)
private val LightInset = Color(0xFFDCE3F2)
private val LightShadow = Color(0xFFFFFFFF)
private val LightDarkShadow = Color(0xFFA3AFCA)
private val LightOnSurface = Color(0xFF2C3752)
private val LightOnSurfaceMuted = Color(0xFF7A86A1)
private val LightPrimary = Color(0xFF5B6BD6)
private val LightPrimaryDeep = Color(0xFF4555B8)
private val LightPrimarySoft = Color(0xFFEDF0FC)
private val LightSuccess = Color(0xFF3BC97E)
private val LightWarning = Color(0xFFF5B64C)
private val LightError = Color(0xFFF0657A)

// --- Dark (professional slate-navy, mirrors parent app) ---
private val DarkBg = Color(0xFF080C16)
private val DarkSurface = Color(0xFF131B31)
private val DarkInset = Color(0xFF060B15)
private val DarkLightShadow = Color(0xFF2A3A5C)
private val DarkShadow = Color(0xFF000000)
private val DarkOnSurface = Color(0xFFF1F5F9) // ~15:1 on DarkSurface
private val DarkOnSurfaceMuted = Color(0xFF9AA9C7) // ~7:1 on DarkSurface
private val DarkPrimary = Color(0xFF8F83FF) // brightened indigo, readable on dark
private val DarkPrimaryDeep = Color(0xFF7C6CFF)
private val DarkPrimarySoft = Color(0xFF23264E) // dark indigo container
private val DarkSuccess = Color(0xFF34D399)
private val DarkWarning = Color(0xFFFBBF24)
private val DarkError = Color(0xFFFB7185)

val NeumorphicBackgroundColor: Color get() = if (ChildNmTheme.isDark) DarkBg else LightBg
val NeumorphicSurface: Color get() = if (ChildNmTheme.isDark) DarkSurface else LightSurface
val NeumorphicSurfaceInset: Color get() = if (ChildNmTheme.isDark) DarkInset else LightInset
val NeumorphicLightShadow: Color get() = if (ChildNmTheme.isDark) DarkLightShadow else LightShadow
val NeumorphicDarkShadow: Color get() = if (ChildNmTheme.isDark) DarkShadow else LightDarkShadow
val NeumorphicOnSurface: Color get() = if (ChildNmTheme.isDark) DarkOnSurface else LightOnSurface
val NeumorphicOnSurfaceMuted: Color get() = if (ChildNmTheme.isDark) DarkOnSurfaceMuted else LightOnSurfaceMuted
val NeumorphicPrimary: Color get() = if (ChildNmTheme.isDark) DarkPrimary else LightPrimary
val NeumorphicPrimaryDeep: Color get() = if (ChildNmTheme.isDark) DarkPrimaryDeep else LightPrimaryDeep
val NeumorphicPrimarySoft: Color get() = if (ChildNmTheme.isDark) DarkPrimarySoft else LightPrimarySoft
val NeumorphicSuccess: Color get() = if (ChildNmTheme.isDark) DarkSuccess else LightSuccess
val NeumorphicWarning: Color get() = if (ChildNmTheme.isDark) DarkWarning else LightWarning
val NeumorphicError: Color get() = if (ChildNmTheme.isDark) DarkError else LightError
val NeumorphicOnline: Color get() = if (ChildNmTheme.isDark) DarkSuccess else LightSuccess
val NeumorphicOffline: Color get() = if (ChildNmTheme.isDark) DarkError else LightError

// Hairlines that stay visible in both modes. Never use NeumorphicDarkShadow
// (pure black in dark) directly for dividers / borders — it vanishes on navy.
val NeumorphicDivider: Color
    get() = if (ChildNmTheme.isDark) Color(0xFF2A3A5C) else LightDarkShadow.copy(alpha = 0.35f)
val NeumorphicFieldBorder: Color
    get() = if (ChildNmTheme.isDark) DarkOnSurfaceMuted.copy(alpha = 0.38f) else LightDarkShadow.copy(alpha = 0.5f)
