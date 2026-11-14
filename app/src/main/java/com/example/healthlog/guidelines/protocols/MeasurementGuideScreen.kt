package com.example.healthlog.guidelines.protocols

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.healthlog.R
import com.example.healthlog.model.MeasurementType
import com.example.healthlog.ui.theme.HealthLogTheme
import com.example.healthlog.ui.theme.icon

// Dedicated screen presenting clinical measurement instructions and protocols.
// When showTabs is false (default), it stays laser-focused on the specific measurement type.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasurementGuideScreen(
    type: MeasurementType,
    showTabs: Boolean = false,
    onBack: () -> Unit,
) {
    var activeType by rememberSaveable { mutableStateOf(type) }
    val displayType = if (showTabs) activeType else type
    val protocol = getMeasurementProtocol(displayType)
    val allTypes = MeasurementType.entries
    val selectedIndex = allTypes.indexOf(displayType)

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                painter = painterResource(displayType.icon),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = Color.Unspecified,
                            )
                            Text(
                                text = stringResource(displayType.label),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_back),
                                contentDescription = stringResource(R.string.action_back),
                                tint = Color.Unspecified,
                            )
                        }
                    },
                )

                // Optional type tabs when browsing all metrics
                if (showTabs) {
                    PrimaryScrollableTabRow(
                        selectedTabIndex = selectedIndex,
                        edgePadding = 8.dp,
                    ) {
                        allTypes.forEachIndexed { index, itemType ->
                            val selected = index == selectedIndex
                            Tab(
                                selected = selected,
                                onClick = { activeType = itemType },
                                selectedContentColor = MaterialTheme.colorScheme.primary,
                                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                text = {
                                    Text(
                                        text = stringResource(itemType.label),
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    )
                                },
                            )
                        }
                    }
                }
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MeasurementGuideContent(protocol = protocol)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MeasurementGuideScreenFocusedPreview() {
    HealthLogTheme {
        MeasurementGuideScreen(
            type = MeasurementType.BLOOD_PRESSURE,
            showTabs = false,
            onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MeasurementGuideScreenWithTabsPreview() {
    HealthLogTheme {
        MeasurementGuideScreen(
            type = MeasurementType.BLOOD_SUGAR,
            showTabs = true,
            onBack = {},
        )
    }
}
