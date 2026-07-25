package com.niranjan.medqueue.ui.theme

import androidx.compose.ui.graphics.Color

// ══════════════════════════════════════════════════════════════════════════════
// Design tokens — "MedQueue Redesign.dc.html"
//
// These are the raw CSS custom properties from the mockup, kept under their
// original names so a diff against the design file stays readable.
// ══════════════════════════════════════════════════════════════════════════════

val Ink        = Color(0xFF101828)
val Paper      = Color(0xFFF4F6F7)
val SurfaceLt  = Color(0xFFFFFFFF)
val Teal       = Color(0xFF0F766E)
val TealDark   = Color(0xFF0B5C56)
val TealTint   = Color(0xFFE3F4F1)
val Amber      = Color(0xFFB45309)
val AmberTint  = Color(0xFFFDF1E2)
val Red        = Color(0xFFBE123C)
val RedTint    = Color(0xFFFDEAF0)
val Line       = Color(0xFFE7EAEC)
val Muted      = Color(0xFF667085)

/** Placeholder text inside the mockup's filled input chips. */
val Placeholder = Color(0xFFA6ADB8)

/** Dashed-outline colour used by the empty prescription slot. */
val DashedLine = Color(0xFFCFD7DC)

/** Inactive switch track. */
val TrackOff = Color(0xFFDCDFE3)

/** WhatsApp action button in the detail screen's contact row. */
val WhatsAppTint = Color(0xFFE9F7EE)
val WhatsAppInk  = Color(0xFF1A7A3E)

// ══════════════════════════════════════════════════════════════════════════════
// M3 scheme derived from the tokens above
// ══════════════════════════════════════════════════════════════════════════════

// ── Light ────────────────────────────────────────────────────────────────────
val md_theme_light_primary              = Teal
val md_theme_light_onPrimary            = Color(0xFFFFFFFF)
val md_theme_light_primaryContainer     = TealTint
val md_theme_light_onPrimaryContainer   = TealDark
val md_theme_light_secondary            = Amber
val md_theme_light_onSecondary          = Color(0xFFFFFFFF)
val md_theme_light_secondaryContainer   = AmberTint
val md_theme_light_onSecondaryContainer = Amber
val md_theme_light_tertiary             = WhatsAppInk
val md_theme_light_onTertiary           = Color(0xFFFFFFFF)
val md_theme_light_tertiaryContainer    = WhatsAppTint
val md_theme_light_onTertiaryContainer  = WhatsAppInk
val md_theme_light_error                = Red
val md_theme_light_onError              = Color(0xFFFFFFFF)
val md_theme_light_errorContainer       = RedTint
val md_theme_light_onErrorContainer     = Red
val md_theme_light_background           = Paper
val md_theme_light_onBackground         = Ink
val md_theme_light_surface              = SurfaceLt
val md_theme_light_onSurface            = Ink
val md_theme_light_surfaceVariant       = Paper
val md_theme_light_onSurfaceVariant     = Muted
val md_theme_light_outline              = Muted
val md_theme_light_outlineVariant       = Line
val md_theme_light_inverseSurface       = Ink
val md_theme_light_inverseOnSurface     = Paper
val md_theme_light_inversePrimary       = Color(0xFF5EDDD0)
val md_theme_light_surfaceTint          = Teal

// ── Dark ─────────────────────────────────────────────────────────────────────
// The mockup only specifies a light UI (the splash is the sole dark surface),
// so this is a derived companion scheme rather than a designed one.
val md_theme_dark_primary               = Color(0xFF5EDDD0)
val md_theme_dark_onPrimary             = Color(0xFF00352F)
val md_theme_dark_primaryContainer      = Color(0xFF0B4B45)
val md_theme_dark_onPrimaryContainer    = TealTint
val md_theme_dark_secondary             = Color(0xFFF0B87A)
val md_theme_dark_onSecondary           = Color(0xFF452200)
val md_theme_dark_secondaryContainer    = Color(0xFF6A3400)
val md_theme_dark_onSecondaryContainer  = AmberTint
val md_theme_dark_tertiary              = Color(0xFF7CD79B)
val md_theme_dark_onTertiary            = Color(0xFF00391C)
val md_theme_dark_tertiaryContainer     = Color(0xFF0F5129)
val md_theme_dark_onTertiaryContainer   = WhatsAppTint
val md_theme_dark_error                 = Color(0xFFFFB3C1)
val md_theme_dark_onError               = Color(0xFF65001B)
val md_theme_dark_errorContainer        = Color(0xFF8E0028)
val md_theme_dark_onErrorContainer      = RedTint
val md_theme_dark_background            = Color(0xFF111513)
val md_theme_dark_onBackground          = Color(0xFFE3E6E5)
val md_theme_dark_surface               = Color(0xFF1A201E)
val md_theme_dark_onSurface             = Color(0xFFE3E6E5)
val md_theme_dark_surfaceVariant        = Color(0xFF232A28)
val md_theme_dark_onSurfaceVariant      = Color(0xFFA9B2B0)
val md_theme_dark_outline               = Color(0xFF8B9491)
val md_theme_dark_outlineVariant        = Color(0xFF2E3634)
val md_theme_dark_inverseSurface        = Color(0xFFE3E6E5)
val md_theme_dark_inverseOnSurface      = Color(0xFF1A201E)
val md_theme_dark_inversePrimary        = Teal
val md_theme_dark_surfaceTint           = Color(0xFF5EDDD0)
