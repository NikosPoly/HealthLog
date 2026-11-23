package com.example.healthlog.ui.calendar.timeline

import com.example.healthlog.database.Measurement
import com.example.healthlog.database.SymptomEntry
import com.example.healthlog.model.ScheduledDoseItem
import java.time.LocalDate

enum class TimelineFilter {
    ALL,
    VITALS,
    MEDICATIONS,
    SYMPTOMS
}

sealed class TimelineEvent {
    abstract val eventId: String
    abstract val hour: Int
    abstract val minute: Int
    abstract val timeString: String

    data class Vital(
        val measurement: Measurement,
        override val hour: Int,
        override val minute: Int,
        override val timeString: String
    ) : TimelineEvent() {
        override val eventId: String get() = "vital_${measurement.id}"
    }

    data class Medication(
        val doseItem: ScheduledDoseItem,
        override val hour: Int,
        override val minute: Int,
        override val timeString: String
    ) : TimelineEvent() {
        override val eventId: String get() = "med_${doseItem.medicationId}_${doseItem.reminderId ?: -1}_${doseItem.doseLogId ?: -1}_${doseItem.scheduledTime}"
    }

    data class Symptom(
        val symptom: SymptomEntry,
        override val hour: Int,
        override val minute: Int,
        override val timeString: String
    ) : TimelineEvent() {
        override val eventId: String get() = "symptom_${symptom.id}"
    }
}

data class TimelineHourSlot(
    val hour: Int,
    val timeLabel: String,
    val events: List<TimelineEvent> = emptyList()
) {
    val isPopulated: Boolean get() = events.isNotEmpty()
}

data class DayTimelineUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val activeFilter: TimelineFilter = TimelineFilter.ALL,
    val hourSlots: List<TimelineHourSlot> = emptyList(),
    val totalEventCount: Int = 0,
    val vitalsCount: Int = 0,
    val medsCount: Int = 0,
    val symptomsCount: Int = 0,
    val editingSymptom: SymptomEntry? = null,
    val showAddEditSymptomSheet: Boolean = false,
    val symptomToDelete: SymptomEntry? = null
)
