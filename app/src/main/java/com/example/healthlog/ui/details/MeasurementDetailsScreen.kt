package com.example.healthlog.ui.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.healthlog.R
import com.example.healthlog.database.Measurement
import com.example.healthlog.model.BodyPosition
import com.example.healthlog.model.MealContext
import com.example.healthlog.model.MeasurementArm
import com.example.healthlog.model.MeasurementType
import com.example.healthlog.model.arrhythmiaIcon
import com.example.healthlog.model.arrhythmiaLabel
import com.example.healthlog.ui.format.decimalSeparator
import com.example.healthlog.ui.format.formatMeasurementValue
import com.example.healthlog.ui.format.formatTimestamp
import com.example.healthlog.ui.format.rememberDateTimeFormatter
import com.example.healthlog.ui.format.timeAgo
import com.example.healthlog.ui.format.timeAgoText
import com.example.healthlog.ui.history.DeleteConfirmationDialog
import com.example.healthlog.ui.theme.HealthLogTheme
import com.example.healthlog.ui.theme.TypeTheme
import com.example.healthlog.ui.theme.icon
import java.time.ZonedDateTime

// Full details screen for a single measurement entry.
@Composable
fun MeasurementDetailsScreen(
    viewModel: MeasurementDetailsViewModel,
    onBack: () -> Unit,
) {
    val measurement by viewModel.measurement.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    MeasurementDetailsContent(
        measurement = measurement,
        onBack = onBack,
        onDelete = { showDeleteDialog = true }
    )

    if (showDeleteDialog && (measurement != null)) {
        val current = measurement!!
        val details = stringResource(
            R.string.delete_dialog_details,
            stringResource(current.type.label),
            formatMeasurementValue(current, decimalSeparator())
        )

        DeleteConfirmationDialog(
            details = details,
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteMeasurement(current, onDeleted = onBack)
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

// Pure presentation of the measurement details, decoupled from the ViewModel for preview and testing.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasurementDetailsContent(
    measurement: Measurement?,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    guidelinesSlot: (@Composable () -> Unit)? = null
) {
    val content = @Composable {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (measurement != null) {
                                Icon(
                                    painter = painterResource(measurement.type.icon),
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp),
                                    tint = Color.Unspecified
                                )
                                Text(
                                    text = stringResource(measurement.type.label),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.details_title),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_back),
                                contentDescription = stringResource(R.string.action_back),
                                tint = Color.Unspecified
                            )
                        }
                    },
                    actions = {
                        if (measurement != null) {
                            IconButton(onClick = onDelete) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_delete),
                                    contentDescription = stringResource(R.string.action_delete),
                                    tint = Color.Unspecified
                                )
                            }
                        }
                    }
                )
            }
        ) { padding ->
            if (measurement == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.details_not_found),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val separator = decimalSeparator()
                val dateFormatter = rememberDateTimeFormatter()
                val valueText = formatMeasurementValue(measurement, separator)
                val recorded = timeAgoText(timeAgo(measurement.timestamp, ZonedDateTime.now())) +
                    " · " + formatTimestamp(measurement.timestamp, dateFormatter)

                val arm = MeasurementArm.fromNameOrNull(measurement.arm)
                val position = BodyPosition.fromNameOrNull(measurement.bodyPosition)
                val meal = MealContext.fromNameOrNull(measurement.mealContext)
                val arrhythmia = measurement.hasArrhythmia
                val hasConditions = arm != null || position != null || meal != null || arrhythmia != null

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Compact Value & Timestamp Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = valueText,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = recorded,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Clinical Context / Condition Chips (without redundant headers)
                    if (hasConditions) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (arm != null) {
                                ConditionTag(
                                    iconRes = arm.iconRes,
                                    label = stringResource(arm.label)
                                )
                            }

                            if (position != null) {
                                ConditionTag(
                                    iconRes = position.iconRes,
                                    label = stringResource(position.label)
                                )
                            }

                            if (meal != null) {
                                ConditionTag(
                                    iconRes = meal.iconRes,
                                    label = stringResource(meal.label)
                                )
                            }

                            if (arrhythmia != null) {
                                ConditionTag(
                                    iconRes = arrhythmiaIcon(arrhythmia),
                                    label = stringResource(R.string.section_arrhythmia) + ": " +
                                        stringResource(arrhythmiaLabel(arrhythmia))
                                )
                            }
                        }
                    }

                    // Optional Slot for future guideline tables / statistics
                    guidelinesSlot?.invoke()
                }
            }
        }
    }

    if (measurement != null) {
        TypeTheme(measurement.type, content = content)
    } else {
        HealthLogTheme(content = content)
    }
}

// A single tag displaying a clinical condition icon and its label.
@Composable
private fun ConditionTag(
    iconRes: Int,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = Color.Unspecified
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// Previews for blood pressure with full context conditions, and weight without extra context.
@Preview(showBackground = true)
@Composable
private fun MeasurementDetailsBloodPressurePreview() {
    HealthLogTheme {
        MeasurementDetailsContent(
            measurement = Measurement(
                id = 1L,
                type = MeasurementType.BLOOD_PRESSURE,
                value = 125.0,
                secondaryValue = 82.0,
                tertiaryValue = 72.0,
                hasArrhythmia = false,
                arm = MeasurementArm.LEFT.name,
                bodyPosition = BodyPosition.SITTING.name,
                timestamp = System.currentTimeMillis() - 3600_000L
            ),
            onBack = {},
            onDelete = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MeasurementDetailsBloodSugarPreview() {
    HealthLogTheme {
        MeasurementDetailsContent(
            measurement = Measurement(
                id = 2L,
                type = MeasurementType.BLOOD_SUGAR,
                value = 95.0,
                mealContext = MealContext.BEFORE_MEAL.name,
                timestamp = System.currentTimeMillis()
            ),
            onBack = {},
            onDelete = {}
        )
    }
}
