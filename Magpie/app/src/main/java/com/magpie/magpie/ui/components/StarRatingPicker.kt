package com.magpie.magpie.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun StarRatingPicker(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    starSize: Dp = 26.dp,
    activeColor: Color = MaterialTheme.colorScheme.primary
) {
    val gap = 2.dp
    val totalWidth = starSize * 5 + gap * 4
    BoxWithConstraints(
        modifier = modifier
            .width(totalWidth)
            .height(starSize + 8.dp)
    ) {
        StarRatingRow(
            rating = value.toDouble().coerceIn(0.0, 5.0),
            starSize = starSize,
            activeColor = activeColor,
            horizontalArrangement = Arrangement.spacedBy(gap),
            modifier = Modifier.align(Alignment.CenterStart)
        )
        Row(
            Modifier
                .matchParentSize()
                .align(Alignment.CenterStart)
        ) {
            repeat(10) { idx ->
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onValueChange(0.5f * (idx + 1)) }
                )
            }
        }
    }
}
