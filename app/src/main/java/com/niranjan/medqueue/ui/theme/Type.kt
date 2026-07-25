package com.niranjan.medqueue.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.niranjan.medqueue.R

/**
 * Manrope, bundled as static weights (OFL-1.1) rather than fetched through the
 * downloadable-fonts provider — the app has to work on a shop counter phone
 * with no Play Services and no connection.
 */
val Manrope = FontFamily(
    Font(R.font.manrope_regular,   FontWeight.Normal),
    Font(R.font.manrope_medium,    FontWeight.Medium),
    Font(R.font.manrope_semibold,  FontWeight.SemiBold),
    Font(R.font.manrope_bold,      FontWeight.Bold),
    Font(R.font.manrope_extrabold, FontWeight.ExtraBold)
)

/**
 * Type scale mapped from the mockup. The design leans on ExtraBold for screen
 * titles and a tight negative tracking, which is what gives it its character —
 * both are preserved here rather than rounded to the M3 defaults.
 */
val AppTypography = Typography(
    // Marketing-scale heading (splash wordmark)
    headlineLarge = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.26).sp
    ),
    // Onboarding title — 24/800
    headlineMedium = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.24).sp
    ),
    // Screen titles — 22/800
    titleLarge = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.22).sp
    ),
    // Detail hero phone number — 19/800
    titleMedium = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 19.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.19).sp
    ),
    // Card headline / list primary line — 15/700
    titleSmall = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    // Field values — 15/500
    bodyLarge = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    // Secondary line — 13/400
    bodyMedium = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.sp
    ),
    // Fine print — 12/400
    bodySmall = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.sp
    ),
    // Buttons — 15/700
    labelLarge = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    // Field labels — 12/600
    labelMedium = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    ),
    // Badges & nav labels — 11/700
    labelSmall = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.sp
    )
)

/**
 * The mockup's section eyebrow: 11px / 800 / uppercase with 0.08em tracking,
 * always in teal. Used at the top of every card.
 */
val SectionEyebrow = TextStyle(
    fontFamily = Manrope,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 11.sp,
    lineHeight = 14.sp,
    letterSpacing = 0.88.sp
)
