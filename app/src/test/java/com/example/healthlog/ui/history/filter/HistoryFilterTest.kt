package com.example.healthlog.ui.history.filter

import com.example.healthlog.R
import com.example.healthlog.database.Measurement
import com.example.healthlog.guidelines.BloodGlucoseCategory
import com.example.healthlog.guidelines.BloodPressureStage
import com.example.healthlog.guidelines.BmiCategory
import com.example.healthlog.guidelines.PulseZone
import com.example.healthlog.model.MealContext
import com.example.healthlog.model.MeasurementType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryFilterTest {

    @Test
    fun filterMeasurements_whenNoFilterActive_returnsAllMeasurements() {
        val list = listOf(
            Measurement(id = 1, type = MeasurementType.PULSE, value = 55.0),
            Measurement(id = 2, type = MeasurementType.PULSE, value = 75.0),
            Measurement(id = 3, type = MeasurementType.PULSE, value = 110.0),
        )

        val result = filterMeasurements(list, HistoryFilterState(stage = null))
        assertEquals(list, result)
        assertFalse(HistoryFilterState().isActive)
    }

    @Test
    fun filterMeasurements_pulse_filtersByZone() {
        val bradycardia = Measurement(id = 1, type = MeasurementType.PULSE, value = 55.0)
        val normal = Measurement(id = 2, type = MeasurementType.PULSE, value = 75.0)
        val tachycardia = Measurement(id = 3, type = MeasurementType.PULSE, value = 110.0)
        val measurements = listOf(bradycardia, normal, tachycardia)

        val bradycardiaFiltered = filterMeasurements(
            measurements,
            HistoryFilterState(stage = ClinicalStageFilter.Pulse(PulseZone.BRADYCARDIA)),
        )
        assertEquals(listOf(bradycardia), bradycardiaFiltered)

        val normalFiltered = filterMeasurements(
            measurements,
            HistoryFilterState(stage = ClinicalStageFilter.Pulse(PulseZone.NORMAL)),
        )
        assertEquals(listOf(normal), normalFiltered)

        val tachycardiaFiltered = filterMeasurements(
            measurements,
            HistoryFilterState(stage = ClinicalStageFilter.Pulse(PulseZone.TACHYCARDIA)),
        )
        assertEquals(listOf(tachycardia), tachycardiaFiltered)
    }

    @Test
    fun filterMeasurements_bloodPressure_filtersByStage() {
        val normal = Measurement(id = 1, type = MeasurementType.BLOOD_PRESSURE, value = 120.0, secondaryValue = 80.0)
        val stage1 = Measurement(id = 2, type = MeasurementType.BLOOD_PRESSURE, value = 145.0, secondaryValue = 92.0)
        val crisis = Measurement(id = 3, type = MeasurementType.BLOOD_PRESSURE, value = 185.0, secondaryValue = 125.0)
        val measurements = listOf(normal, stage1, crisis)

        val stage1Filtered = filterMeasurements(
            measurements,
            HistoryFilterState(stage = ClinicalStageFilter.BloodPressure(BloodPressureStage.GRADE_1_HYPERTENSION)),
        )
        assertEquals(listOf(stage1), stage1Filtered)

        val crisisFiltered = filterMeasurements(
            measurements,
            HistoryFilterState(stage = ClinicalStageFilter.BloodPressure(BloodPressureStage.HYPERTENSIVE_CRISIS)),
        )
        assertEquals(listOf(crisis), crisisFiltered)
    }

    @Test
    fun filterMeasurements_bloodSugar_filtersByCategory() {
        val hypo = Measurement(id = 1, type = MeasurementType.BLOOD_SUGAR, value = 62.0)
        val normalFasting = Measurement(
            id = 2,
            type = MeasurementType.BLOOD_SUGAR,
            value = 85.0,
            mealContext = MealContext.BEFORE_MEAL.name,
        )
        val prediabetesPostMeal = Measurement(
            id = 3,
            type = MeasurementType.BLOOD_SUGAR,
            value = 150.0,
            mealContext = MealContext.AFTER_MEAL_2H.name,
        )
        val diabetesFasting = Measurement(
            id = 4,
            type = MeasurementType.BLOOD_SUGAR,
            value = 135.0,
            mealContext = MealContext.BEFORE_MEAL.name,
        )
        val measurements = listOf(hypo, normalFasting, prediabetesPostMeal, diabetesFasting)

        val prediabetesFiltered = filterMeasurements(
            measurements,
            HistoryFilterState(stage = ClinicalStageFilter.Glucose(BloodGlucoseCategory.PREDIABETES)),
        )
        assertEquals(listOf(prediabetesPostMeal), prediabetesFiltered)

        val diabetesFiltered = filterMeasurements(
            measurements,
            HistoryFilterState(stage = ClinicalStageFilter.Glucose(BloodGlucoseCategory.DIABETES)),
        )
        assertEquals(listOf(diabetesFasting), diabetesFiltered)
    }

    @Test
    fun filterMeasurements_weight_filtersByBmiCategoryWhenHeightProvided() {
        // Height = 180cm (1.8m) -> Normal weight range: 18.5 * 1.8^2 = ~60kg to 24.9 * 1.8^2 = ~80.7kg
        val underweight = Measurement(id = 1, type = MeasurementType.WEIGHT, value = 55.0)
        val normal = Measurement(id = 2, type = MeasurementType.WEIGHT, value = 72.0)
        val overweight = Measurement(id = 3, type = MeasurementType.WEIGHT, value = 88.0)
        val obese = Measurement(id = 4, type = MeasurementType.WEIGHT, value = 105.0)
        val measurements = listOf(underweight, normal, overweight, obese)

        val normalFiltered = filterMeasurements(
            measurements = measurements,
            filter = HistoryFilterState(stage = ClinicalStageFilter.Weight(BmiCategory.NORMAL)),
            latestHeightCm = 180.0,
        )
        assertEquals(listOf(normal), normalFiltered)

        val overweightFiltered = filterMeasurements(
            measurements = measurements,
            filter = HistoryFilterState(stage = ClinicalStageFilter.Weight(BmiCategory.OVERWEIGHT)),
            latestHeightCm = 180.0,
        )
        assertEquals(listOf(overweight), overweightFiltered)

        // Without height, weight cannot be classified into BMI stages -> empty list
        val withoutHeightFiltered = filterMeasurements(
            measurements = measurements,
            filter = HistoryFilterState(stage = ClinicalStageFilter.Weight(BmiCategory.NORMAL)),
            latestHeightCm = null,
        )
        assertTrue(withoutHeightFiltered.isEmpty())
    }

    @Test
    fun availableStagesForType_returnsExpectedStagesForEachType() {
        assertEquals(6, availableStagesForType(MeasurementType.BLOOD_PRESSURE).size)
        assertEquals(4, availableStagesForType(MeasurementType.BLOOD_SUGAR).size)
        assertEquals(3, availableStagesForType(MeasurementType.PULSE).size)
        assertEquals(4, availableStagesForType(MeasurementType.WEIGHT).size)
        assertTrue(availableStagesForType(MeasurementType.HEIGHT).isEmpty())
    }

    @Test
    fun labelRes_mapsToExpectedStringResource() {
        assertEquals(R.string.bp_stage_grade_1, ClinicalStageFilter.BloodPressure(BloodPressureStage.GRADE_1_HYPERTENSION).labelRes)
        assertEquals(R.string.pulse_zone_bradycardia, ClinicalStageFilter.Pulse(PulseZone.BRADYCARDIA).labelRes)
        assertEquals(R.string.glucose_category_prediabetes, ClinicalStageFilter.Glucose(BloodGlucoseCategory.PREDIABETES).labelRes)
        assertEquals(R.string.bmi_category_overweight, ClinicalStageFilter.Weight(BmiCategory.OVERWEIGHT).labelRes)
    }
}
