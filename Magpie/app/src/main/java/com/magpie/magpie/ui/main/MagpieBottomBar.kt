package com.magpie.magpie.ui.main

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.magpie.magpie.R
import com.magpie.magpie.navigation.MainTab
import com.magpie.magpie.ui.theme.MagpieBackground
import com.magpie.magpie.ui.theme.MagpiePrimaryDark

private val BarColor = Color(0xFF1A1C1E)
private val BarContent = Color(0xFFE8E8E8)
private val NotchDepthDp = 22.dp
private val NotchHalfWidthDp = 52.dp
private val SelectedHaloDiameterDp = 56.dp
private val SelectedTealDiameterDp = 44.dp
private val SelectedIconLiftDp = 14.dp

@Composable
fun MagpieBottomBar(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val selectedIndex = MainTab.entries.indexOf(selectedTab).coerceAtLeast(0)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
    ) {
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }
        val notchDepthPx = with(density) { NotchDepthDp.toPx() }
        val notchHalfWidthPx = with(density) { NotchHalfWidthDp.toPx() }
        val centerFraction = (selectedIndex + 0.5f) / MainTab.entries.size
        val cx = wPx * centerFraction

        Canvas(Modifier.fillMaxSize()) {
            val leftX = (cx - notchHalfWidthPx).coerceAtLeast(0f)
            val rightX = (cx + notchHalfWidthPx).coerceAtMost(wPx)
            val h = notchDepthPx.coerceAtLeast(1f)

            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(leftX, 0f)
                quadraticBezierTo(cx, 2f * h, rightX, 0f)
                lineTo(wPx, 0f)
                lineTo(wPx, hPx)
                lineTo(0f, hPx)
                close()
            }
            drawPath(path, BarColor)
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 10.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MainTab.entries.forEach { tab ->
                val selected = tab == selectedTab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Tab
                        ) { onTabSelected(tab) }
                        .semantics {
                            this.selected = selected
                            role = Role.Tab
                        },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    if (selected) {
                        ElevatedSelectedTabItem(tab = tab)
                    } else {
                        DefaultTabItem(tab = tab)
                    }
                }
            }
        }
    }
}

@Composable
private fun ElevatedSelectedTabItem(tab: MainTab) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset(y = -SelectedIconLiftDp)
                .size(SelectedHaloDiameterDp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(MagpieBackground)
            )
            Box(
                modifier = Modifier
                    .size(SelectedTealDiameterDp)
                    .clip(CircleShape)
                    .background(MagpiePrimaryDark),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tab.filledIcon(),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Text(
            text = stringResource(tab.labelRes),
            style = MaterialTheme.typography.labelSmall,
            color = BarContent
        )
    }
}

@Composable
private fun DefaultTabItem(tab: MainTab) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier
            .fillMaxHeight()
            .padding(bottom = 2.dp)
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            imageVector = tab.outlinedIcon(),
            contentDescription = null,
            tint = BarContent,
            modifier = Modifier.size(26.dp)
        )
        Text(
            text = stringResource(tab.labelRes),
            style = MaterialTheme.typography.labelSmall,
            color = BarContent,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

private fun MainTab.filledIcon(): ImageVector = when (this) {
    MainTab.Home -> Icons.Filled.Home
    MainTab.Search -> Icons.Filled.Search
    MainTab.Rate -> Icons.Filled.Add
    MainTab.Profile -> Icons.Filled.Person
    MainTab.More -> Icons.Filled.Menu
}

private fun MainTab.outlinedIcon(): ImageVector = when (this) {
    MainTab.Home -> Icons.Outlined.Home
    MainTab.Search -> Icons.Outlined.Search
    MainTab.Rate -> Icons.Outlined.Add
    MainTab.Profile -> Icons.Outlined.AccountCircle
    MainTab.More -> Icons.Outlined.Menu
}

private val MainTab.labelRes: Int
    get() = when (this) {
        MainTab.Home -> R.string.nav_home
        MainTab.Search -> R.string.nav_search
        MainTab.Rate -> R.string.nav_rate
        MainTab.Profile -> R.string.nav_profile
        MainTab.More -> R.string.nav_more
    }
