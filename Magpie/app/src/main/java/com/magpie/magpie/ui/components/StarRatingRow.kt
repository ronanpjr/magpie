package com.magpie.magpie.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun StarRatingRow(
    rating: Double,
    modifier: Modifier = Modifier,
    maxStars: Int = 5,
    starSize: Dp = 18.dp,
    activeColor: Color = MaterialTheme.colorScheme.tertiary,
    inactiveColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(2.dp)
) {
    Row(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        repeat(maxStars) { index ->
            val starNumber = index + 1
            val filled = rating >= starNumber - 0.25
            val half = !filled && rating >= starNumber - 0.75
            val icon = when {
                filled -> Icons.Filled.Star
                half -> Icons.Filled.StarHalf
                else -> Icons.Outlined.StarOutline
            }
            val tint = when {
                filled || half -> activeColor
                else -> inactiveColor
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(starSize),
                tint = tint
            )
        }
    }
}
