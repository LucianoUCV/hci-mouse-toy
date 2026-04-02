package com.example.mousetoyapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.mousetoyapp.ui.theme.AccentCyan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun HCIJoystick(enabled: Boolean, onMove: (Float, Float) -> Unit) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val baseSize = 220.dp
    val thumbSize = 80.dp
    val maxTravelPx = with(LocalDensity.current) { ((baseSize - thumbSize) / 2).toPx() }

    Box(
        modifier = Modifier
            .size(baseSize)
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF222228), Color.Transparent),
                    radius = 300f
                ),
                CircleShape
            )
            .border(2.dp, Color.White.copy(alpha = if (enabled) 0.15f else 0.05f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .size(thumbSize)
                .shadow(if (enabled) 15.dp else 0.dp, CircleShape, ambientColor = AccentCyan, spotColor = AccentCyan)
                .background(
                    if (enabled) {
                        Brush.radialGradient(
                            listOf(Color(0xFF88FFFF), AccentCyan, Color(0xFF0099AA)),
                            center = Offset(30f, 30f),
                            radius = 150f
                        )
                    } else {
                        Brush.radialGradient(
                            listOf(Color.Gray, Color.DarkGray),
                            center = Offset(30f, 30f),
                            radius = 150f
                        )
                    },
                    CircleShape
                )
                .border(1.dp, Color.White.copy(0.3f), CircleShape)
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectDragGestures(
                        onDragEnd = {
                            offsetX = 0f
                            offsetY = 0f
                            onMove(0f, 0f)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val nX = offsetX + dragAmount.x
                            val nY = offsetY + dragAmount.y
                            val dist = sqrt(nX * nX + nY * nY)

                            if (dist <= maxTravelPx) {
                                offsetX = nX
                                offsetY = nY
                            } else {
                                val angle = atan2(nY, nX)
                                offsetX = cos(angle) * maxTravelPx
                                offsetY = sin(angle) * maxTravelPx
                            }

                            onMove((offsetX / maxTravelPx) * 100f, (offsetY / maxTravelPx) * 100f)
                        }
                    )
                }
        )
    }
}

