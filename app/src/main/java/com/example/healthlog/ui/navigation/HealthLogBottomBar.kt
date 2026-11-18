package com.example.healthlog.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.healthlog.R

enum class NavigationTab(
    @StringRes val titleRes: Int,
    @DrawableRes val iconRes: Int,
    val testTag: String,
    val activeColor: Color,
    val indicatorColor: Color,
    val borderColor: Color,
) {
    VITALS(
        titleRes = R.string.nav_vitals,
        iconRes = R.drawable.ic_nav_vitals,
        testTag = "tab_vitals",
        activeColor = Color(0xFFD32F2F),
        indicatorColor = Color(0xFFFFEBEE),
        borderColor = Color(0xFFFFCDD2),
    ),
    MEDICATIONS(
        titleRes = R.string.nav_medications,
        iconRes = R.drawable.ic_nav_medications,
        testTag = "tab_medications",
        activeColor = Color(0xFF00838F),
        indicatorColor = Color(0xFFE0F7FA),
        borderColor = Color(0xFFB2EBF2),
    ),
    CALENDAR(
        titleRes = R.string.nav_calendar,
        iconRes = R.drawable.ic_nav_calendar,
        testTag = "tab_calendar",
        activeColor = Color(0xFF6A1B9A),
        indicatorColor = Color(0xFFEDE7F6),
        borderColor = Color(0xFFD1C4E9),
    ),
}

@Composable
fun HealthLogBottomBar(
    currentTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(NavigationBarDefaults.windowInsets)
                .height(82.dp)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavigationTab.entries.forEach { tab ->
                val isSelected = currentTab == tab

                val backgroundColor by animateColorAsState(
                    targetValue = if (isSelected) tab.indicatorColor else Color.Transparent,
                    label = "tab_bg_color",
                )

                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.08f else 0.95f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                    label = "tab_scale",
                )

                val alpha by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0.65f,
                    label = "tab_alpha",
                )

                val shape = RoundedCornerShape(16.dp)

                Surface(
                    shape = shape,
                    color = backgroundColor,
                    border = if (isSelected) BorderStroke(1.5.dp, tab.borderColor) else null,
                    modifier = Modifier
                        .weight(1f)
                        .height(70.dp)
                        .clip(shape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = tab.activeColor),
                        ) {
                            onTabSelected(tab)
                        }
                        .testTag(tab.testTag),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp),
                    ) {
                        Icon(
                            painter = painterResource(tab.iconRes),
                            contentDescription = stringResource(tab.titleRes),
                            tint = Color.Unspecified,
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    this.alpha = alpha
                                }
                                .size(28.dp),
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = stringResource(tab.titleRes),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) tab.activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
