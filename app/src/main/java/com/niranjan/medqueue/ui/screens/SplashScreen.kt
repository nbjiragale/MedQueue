package com.niranjan.medqueue.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.niranjan.medqueue.R
import com.niranjan.medqueue.ui.theme.TealDark
import kotlinx.coroutines.delay

// ══════════════════════════════════════════════════════════════════════════════
// SPLASH — flat teal-dark field, rounded-square mark, three-dot loader
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun SplashScreen(onFinished: () -> Unit) {

    var start by remember { mutableStateOf(false) }

    val markScale by animateFloatAsState(
        targetValue = if (start) 1f else 0.7f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "markScale"
    )
    val markAlpha by animateFloatAsState(
        targetValue = if (start) 1f else 0f,
        animationSpec = tween(400),
        label = "markAlpha"
    )
    val textAlpha by animateFloatAsState(
        targetValue = if (start) 1f else 0f,
        animationSpec = tween(400, delayMillis = 220),
        label = "textAlpha"
    )

    // Three dots cycling at even offsets, matching the mockup's loader.
    val cycle = rememberInfiniteTransition(label = "dots")
    val phase by cycle.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(1050, easing = LinearEasing)),
        label = "dotPhase"
    )

    LaunchedEffect(Unit) {
        start = true
        delay(1200)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TealDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Rounded-square tile holding an outlined square mark
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .scale(markScale)
                    .alpha(markAlpha)
                    .size(72.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White.copy(alpha = 0.14f))
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .border(3.dp, Color.White, RoundedCornerShape(8.dp))
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                modifier = Modifier.alpha(textAlpha)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.65f),
                modifier = Modifier.alpha(textAlpha)
            )

            Spacer(Modifier.height(28.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.alpha(textAlpha)
            ) {
                repeat(3) { index ->
                    // Distance from the travelling phase to this dot's slot,
                    // wrapped so dot 0 lights up again after dot 2.
                    val distance = ((phase - index + 3f) % 3f).let { minOf(it, 3f - it) }
                    val intensity = (1f - distance).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f + 0.8f * intensity))
                    )
                }
            }
        }
    }
}
