package com.niranjan.medqueue.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niranjan.medqueue.ui.theme.HeaderGradientEnd
import com.niranjan.medqueue.ui.theme.HeaderGradientMid
import com.niranjan.medqueue.ui.theme.HeaderGradientStart
import kotlinx.coroutines.delay

// ══════════════════════════════════════════════════════════════════════════════
// ── SPLASH SCREEN
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun SplashScreen(onFinished: () -> Unit) {

    // ── Animation triggers ───────────────────────────────────────────────
    var startAnimation by remember { mutableStateOf(false) }

    // Logo: scale up with overshoot
    val logoScale by animateFloatAsState(
        targetValue   = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label         = "logoScale"
    )

    // Logo: fade in
    val logoAlpha by animateFloatAsState(
        targetValue   = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label         = "logoAlpha"
    )

    // App name: slide up + fade in (delayed)
    val textAlpha by animateFloatAsState(
        targetValue   = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 500, delayMillis = 350),
        label         = "textAlpha"
    )
    val textOffset by animateFloatAsState(
        targetValue   = if (startAnimation) 0f else 30f,
        animationSpec = tween(durationMillis = 500, delayMillis = 350, easing = FastOutSlowInEasing),
        label         = "textOffset"
    )

    // Subtitle: fade in (more delayed)
    val subtitleAlpha by animateFloatAsState(
        targetValue   = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 500, delayMillis = 600),
        label         = "subtitleAlpha"
    )

    // Pill loader dots — infinite bouncing
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    val dot1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes { durationMillis = 800; 1f at 200; 0f at 400 },
            repeatMode = RepeatMode.Restart
        ), label = "dot1"
    )
    val dot2 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes { durationMillis = 800; 1f at 400; 0f at 600 },
            repeatMode = RepeatMode.Restart
        ), label = "dot2"
    )
    val dot3 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes { durationMillis = 800; 1f at 600; 0f at 800 },
            repeatMode = RepeatMode.Restart
        ), label = "dot3"
    )

    // Start animation on first composition, navigate away after delay
    LaunchedEffect(Unit) {
        startAnimation = true
        delay(1200)  // total splash duration
        onFinished()
    }

    // ── UI ────────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(HeaderGradientStart, HeaderGradientMid, HeaderGradientEnd)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ── Animated "M" logo ────────────────────────────────────────
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .scale(logoScale)
                    .alpha(logoAlpha)
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f))
                ) {
                    Text(
                        text       = "M",
                        fontSize   = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── App name ─────────────────────────────────────────────────
            Text(
                text  = "MedQueue",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color    = Color.White,
                modifier = Modifier
                    .alpha(textAlpha)
                    .offset { androidx.compose.ui.unit.IntOffset(0, textOffset.dp.roundToPx()) }
            )

            Spacer(Modifier.height(6.dp))

            // ── Subtitle ─────────────────────────────────────────────────
            Text(
                text  = "Medicine requests, simplified",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight    = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                ),
                color    = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.alpha(subtitleAlpha)
            )

            Spacer(Modifier.height(48.dp))

            // ── Loading dots ─────────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically,
                modifier              = Modifier.alpha(subtitleAlpha)
            ) {
                listOf(dot1, dot2, dot3).forEach { anim ->
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .scale(0.6f + 0.4f * anim)
                            .alpha(0.4f + 0.6f * anim)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }
        }

        // ── Bottom tagline ───────────────────────────────────────────────
        Text(
            text     = "💊  Your pharmacy assistant",
            style    = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
            color    = Color.White.copy(alpha = 0.4f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp)
                .alpha(subtitleAlpha)
        )
    }
}



