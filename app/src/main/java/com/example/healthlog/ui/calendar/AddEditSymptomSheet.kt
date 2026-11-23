package com.example.healthlog.ui.calendar

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healthlog.R
import com.example.healthlog.database.SymptomEntry
import com.example.healthlog.model.SymptomSeverity
import com.example.healthlog.model.SymptomType
import com.example.healthlog.ui.format.formatLocale
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditSymptomSheet(
    editingItem: SymptomEntry?,
    defaultDate: LocalDate = LocalDate.now(),
    defaultTime: Pair<Int, Int>? = null,
    onDismiss: () -> Unit,
    onSave: (
        symptomType: String,
        customName: String?,
        severity: Int,
        timestamp: Long,
        notes: String?
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val locale = formatLocale()
    val stateKey = "${editingItem?.id}_${defaultDate}_${defaultTime?.first}_${defaultTime?.second}"

    // Form states
    var selectedType by rememberSaveable(stateKey) {
        mutableStateOf(editingItem?.symptomType ?: SymptomType.HEADACHE.name)
    }
    var customName by rememberSaveable(stateKey) {
        mutableStateOf(editingItem?.customName.orEmpty())
    }
    var severityLevel by rememberSaveable(stateKey) {
        mutableIntStateOf(editingItem?.severity ?: 3)
    }
    var timestamp by rememberSaveable(stateKey) {
        val initialTime = defaultTime?.let { (h, m) -> LocalTime.of(h.coerceIn(0, 23), m.coerceIn(0, 59)) }
            ?: LocalTime.now()
        mutableLongStateOf(
            editingItem?.timestamp ?: defaultDate.atTime(initialTime)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        )
    }
    var notes by rememberSaveable(stateKey) {
        mutableStateOf(editingItem?.notes.orEmpty())
    }

    val isCustom = selectedType == SymptomType.CUSTOM.name
    val isCustomValid = !isCustom || customName.isNotBlank()
    val canSave = isCustomValid

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Sheet Header
            Text(
                text = stringResource(
                    if (editingItem == null) R.string.symptom_form_title_add
                    else R.string.symptom_form_title_edit
                ),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 1. Symptom Type Selector Chips
            Text(
                text = stringResource(R.string.symptom_form_type_label),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SymptomType.entries.forEach { type ->
                    val isSelected = selectedType == type.name
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedType = type.name },
                        label = { Text(text = stringResource(type.titleRes)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Custom name input if "CUSTOM" is chosen
            if (isCustom) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = customName,
                    onValueChange = { customName = it },
                    label = { Text(stringResource(R.string.symptom_form_custom_label)) },
                    placeholder = { Text(stringResource(R.string.symptom_form_custom_hint)) },
                    isError = customName.isBlank(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Severity Slider with Live Indicator
            val currentSeverity = SymptomSeverity.fromLevel(severityLevel)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.symptom_form_severity_label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = currentSeverity.lightColor.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(currentSeverity.lightColor, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${currentSeverity.level} — ${stringResource(currentSeverity.labelRes)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = currentSeverity.lightColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Slider(
                value = severityLevel.toFloat(),
                onValueChange = { severityLevel = it.toInt() },
                valueRange = 1f..5f,
                steps = 3,
                colors = SliderDefaults.colors(
                    thumbColor = currentSeverity.lightColor,
                    activeTrackColor = currentSeverity.lightColor
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Date & Time Picker Card
            Text(
                text = stringResource(R.string.symptom_form_date_time_label),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            val currentDateTime = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault())
            val formattedDate = currentDateTime.format(
                DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", locale)
            ).replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
            val formattedTime = currentDateTime.format(DateTimeFormatter.ofPattern("HH:mm"))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        // Open Date Picker followed by Time Picker
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val pickedDate = LocalDate.of(year, month + 1, dayOfMonth)
                                TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        val pickedTime = LocalTime.of(hourOfDay, minute)
                                        timestamp = ZonedDateTime.of(pickedDate, pickedTime, ZoneId.systemDefault())
                                            .toInstant()
                                            .toEpochMilli()
                                    },
                                    currentDateTime.hour,
                                    currentDateTime.minute,
                                    true
                                ).show()
                            },
                            currentDateTime.year,
                            currentDateTime.monthValue - 1,
                            currentDateTime.dayOfMonth
                        ).show()
                    },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(
                        painter = painterResource(R.drawable.ic_calendar),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 4. Notes input
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.symptom_form_notes_label)) },
                placeholder = { Text(stringResource(R.string.symptom_form_notes_hint)) },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 5. Submit Button
            Button(
                onClick = {
                    onSave(
                        selectedType,
                        if (isCustom) customName.trim() else null,
                        severityLevel,
                        timestamp,
                        notes.trim().ifBlank { null }
                    )
                },
                enabled = canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = stringResource(R.string.symptom_form_save),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
