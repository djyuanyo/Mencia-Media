package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

/**
 * Modifier that adds Google TV / Android TV remote control D-Pad focus indicators.
 * When the element receives focus via remote control arrows:
 * - Scales up smoothly (tactile TV feedback)
 * - Highlights with a vibrant high-contrast border
 * - Raises z-index and shadow so it stands out prominently above adjacent items
 */
fun Modifier.tvFocusable(
    shape: Shape,
    focusedBorderColor: Color = Color(0xFF00A8E1),
    focusedBorderWidth: Dp = 3.dp,
    focusedScale: Float = 1.06f,
    interactionSource: MutableInteractionSource? = null
): Modifier = composed {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isFocused by source.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) focusedScale else 1.0f,
        animationSpec = tween(durationMillis = 180),
        label = "tv_focus_scale"
    )

    this
        .zIndex(if (isFocused) 10f else 1f)
        .scale(scale)
        .shadow(
            elevation = if (isFocused) 12.dp else 0.dp,
            shape = shape,
            ambientColor = focusedBorderColor.copy(alpha = 0.5f),
            spotColor = focusedBorderColor
        )
        .border(
            width = if (isFocused) focusedBorderWidth else 0.dp,
            color = if (isFocused) focusedBorderColor else Color.Transparent,
            shape = shape
        )
        .focusable(interactionSource = source)
}
