package com.example.healthlog.ui.history.filter

import com.example.healthlog.R
import com.example.healthlog.database.Measurement
import com.example.healthlog.guidelines.BloodGlucoseCategory
import com.example.healthlog.guidelines.BloodPressureStage
import com.example.healthlog.guidelines.BmiCategory
import com.example.healthlog.guidelines.PulseZone
import com.example.healthlog.model.BodyPosition
import com.example.healthlog.model.MealContext
import com.example.healthlog.model.MeasurementArm
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

        val result = filterMeasurements(list, HistoryFilterState())
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
            HistoryFilterState(stages = setOf(ClinicalStageFilter.Pulse(PulseZone.BRADYCARDIA))),
        )
        assertEquals(listOf(bradycardia), bradycardiaFiltered)

        val normalFiltered = filterMeasurements(
            measurements,
            HistoryFilterState(stages = setOf(ClinicalStageFilter.Pulse(PulseZone.NORMAL))),
        )
        assertEquals(listOf(normal), normalFiltered)

        val tachycardiaFiltered = filterMeasurements(
            measurements,
            HistoryFilterState(stages = setOf(ClinicalStageFilter.Pulse(PulseZone.TACHYCARDIA))),
        )
        assertEquals(listOf(tachycardia), tachycardiaFiltered)
    }

    @Test
    fun filterMeasurements_pulse_filtersByMultipleZonesTogether() {
        val bradycardia = Measurement(id = 1, type = MeasurementType.PULSE, value = 55.0)
        val normal = Measurement(id = 2, type = MeasurementType.PULSE, value = 75.0)
        val tachycardia = Measurement(id = 3, type = MeasurementType.PULSE, value = 110.0)
        val measurements = listOf(bradycardia, normal, tachycardia)

        val multiFiltered = filterMeasurements(
            measurements,
            HistoryFilterState(
                stages = setOf(
                    ClinicalStageFilter.Pulse(PulseZone.BRADYCARDIA),
                    ClinicalStageFilter.Pulse(PulseZone.TACHYCARDIA),
                ),
            ),
        )
        assertEquals(listOf(bradycardia, tachycardia), multiFiltered)
    }

    @Test
    fun filterMeasurements_bloodPressure_filtersByStage() {
        val normal = Measurement(id = 1, type = MeasurementType.BLOOD_PRESSURE, value = 120.0, secondaryValue = 80.0)
        val stage1 = Measurement(id = 2, type = MeasurementType.BLOOD_PRESSURE, value = 145.0, secondaryValue = 92.0)
        val crisis = Measurement(id = 3, type = MeasurementType.BLOOD_PRESSURE, value = 185.0, secondaryValue = 125.0)
        val measurements = listOf(normal, stage1, crisis)

        val stage1Filtered = filterMeasurements(
            measurements,
            HistoryFilterState(stages = setOf(ClinicalStageFilter.BloodPressure(BloodPressureStage.GRADE_1_HYPERTENSION))),
        )
        assertEquals(listOf(stage1), stage1Filtered)

        val crisisFiltered = filterMeasurements(
            measurements,
            HistoryFilterState(stages = setOf(ClinicalStageFilter.BloodPressure(BloodPressureStage.HYPERTENSIVE_CRISIS))),
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
            HistoryFilterState(stages = setOf(ClinicalStageFilter.Glucose(BloodGlucoseCategory.PREDIABETES))),
        )
        assertEquals(listOf(prediabetesPostMeal), prediabetesFiltered)

        val diabetesFiltered = filterMeasurements(
            measurements,
            HistoryFilterState(stages = setOf(ClinicalStageFilter.Glucose(BloodGlucoseCategory.DIABETES))),
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
            filter = HistoryFilterState(stages = setOf(ClinicalStageFilter.Weight(BmiCategory.NORMAL))),
            latestHeightCm = 180.0,
        )
        assertEquals(listOf(normal), normalFiltered)

        val overweightFiltered = filterMeasurements(
            measurements = measurements,
            filter = HistoryFilterState(stages = setOf(ClinicalStageFilter.Weight(BmiCategory.OVERWEIGHT))),
            latestHeightCm = 180.0,
        )
        assertEquals(listOf(overweight), overweightFiltered)

        // Multiple selection: Normal weight AND Underweight together
        val normalAndUnderweightFiltered = filterMeasurements(
            measurements = measurements,
            filter = HistoryFilterState(
                stages = setOf(
                    ClinicalStageFilter.Weight(BmiCategory.UNDERWEIGHT),
                    ClinicalStageFilter.Weight(BmiCategory.NORMAL),
                ),
            ),
            latestHeightCm = 180.0,
        )
        assertEquals(listOf(underweight, normal), normalAndUnderweightFiltered)

        // Without height, weight cannot be classified into BMI stages -> empty list
        val withoutHeightFiltered = filterMeasurements(
            measurements = measurements,
            filter = HistoryFilterState(stages = setOf(ClinicalStageFilter.Weight(BmiCategory.NORMAL))),
            latestHeightCm = null,
        )
        assertTrue(withoutHeightFiltered.isEmpty())
    }

    @Test
    fun filterMeasurements_arm_filtersByLeftOrRightArm() {
        val leftArm = Measurement(
            id = 1,
            type = MeasurementType.BLOOD_PRESSURE,
            value = 120.0,
            secondaryValue = 80.0,
            arm = MeasurementArm.LEFT.name,
        )
        val rightArm = Measurement(
            id = 2,
            type = MeasurementType.BLOOD_PRESSURE,
            value = 125.0,
            secondaryValue = 82.0,
            arm = MeasurementArm.RIGHT.name,
        )
        val noArm = Measurement(
            id = 3,
            type = MeasurementType.BLOOD_PRESSURE,
            value = 118.0,
            secondaryValue = 78.0,
            arm = null,
        )
        val measurements = listOf(leftArm, rightArm, noArm)

        val leftFiltered = filterMeasurements(
            measurements,
            HistoryFilterState(conditions = setOf(MeasurementConditionFilter.Arm(MeasurementArm.LEFT))),
        )
        assertEquals(listOf(leftArm), leftFiltered)

        val rightFiltered = filterMeasurements(
            measurements,
            HistoryFilterState(conditions = setOf(MeasurementConditionFilter.Arm(MeasurementArm.RIGHT))),
        )
        assertEquals(listOf(rightArm), rightFiltered)
    }

    @Test
    fun filterMeasurements_bodyPosition_filtersBySittingStandingOrLying() {
        val sitting = Measurement(
            id = 1,
            type = MeasurementType.BLOOD_PRESSURE,
            value = 120.0,
            secondaryValue = 80.0,
            bodyPosition = BodyPosition.SITTING.name,
        )
        val standing = Measurement(
            id = 2,
            type = MeasurementType.BLOOD_PRESSURE,
            value = 125.0,
            secondaryValue = 82.0,
            bodyPosition = BodyPosition.STANDING.name,
        )
        val lying = Measurement(
            id = 3,
            type = MeasurementType.BLOOD_PRESSURE,
            value = 115.0,
            secondaryValue = 75.0,
            bodyPosition = BodyPosition.LYING_DOWN.name,
        )
        val measurements = listOf(sitting, standing, lying)

        val sittingFiltered = filterMeasurements(
            measurements,
            HistoryFilterState(conditions = setOf(MeasurementConditionFilter.Position(BodyPosition.SITTING))),
        )
        assertEquals(listOf(sitting), sittingFiltered)

        val multiPositionFiltered = filterMeasurements(
            measurements,
            HistoryFilterState(
                conditions = setOf(
                    MeasurementConditionFilter.Position(BodyPosition.SITTING),
                    MeasurementConditionFilter.Position(BodyPosition.LYING_DOWN),
                ),
            ),
        )
        assertEquals(listOf(sitting, lying), multiPositionFiltered)
    }

    @Test
    fun filterMeasurements_mealContext_filtersByBeforeOrAfterMeal() {
        val fasting = Measurement(
            id = 1,
            type = MeasurementType.BLOOD_SUGAR,
            value = 90.0,
            mealContext = MealContext.BEFORE_MEAL.name,
        )
        val postMeal = Measurement(
            id = 2,
            type = MeasurementType.BLOOD_SUGAR,
            value = 130.0,
            mealContext = MealContext.AFTER_MEAL_2H.name,
        )
        val measurements = listOf(fasting, postMeal)

        val fastingFiltered = filterMeasurements(
            measurements,
            HistoryFilterState(conditions = setOf(MeasurementConditionFilter.Meal(MealContext.BEFORE_MEAL))),
        )
        assertEquals(listOf(fasting), fastingFiltered)

        val postMealFiltered = filterMeasurements(
            measurements,
            HistoryFilterState(conditions = setOf(MeasurementConditionFilter.Meal(MealContext.AFTER_MEAL_2H))),
        )
        assertEquals(listOf(postMeal), postMealFiltered)
    }

    @Test
    fun filterMeasurements_arrhythmia_filtersByDetectedArrhythmia() {
        val normalPulse = Measurement(id = 1, type = MeasurementType.PULSE, value = 72.0, hasArrhythmia = false)
        val irregularPulse = Measurement(id = 2, type = MeasurementType.PULSE, value = 85.0, hasArrhythmia = true)
        val nullArrhythmia = Measurement(id = 3, type = MeasurementType.PULSE, value = 68.0, hasArrhythmia = null)
        val measurements = listOf(normalPulse, irregularPulse, nullArrhythmia)

        val arrhythmiaYesFiltered = filterMeasurements(
            measurements,
            HistoryFilterState(conditions = setOf(MeasurementConditionFilter.Arrhythmia(detected = true))),
        )
        assertEquals(listOf(irregularPulse), arrhythmiaYesFiltered)

        val arrhythmiaNoFiltered = filterMeasurements(
            measurements,
            HistoryFilterState(conditions = setOf(MeasurementConditionFilter.Arrhythmia(detected = false))),
        )
        assertEquals(listOf(normalPulse, nullArrhythmia), arrhythmiaNoFiltered)
    }

    @Test
    fun filterMeasurements_combiningStageAndConditions_appliesAllConstraints() {
        val match = Measurement(
            id = 1,
            type = MeasurementType.BLOOD_PRESSURE,
            value = 145.0,
            secondaryValue = 92.0,
            arm = MeasurementArm.LEFT.name,
            bodyPosition = BodyPosition.SITTING.name,
            hasArrhythmia = true,
        )
        val wrongStage = Measurement(
            id = 2,
            type = MeasurementType.BLOOD_PRESSURE,
            value = 120.0,
            secondaryValue = 80.0,
            arm = MeasurementArm.LEFT.name,
            bodyPosition = BodyPosition.SITTING.name,
            hasArrhythmia = true,
        )
        val wrongArm = Measurement(
            id = 3,
            type = MeasurementType.BLOOD_PRESSURE,
            value = 145.0,
            secondaryValue = 92.0,
            arm = MeasurementArm.RIGHT.name,
            bodyPosition = BodyPosition.SITTING.name,
            hasArrhythmia = true,
        )
        val noArrhythmia = Measurement(
            id = 4,
            type = MeasurementType.BLOOD_PRESSURE,
            value = 145.0,
            secondaryValue = 92.0,
            arm = MeasurementArm.LEFT.name,
            bodyPosition = BodyPosition.SITTING.name,
            hasArrhythmia = false,
        )
        val measurements = listOf(match, wrongStage, wrongArm, noArrhythmia)

        val combinedFiltered = filterMeasurements(
            measurements,
            HistoryFilterState(
                stages = setOf(ClinicalStageFilter.BloodPressure(BloodPressureStage.GRADE_1_HYPERTENSION)),
                conditions = setOf(
                    MeasurementConditionFilter.Arm(MeasurementArm.LEFT),
                    MeasurementConditionFilter.Position(BodyPosition.SITTING),
                    MeasurementConditionFilter.Arrhythmia(detected = true),
                ),
            ),
        )
        assertEquals(listOf(match), combinedFiltered)
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
    fun availableConditionsForType_returnsExpectedConditionsForEachType() {
        assertEquals(7, availableConditionsForType(MeasurementType.BLOOD_PRESSURE).size)
        assertEquals(2, availableConditionsForType(MeasurementType.PULSE).size)
        assertEquals(2, availableConditionsForType(MeasurementType.BLOOD_SUGAR).size)
        assertTrue(availableConditionsForType(MeasurementType.WEIGHT).isEmpty())
        assertTrue(availableConditionsForType(MeasurementType.HEIGHT).isEmpty())
    }

    @Test
    fun labelRes_mapsToExpectedStringResource() {
        assertEquals(R.string.bp_stage_grade_1, ClinicalStageFilter.BloodPressure(BloodPressureStage.GRADE_1_HYPERTENSION).labelRes)
        assertEquals(R.string.pulse_zone_bradycardia, ClinicalStageFilter.Pulse(PulseZone.BRADYCARDIA).labelRes)
        assertEquals(R.string.glucose_category_prediabetes, ClinicalStageFilter.Glucose(BloodGlucoseCategory.PREDIABETES).labelRes)
        assertEquals(R.string.bmi_category_overweight, ClinicalStageFilter.Weight(BmiCategory.OVERWEIGHT).labelRes)
        assertEquals(R.string.arm_left, MeasurementConditionFilter.Arm(MeasurementArm.LEFT).labelRes)
        assertEquals(R.string.position_sitting, MeasurementConditionFilter.Position(BodyPosition.SITTING).labelRes)
        assertEquals(R.string.meal_before, MeasurementConditionFilter.Meal(MealContext.BEFORE_MEAL).labelRes)
        assertEquals(R.string.arrhythmia_yes, MeasurementConditionFilter.Arrhythmia(detected = true).labelRes)
        assertEquals(R.string.arrhythmia_no, MeasurementConditionFilter.Arrhythmia(detected = false).labelRes)
    }

    @Test
    fun filterMeasurements_dateFilter_today_filtersTodayReadings() {
        val now = java.time.ZonedDateTime.of(2026, 11, 17, 15, 0, 0, 0, java.time.ZoneId.of("Europe/Athens"))
        val todayReading = Measurement(
            id = 1,
            type = MeasurementType.PULSE,
            value = 70.0,
            timestamp = now.minusHours(2).toInstant().toEpochMilli(),
        )
        val yesterdayReading = Measurement(
            id = 2,
            type = MeasurementType.PULSE,
            value = 72.0,
            timestamp = now.minusDays(1).toInstant().toEpochMilli(),
        )

        val filtered = filterMeasurements(
            measurements = listOf(todayReading, yesterdayReading),
            filter = HistoryFilterState(dateFilter = DateFilter.Today),
            now = now,
        )
        assertEquals(listOf(todayReading), filtered)
    }

    @Test
    fun filterMeasurements_dateFilter_last7Days_filtersWithin7Days() {
        val now = java.time.ZonedDateTime.of(2026, 11, 17, 15, 0, 0, 0, java.time.ZoneId.of("Europe/Athens"))
        val threeDaysAgo = Measurement(
            id = 1,
            type = MeasurementType.PULSE,
            value = 70.0,
            timestamp = now.minusDays(3).toInstant().toEpochMilli(),
        )
        val tenDaysAgo = Measurement(
            id = 2,
            type = MeasurementType.PULSE,
            value = 72.0,
            timestamp = now.minusDays(10).toInstant().toEpochMilli(),
        )

        val filtered = filterMeasurements(
            measurements = listOf(threeDaysAgo, tenDaysAgo),
            filter = HistoryFilterState(dateFilter = DateFilter.Last7Days),
            now = now,
        )
        assertEquals(listOf(threeDaysAgo), filtered)
    }

    @Test
    fun filterMeasurements_dateFilter_last30Days_filtersWithin30Days() {
        val now = java.time.ZonedDateTime.of(2026, 11, 17, 15, 0, 0, 0, java.time.ZoneId.of("Europe/Athens"))
        val fifteenDaysAgo = Measurement(
            id = 1,
            type = MeasurementType.PULSE,
            value = 70.0,
            timestamp = now.minusDays(15).toInstant().toEpochMilli(),
        )
        val fortyDaysAgo = Measurement(
            id = 2,
            type = MeasurementType.PULSE,
            value = 72.0,
            timestamp = now.minusDays(40).toInstant().toEpochMilli(),
        )

        val filtered = filterMeasurements(
            measurements = listOf(fifteenDaysAgo, fortyDaysAgo),
            filter = HistoryFilterState(dateFilter = DateFilter.Last30Days),
            now = now,
        )
        assertEquals(listOf(fifteenDaysAgo), filtered)
    }

    @Test
    fun filterMeasurements_dateFilter_customRange_filtersExactDateSpan() {
        val zone = java.time.ZoneId.of("Europe/Athens")
        val now = java.time.ZonedDateTime.of(2026, 11, 17, 15, 0, 0, 0, zone)

        // Range: 12 November to 28 November 2026
        val customRange = DateFilter.CustomRange(
            startDate = java.time.LocalDate.of(2026, 11, 12),
            endDate = java.time.LocalDate.of(2026, 11, 28),
        )

        val beforeRange = Measurement(
            id = 1,
            type = MeasurementType.WEIGHT,
            value = 70.0,
            timestamp = java.time.LocalDate.of(2026, 11, 11).atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli(),
        )
        val startOfRange = Measurement(
            id = 2,
            type = MeasurementType.WEIGHT,
            value = 71.0,
            timestamp = java.time.LocalDate.of(2026, 11, 12).atStartOfDay(zone).toInstant().toEpochMilli(),
        )
        val middleOfRange = Measurement(
            id = 3,
            type = MeasurementType.WEIGHT,
            value = 72.0,
            timestamp = java.time.LocalDate.of(2026, 11, 20).atTime(12, 0).atZone(zone).toInstant().toEpochMilli(),
        )
        val endOfRange = Measurement(
            id = 4,
            type = MeasurementType.WEIGHT,
            value = 73.0,
            timestamp = java.time.LocalDate.of(2026, 11, 28).atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli(),
        )
        val afterRange = Measurement(
            id = 5,
            type = MeasurementType.WEIGHT,
            value = 74.0,
            timestamp = java.time.LocalDate.of(2026, 11, 29).atStartOfDay(zone).toInstant().toEpochMilli(),
        )

        val filtered = filterMeasurements(
            measurements = listOf(beforeRange, startOfRange, middleOfRange, endOfRange, afterRange),
            filter = HistoryFilterState(dateFilter = customRange),
            now = now,
        )
        assertEquals(listOf(startOfRange, middleOfRange, endOfRange), filtered)
    }

    @Test
    fun customRange_fromUtcMillis_createsExpectedLocalDateRange() {
        val startUtc = java.time.LocalDate.of(2026, 11, 12).atStartOfDay(java.time.ZoneId.of("UTC")).toInstant().toEpochMilli()
        val endUtc = java.time.LocalDate.of(2026, 11, 28).atStartOfDay(java.time.ZoneId.of("UTC")).toInstant().toEpochMilli()

        val range = DateFilter.CustomRange.fromUtcMillis(startUtc, endUtc)
        assertEquals(java.time.LocalDate.of(2026, 11, 12), range.startDate)
        assertEquals(java.time.LocalDate.of(2026, 11, 28), range.endDate)

        val singleDay = DateFilter.CustomRange.fromUtcMillis(startUtc, null)
        assertEquals(java.time.LocalDate.of(2026, 11, 12), singleDay.startDate)
        assertEquals(java.time.LocalDate.of(2026, 11, 12), singleDay.endDate)
    }

    @Test
    fun filterMeasurements_combiningStageConditionAndDate_appliesAllTogether() {
        val zone = java.time.ZoneId.of("Europe/Athens")
        val now = java.time.ZonedDateTime.of(2026, 11, 17, 15, 0, 0, 0, zone)

        val customRange = DateFilter.CustomRange(
            startDate = java.time.LocalDate.of(2026, 11, 12),
            endDate = java.time.LocalDate.of(2026, 11, 28),
        )

        val match = Measurement(
            id = 1,
            type = MeasurementType.BLOOD_PRESSURE,
            value = 145.0,
            secondaryValue = 92.0,
            arm = MeasurementArm.LEFT.name,
            timestamp = java.time.LocalDate.of(2026, 11, 15).atTime(10, 0).atZone(zone).toInstant().toEpochMilli(),
        )
        val wrongDate = Measurement(
            id = 2,
            type = MeasurementType.BLOOD_PRESSURE,
            value = 145.0,
            secondaryValue = 92.0,
            arm = MeasurementArm.LEFT.name,
            timestamp = java.time.LocalDate.of(2026, 11, 5).atTime(10, 0).atZone(zone).toInstant().toEpochMilli(),
        )
        val wrongArm = Measurement(
            id = 3,
            type = MeasurementType.BLOOD_PRESSURE,
            value = 145.0,
            secondaryValue = 92.0,
            arm = MeasurementArm.RIGHT.name,
            timestamp = java.time.LocalDate.of(2026, 11, 15).atTime(10, 0).atZone(zone).toInstant().toEpochMilli(),
        )
        val wrongStage = Measurement(
            id = 4,
            type = MeasurementType.BLOOD_PRESSURE,
            value = 120.0,
            secondaryValue = 80.0,
            arm = MeasurementArm.LEFT.name,
            timestamp = java.time.LocalDate.of(2026, 11, 15).atTime(10, 0).atZone(zone).toInstant().toEpochMilli(),
        )

        val filtered = filterMeasurements(
            measurements = listOf(match, wrongDate, wrongArm, wrongStage),
            filter = HistoryFilterState(
                stages = setOf(ClinicalStageFilter.BloodPressure(BloodPressureStage.GRADE_1_HYPERTENSION)),
                conditions = setOf(MeasurementConditionFilter.Arm(MeasurementArm.LEFT)),
                dateFilter = customRange,
            ),
            now = now,
        )
        assertEquals(listOf(match), filtered)
    }
}
