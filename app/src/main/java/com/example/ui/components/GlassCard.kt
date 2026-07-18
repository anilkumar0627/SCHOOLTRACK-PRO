package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    borderWidth: Dp = 1.dp,
    content: @Composable () -> Unit
) {
    val isLight = MaterialTheme.colorScheme.surfaceTint == MaterialTheme.colorScheme.primary // simple light/dark check or default
    val glassColor = if (isLight) {
        Color(0x80FFFFFF) // Semi-transparent white
    } else {
        Color(0x401E1E1E) // Semi-transparent dark slate
    }

    val borderBrush = if (isLight) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xBBFFFFFF),
                Color(0x33FFFFFF)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0x55FFFFFF),
                Color(0x11FFFFFF)
            )
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(glassColor)
            .border(
                border = BorderStroke(borderWidth, borderBrush),
                shape = RoundedCornerShape(cornerRadius)
            )
            .padding(16.dp)
    ) {
        content()
    }
}
