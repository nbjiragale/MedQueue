package com.niranjan.medqueue.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.io.File

/**
 * Full-screen image viewer shown over the app rather than handing the photo to
 * an external gallery — a prescription is customer health information and
 * should not leave the app to be viewed.
 *
 * Pinch to zoom, drag to pan, double-tap to toggle fit/zoomed. Panning is
 * clamped to the scaled image's bounds so it can never be flung off-screen.
 */
@Composable
fun ImageViewerDialog(
    file: File,
    contentDescription: String,
    closeLabel: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false
        )
    ) {
        val scope = rememberCoroutineScope()
        val scale = remember { Animatable(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        var canvas by remember { mutableStateOf(IntSize.Zero) }

        /** Keeps the image's edges from travelling inside the viewport. */
        fun clamp(candidate: Offset, atScale: Float): Offset {
            if (atScale <= 1f) return Offset.Zero
            val maxX = canvas.width * (atScale - 1f) / 2f
            val maxY = canvas.height * (atScale - 1f) / 2f
            return Offset(
                candidate.x.coerceIn(-maxX, maxX),
                candidate.y.coerceIn(-maxY, maxY)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Scrim)
                .onSizeChanged { canvas = it }
        ) {
            AsyncImage(
                model = file,
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val next = (scale.value * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
                            scope.launch { scale.snapTo(next) }
                            offset = clamp(offset + pan, next)
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                val zoomedIn = scale.value > 1f + ZOOM_EPSILON
                                val target = if (zoomedIn) 1f else DOUBLE_TAP_SCALE
                                offset = Offset.Zero
                                scope.launch { scale.animateTo(target) }
                            }
                        )
                    }
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                        translationX = offset.x
                        translationY = offset.y
                    }
            )

            IconButton(
                onClick = onDismiss,
                colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(12.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.16f))
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = closeLabel,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private val Scrim = Color(0xFF0B0B0C)

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 5f
private const val DOUBLE_TAP_SCALE = 2.5f

/** Float comparison slack, so a pinch that lands near 1.0 still reads as "fit". */
private const val ZOOM_EPSILON = 0.01f
