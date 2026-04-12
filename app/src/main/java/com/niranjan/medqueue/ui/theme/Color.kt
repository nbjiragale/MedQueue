package com.niranjan.medqueue.ui.theme

import androidx.compose.ui.graphics.Color

// ══════════════════════════════════════════════════════════════════════════════
// M3 scheme – Medical teal/green seed (#006B5E)
// ══════════════════════════════════════════════════════════════════════════════

// ── Light scheme ─────────────────────────────────────────────────────────────
val md_theme_light_primary              = Color(0xFF006B5E)
val md_theme_light_onPrimary            = Color(0xFFFFFFFF)
val md_theme_light_primaryContainer     = Color(0xFF7CF8E0)
val md_theme_light_onPrimaryContainer   = Color(0xFF00201B)
val md_theme_light_secondary            = Color(0xFF4B635C)
val md_theme_light_onSecondary          = Color(0xFFFFFFFF)
val md_theme_light_secondaryContainer   = Color(0xFFCDE8DF)
val md_theme_light_onSecondaryContainer = Color(0xFF07201A)
val md_theme_light_tertiary             = Color(0xFF426278)
val md_theme_light_onTertiary           = Color(0xFFFFFFFF)
val md_theme_light_tertiaryContainer    = Color(0xFFC7E7FF)
val md_theme_light_onTertiaryContainer  = Color(0xFF001E2E)
val md_theme_light_error                = Color(0xFFBA1A1A)
val md_theme_light_onError              = Color(0xFFFFFFFF)
val md_theme_light_errorContainer       = Color(0xFFFFDAD6)
val md_theme_light_onErrorContainer     = Color(0xFF410002)
val md_theme_light_background           = Color(0xFFF5FAF7)
val md_theme_light_onBackground         = Color(0xFF191C1B)
val md_theme_light_surface              = Color(0xFFF5FAF7)
val md_theme_light_onSurface            = Color(0xFF191C1B)
val md_theme_light_surfaceVariant       = Color(0xFFDBE5E0)
val md_theme_light_onSurfaceVariant     = Color(0xFF3F4945)
val md_theme_light_outline              = Color(0xFF6F7975)
val md_theme_light_outlineVariant       = Color(0xFFBFC9C4)
val md_theme_light_inverseSurface       = Color(0xFF2E3130)
val md_theme_light_inverseOnSurface     = Color(0xFFEFF1EF)
val md_theme_light_inversePrimary       = Color(0xFF5DDBC4)
val md_theme_light_surfaceTint          = Color(0xFF006B5E)

// ── Dark scheme ──────────────────────────────────────────────────────────────
val md_theme_dark_primary               = Color(0xFF5DDBC4)
val md_theme_dark_onPrimary             = Color(0xFF003830)
val md_theme_dark_primaryContainer      = Color(0xFF005047)
val md_theme_dark_onPrimaryContainer    = Color(0xFF7CF8E0)
val md_theme_dark_secondary             = Color(0xFFB2CCC3)
val md_theme_dark_onSecondary           = Color(0xFF1D352F)
val md_theme_dark_secondaryContainer    = Color(0xFF344C45)
val md_theme_dark_onSecondaryContainer  = Color(0xFFCDE8DF)
val md_theme_dark_tertiary              = Color(0xFFAACBE3)
val md_theme_dark_onTertiary            = Color(0xFF103447)
val md_theme_dark_tertiaryContainer     = Color(0xFF2A4A5F)
val md_theme_dark_onTertiaryContainer   = Color(0xFFC7E7FF)
val md_theme_dark_error                 = Color(0xFFFFB4AB)
val md_theme_dark_onError               = Color(0xFF690005)
val md_theme_dark_errorContainer        = Color(0xFF93000A)
val md_theme_dark_onErrorContainer      = Color(0xFFFFDAD6)
val md_theme_dark_background            = Color(0xFF191C1B)
val md_theme_dark_onBackground          = Color(0xFFE1E3E0)
val md_theme_dark_surface               = Color(0xFF191C1B)
val md_theme_dark_onSurface             = Color(0xFFE1E3E0)
val md_theme_dark_surfaceVariant        = Color(0xFF3F4945)
val md_theme_dark_onSurfaceVariant      = Color(0xFFBFC9C4)
val md_theme_dark_outline               = Color(0xFF89938F)
val md_theme_dark_outlineVariant        = Color(0xFF3F4945)
val md_theme_dark_inverseSurface        = Color(0xFFE1E3E0)
val md_theme_dark_inverseOnSurface      = Color(0xFF2E3130)
val md_theme_dark_inversePrimary        = Color(0xFF006B5E)
val md_theme_dark_surfaceTint           = Color(0xFF5DDBC4)

// ══════════════════════════════════════════════════════════════════════════════
// Custom card & status colours (used directly by composables)
// ══════════════════════════════════════════════════════════════════════════════

// Card backgrounds – light washes
val CardPendingBg            = Color(0xFFFFF3E0)   // light warm orange
val CardDeliveredBg          = Color(0xFFE8F5E9)   // light soft green

// Status badge – bolder fill so it pops on the card
val StatusPendingBg          = Color(0xFFFF9800)   // orange
val StatusPendingContent     = Color(0xFFFFFFFF)   // white text on orange
val StatusDeliveredBg        = Color(0xFF4CAF50)   // green
val StatusDeliveredContent   = Color(0xFFFFFFFF)   // white text on green

// Left accent stripe on cards
val StripePending            = Color(0xFFFFA726)   // orange accent
val StripeDelivered          = Color(0xFF43A047)   // green accent

// Top-bar tint (medical green)
val TopBarContainer          = Color(0xFF006B5E)   // same as primary
val TopBarContent            = Color(0xFFFFFFFF)
