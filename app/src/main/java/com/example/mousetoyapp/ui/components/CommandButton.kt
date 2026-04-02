package com.example.mousetoyapp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mousetoyapp.ui.theme.AccentCyan
import com.example.mousetoyapp.ui.theme.SurfaceCard

@Composable
fun CommandButton(
    label: String,
    isActive: Boolean,
    isConnected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && isConnected) 0.92f else 1f,
        animationSpec = tween(100),
        label = ""
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActive) AccentCyan.copy(0.15f) else SurfaceCard)
            .border(1.dp, if (isActive) AccentCyan else Color.Transparent, RoundedCornerShape(12.dp))
            .clickable(interactionSource = interactionSource, indication = null, enabled = isConnected) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (isConnected) (if (isActive) AccentCyan else Color.White) else Color.Gray,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

