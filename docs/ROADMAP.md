# HealthLog — Product Roadmap & Feature Specifications

This document outlines the architectural strategy, feature specifications, and implementation roadmap for HealthLog.

---

## 1. Product Philosophy & Core Tenets

1. **Local-First & Offline-First:**
   - All data belongs to the user and stays stored locally on the device in Room.
   - The app functions 100% offline without mandatory account creation or internet connectivity.
2. **Zero Maintenance Cost (€0.00 / $0.00):**
   - By avoiding cloud server hosting and external database subscriptions, there are no ongoing operating costs.
3. **Privacy by Design (GDPR / HIPAA Non-Liability):**
   - Storing health data on cloud servers triggers heavy legal compliance obligations (EU GDPR Special Category Data, US HIPAA).
   - Keeping data strictly on the user's phone ensures user privacy and zero liability for data breaches.
4. **Actionable Clinical Context:**
   - Raw numbers are paired with clinical conditions (arm, posture, meal timing, arrhythmia) and evidence-based medical reference stages.

---

## 2. Feature Inventory & Analysis

### A. Medical Organization Guidelines & Stage Badges (Completed)
* **Purpose:** Classify measurements into clinical stages so users understand whether a reading is optimal, elevated, or requires medical attention.
* **Standards:**
  * **Blood Pressure:** International Society of Hypertension 2020 (ISH 2020) stages (Low < 90/< 60, Normal < 130/< 85, High-Normal 130–139/85–89, Grade 1 Hypertension 140–159/90–99, Grade 2 Hypertension ≥ 160/≥ 100, Hypertensive Crisis > 180/> 120 mmHg) with accompanying resting pulse evaluation.
  * **Blood Glucose:** World Health Organization & International Diabetes Federation (WHO / IDF) criteria correlated with `MealContext` (Hypoglycemia < 70 mg/dL; Fasting: Normal 70–99, Prediabetes 100–125, Diabetes ≥ 126 mg/dL; 2h Postprandial: Normal < 140, Prediabetes 140–199, Diabetes ≥ 200 mg/dL).
  * **Pulse:** World Heart Federation & WHO resting heart rate consensus (Bradycardia < 60, Normal 60–100, Tachycardia > 100 bpm) with athlete resting pulse footnote.
  * **Weight & Height:** WHO BMI categories (Underweight, Normal, Overweight, Obese), Oxford University "New BMI" ($1.3 \times \text{weight} / \text{height}^{2.5}$), healthy weight target range ($18.5 \times H^2 .. 24.9 \times H^2$) with delta, and WHO Asian population risk footnote.
* **UI Integration:** Color-coded stage badges directly beneath the reading card, visual gauge scales (discrete segmented meters and continuous spectrum needles), and inline expandable reference tables with active stage highlighting.

### B. Educational "How to Measure" Guide (Completed)
* **Purpose:** Educate users on clinical measurement protocols to eliminate inaccurate home readings (e.g. resting 5 minutes, cuff at heart level, avoiding caffeine/talking, fasting vs. postprandial timing).
* **Standards & Protocols:**
  * **Blood Pressure:** ISH 2020 / AHA practice guidelines (5-min quiet rest, back supported, arm at heart level, no speaking, empty bladder, caffeine/exercise abstinence).
  * **Blood Glucose:** WHO & ADA self-monitoring standards (wash hands with warm water, avoid first drop by wiping away, lance side of fingertip, test 2nd drop, log fasting vs. post-meal).
  * **Pulse:** WHF & WHO resting protocol (rest 5 min, index + middle fingers on radial artery below thumb base, count full 60 seconds).
  * **Weight:** WHO & CDC anthropometry (morning baseline, post-voiding, barefoot, centered on flat hard floor, 7-day trend focus).
  * **Height:** CDC growth & nutrition guidelines (barefoot, 4-point wall contact [heels, calves, buttocks, shoulders, back of head], horizontal line of sight).
* **UI Integration:**
  * Contextual `(?)` info icon on each metric's `NewEntryDialog` title bar navigating to a dedicated full-screen guide (`MeasurementGuideScreen`), returning cleanly with inputs preserved.
  * Friendly native doctor vector avatar (`ic_doctor_guide.xml`) with conversational speech bubble clouds (`SpeechBubbleCard`, `LeftSpeechBubbleShape`) breaking the monotony of plain text.
  * Structured two-cloud layout: Cloud 1 for step-by-step checklist, Cloud 2 for essential tips and rules.

### C. Condition Breakdown Statistics & Clinical Insights (Completed)
* **Purpose:** Calculate aggregate percentages of measurements taken under various clinical conditions over the selected chart period (e.g., `% Left vs. Right arm`, `% Sitting vs. Standing vs. Lying`, `% Before vs. After meal`, arrhythmia detection frequency).
* **UI Integration:** Dedicated `ConditionInsightsCard` placed directly below `PeriodCard` on the Chart page, featuring progress ratio bars, balanced arrhythmia alerts, and inlined conversational doctor speech bubble clouds with an attentive doctor avatar (`ic_doctor_warning.xml`).

### D. History List Filtering & Discovery
* **Purpose:** Quick filtering of history entries by health status.
* **Filters:** All entries, Flagged/High (Stage 1, Stage 2, Crisis), or specific conditions (e.g., Post-meal readings only).
* **UI Integration:** Filter chips row above the history list.

### E. Visual Normal Ranges & Clinical Stage Coloring on Charts
* **Purpose:** Give users instant visual feedback on their trends over time (7d, 1m, 6m, 1y, all).
* **Specifications:**
  * **Shaded Normal Target Bands:** Render subtle, semi-transparent shaded target/normal zones across the chart background (e.g. green band for systolic 90–129 mmHg and diastolic 60–84 mmHg, fasting glucose 70–99 mg/dL).
  * **Stage-Colored Data Points:** Dynamically tint individual chart data points (dots) using their clinical stage color (e.g. green for normal, orange for high-normal/prediabetes, red for hypertension/diabetes).
  * **Stage-Tethered Tooltip:** The tap tooltip / detail bubble displays the stage chip alongside the exact value, timestamp, and clinical conditions.

### F. Doctor Reporting & Print / Export (Zero Cost & Private)
* **Purpose:** Allow users to share comprehensive health logs with their physicians without complex hospital software or cloud servers.
* **Architecture:**
  * Uses native Android PDF generation (`android.graphics.pdf.PdfDocument`) and Android Print framework (`PrintManager`).
  * Generates a clean, 1–2 page PDF report containing patient summary, date range, average/min/max stats, condition breakdowns, chart snapshot, and tabular log.
  * Triggers Android's native share sheet (Email, WhatsApp, Print via Wi-Fi printer, or Save to Google Drive).
  * Optional CSV export for spreadsheet analysis.

### G. Medication Tracking & Local Reminders
* **Purpose:** Track active medications (name, dosage, frequency), maintain a daily intake checklist (taken/skipped), and configure local notifications so patients take them consistently.
* **Architecture:**
  * Room entities: `Medication(id, name, dosage, unit, frequency, instructions, isActive)` and `MedicationReminder(id, medicationId, hour, minute, daysOfWeek)`.
  * Scheduled notifications using Android `AlarmManager` (`SCHEDULE_EXACT_ALARM`) + `BroadcastReceiver` + `BootReceiver` (rescheduling on device reboot).

### H. Symptom Logging & Clinical Calendar Tracker
* **Purpose:** Log daily symptoms (headaches, dizziness, chest discomfort, palpitations, fatigue) with severity ratings (1–5) on an interactive calendar view.
* **Value:** Correlate symptom spikes directly with blood pressure or blood sugar variations and medication adherence on any given day.

### I. Measurement Notes (Dropped / Superseded)
* **Status:** Deprecated / dropped from Phase 3 scope.
* **Rationale:** In practice, free-form text inputs during rapid vital logging (< 5s) add unnecessary friction and form fatigue. Contextual observations are substantially better modeled as discrete **Symptoms** (with severity and timestamps) or **Medication adherence records**, avoiding an unused nullable column in the `measurements` table.

### J. Personal Profile, Biometric Lock & Data Portability
* **Single Personal Profile (Guest-First):**
  * Designed strictly as a personal health log (no confusing multi-user switching on a personal device).
  * Stored locally in a single-row Room table (`user_profile`) with optional demographic fields: **Name**, **Date of Birth** (computed Age), and **Gender**.
  * Zero onboarding friction — all fields are optional and can be filled in or updated anytime.
  * Optional integration with PDF Doctor Reports (patient header strip with toggle in `ExportDialog`).
* **Biometric / Device Lock:**
  * Optional app-level privacy lock powered by AndroidX `BiometricPrompt` (fingerprint, face unlock, or device PIN/pattern/password) — avoiding custom local passwords that cannot be recovered if forgotten.
  * Locks on cold launch and after 30 seconds in the background, paired with `FLAG_SECURE` when enabled.
* **Data Portability:**
  * Encrypted backup and restore to a JSON file via Storage Access Framework (SAF) that users can save to their personal Google Drive, SD card, or internal storage.

---

## 3. Phased Implementation Roadmap

```
┌────────────────────────────────────────────────────────────────────────┐
│ PHASE 1: Clinical Guidance & Reference Standards (Completed)           │
│ 1.1 Medical Organization Guidelines & Stage Badges (Completed)         │
│ 1.2 "How to Measure" Educational Guide (Completed)                     │
│ 1.3 Condition Breakdown Statistics & Insights (Completed)              │
├────────────────────────────────────────────────────────────────────────┤
│ PHASE 2: Data Visualization, Filtering & Doctor Reporting (Completed)  │
│ 2.1 Chart Normal Ranges / Target Bands (Vico charts) (Completed)       │
│ 2.2 History List Filtering (Stages, Conditions, Dates) (Completed)    │
│ 2.3 Doctor Report Export (On-device PDF & CSV to Downloads) (Completed)│
├────────────────────────────────────────────────────────────────────────┤
│ PHASE 3: Medications & Symptom Calendar (abandoned)                  │
│ 3.1 Bottom Navigation & Medication Management (AlarmManager)(Completed)│
│ 3.2 Symptom Logging & Clinical Correlation Calendar (Next Focus)       │
├────────────────────────────────────────────────────────────────────────┤
│ PHASE 4: Personal Profile, Security & Data Portability                 │
│ 4.1 Personal Profile & Biometric App Lock (Next Focus)                 │
│ 4.2 Backup & Restore (Encrypted JSON export/import via SAF)            │
└────────────────────────────────────────────────────────────────────────┘
```

### Step-by-Step Breakdown

#### **Phase 1: Deepening Clinical Value**
1. **Medical Organization Guidelines & Stage Badges (Completed):**
   * Pure domain evaluation logic (`com.example.healthlog.guidelines`), comprehensive unit test coverage, Room & ViewModel wiring (`latestHeight` flow).
   * Visual gauge scales (segmented and continuous spectrum), stage badges, and inline reference tables (`com.example.healthlog.ui.details.references`) in [MeasurementDetailsScreen](file:///home/nikos/Desktop/Git/HealthLog/app/src/main/java/com/example/healthlog/ui/details/MeasurementDetailsScreen.kt).
2. **Measurement Guide ("How to Measure") (Completed):**
   * Standardized clinical protocols (`com.example.healthlog.guidelines.protocols`) for all 5 vital signs based on ISH, WHO, ADA, WHF, and CDC standards.
   * Dedicated full-screen guide (`MeasurementGuideScreen`) wired seamlessly via `Routes.GUIDE` with single-top navigation.
   * Contextual `(?)` info button on `NewEntryDialog` title bar that opens the metric's protocol directly, returning back with typed values intact.
   * Conversational UI: native vector doctor avatar (`ic_doctor_guide.xml`) with custom speech cloud bubble (`LeftSpeechBubbleShape`) for friendly, approachable instructions.
   * Fully localized in English and Greek; 100% test coverage with automated unit and instrumented UI tests.
3. **Condition Breakdown Statistics & Clinical Insights (Completed):**
   * Aggregate condition breakdowns (% left vs. right arm, % sitting vs. standing vs. lying, meal context, arrhythmia frequency) on the History/Chart screen.
   * Smart clinical advice cards with speech bubble clouds delivered by an attentive doctor avatar (`ic_doctor_warning.xml`).

#### **Phase 2: Discovery & Doctor Communication**
4. **Chart Target Zones & Stage Coloring (2.1) (Completed):**
   * Shaded normal and alert target bands on Vico line charts (Pulse and dynamic height-based WHO BMI categories).
   * Contextual dot shapes and dynamic stage-colored data points across all metrics with 1.5 dp halos.
   * Stage-tinted tooltip callouts matching pinpoint condition colors and accessible chart shape legends.
5. **History List Filtering (2.2) (Completed):**
   * Space-saving `FilterBar` (~40dp) and `FilterBottomSheet` with deferred draft application on "Done".
   * Multi-stage clinical filtering (BP, Glucose, Pulse, Weight), physical condition filtering (Arm, Posture, Meal Context, Arrhythmia Yes & No), and date filtering (Today, 7 days, 30 days, Custom date span picker).
   * Live matching results counter on confirmation button (`Show all (52)`, `Show 12 entries`, `No matching entries (0)`).
   * State preservation across screen rotation via custom bundle-safe Savers, smooth landscape scrollability, and full Greek/English localization.
6. **Doctor Export Report (PDF/CSV) (2.3) (Completed):**
   * On-device clinical PDF reports and raw CSV spreadsheets saved directly to public `Downloads/HealthLog/`.
   * Export modal bottom sheet with format toggle (PDF / CSV), metric scope (Current / All), and date ranges (7 days, 30 days, all time, custom date picker).
   * Native A4 PDF report with branding, summary statistics box, trajectory graph, chronological table with clinical stage evaluations, and multi-page pagination.
   * RFC 4180 compliant CSV export with UTF-8 BOM encoding for seamless Microsoft Excel compatibility.
   * Instant feedback snackbar upon completion with zero external sharing/cloud dependencies.

#### **Phase 3: Medications & Symptom Calendar**
7. **Bottom Navigation & Medication Management (3.1 — Branch 1: `feature/medications-and-reminders`):**
   * Material 3 Bottom Navigation bar with 3 tabs: **Vitals** (measurements & charts), **Medications** (pill routine & alarms), and **Calendar** (symptoms & daily correlations).
   * Room entities: `Medication`, `MedicationReminder`, `MedicationDoseLog`.
   * Offline local notification scheduling using `AlarmManager` with exact alarm permissions, `BroadcastReceiver` ("Mark as Taken"), and `BootReceiver` (device reboot rescheduling).
   * UI: "Today's Schedule" checklist, "My Prescriptions" list, and "Add/Edit Medication" modal sheet.
8. **Symptom Logging & Clinical Correlation Calendar (3.2 — Branch 2: `feature/symptoms-and-calendar`):**
   * Dedicated interactive calendar view with colored indicator dots for symptoms, vitals, and medication adherence.
   * Standardized symptom catalog (Headache, Dizziness, Chest Discomfort, Palpitations, Shortness of Breath, Fatigue, Custom) with severity ratings (1–5).
   * Day detail correlation view linking vital spikes with symptom timestamps and missed/taken medications.
   * *Note on Measurement Notes:* Dropped/superseded in favor of discrete, structured symptoms and adherence records to preserve rapid vital entry.

#### **Phase 4: Personal Profile, Security & Data Portability**
9. **Personal Profile & Biometric App Lock (4.1):**
   * Single local personal profile (`UserProfile`: optional Name, Date of Birth/Age, Gender) stored in Room, accessible from the Home top app bar.
   * Optional device-native biometric/credential app lock (`BiometricPrompt`) with cold-launch lock, 30s background timeout, and `FLAG_SECURE` Recents protection.
   * Optional patient header section on exported Doctor PDF reports.
10. **Encrypted Backup & Restore (4.2):**
    * Full local JSON export/import of all health data (measurements, medications, reminders, dose logs, schedule overrides, symptoms, and personal profile) with optional AES-256-GCM password encryption via Storage Access Framework (SAF).

---

## 4. Technical Architecture Decisions

| Decision | Selection | Rationale |
| :--- | :--- | :--- |
| **Backend / Cloud** | None (Local-First) | Zero server operating costs (€0.00); absolute user privacy; avoids GDPR/HIPAA cloud storage liabilities. |
| **Doctor Sharing** | On-Device PDF & Print | Eliminates need for custom doctor portals; universally supported by all healthcare clinics via email or paper. |
| **User Profile & Security** | Single Local Profile + Biometric Lock | Frictionless personal health log with optional Name, DOB, and Gender, secured via Android `BiometricPrompt` (no cloud accounts or unrecoverable local passwords). |
| **Notifications** | Local `AlarmManager` | Reliable offline reminders without external push notification services (FCM) or remote server dependencies. |

---

## 5. Detailed Step-by-Step Implementation Blueprint

### Phase 1: Deepening Clinical Value (Immediate Next Steps)

#### Milestone 1.1: Medical Organization Guidelines & Stage Badges
* **Goal:** Evaluate any reading against established medical guidelines, display a stage badge on the measurement card, and show an expandable reference table in the `guidelinesSlot` of `MeasurementDetailsScreen`.
* **Step 1 — Domain Classification Model (`model/guidelines/`):**
  * Create pure data classes and enums:
    * `BloodPressureStage`: Normal (<120 and <80), Elevated (120–129 and <80), Stage 1 (130–139 or 80–89), Stage 2 (≥140 or ≥90), Hypertensive Crisis (>180 and/or >120).
    * `BloodSugarCategory`: Takes value + `MealContext`. Fasting/Before meal (Normal: 70–99, Prediabetes: 100–125, Diabetes: ≥126 mg/dL). 2h After meal (Normal: <140, Prediabetes: 140–199, Diabetes: ≥200 mg/dL).
    * `PulseZone`: Bradycardia (<60 bpm), Normal resting (60–100 bpm), Tachycardia (>100 bpm).
    * `BmiCategory`: When both height & weight are known, calculate BMI = kg / m² and classify (Underweight <18.5, Normal 18.5–24.9, Overweight 25–29.9, Obese ≥30).
  * Pure evaluation functions: `evaluateMeasurement(measurement: Measurement, latestHeight: Double? = null): GuidelineEvaluation?`.
* **Step 2 — Unit Tests (`app/src/test/java/com/example/healthlog/model/`):**
  * Test boundary thresholds for all stages (e.g. 119/79 vs 120/80 vs 130/80 vs 140/90 vs 181/120).
  * Test blood sugar evaluation before vs. after meals.
* **Step 3 — Strings & Localization (`res/values/strings.xml` and `res/values-el/strings.xml`):**
  * Add Greek and English labels and descriptions for all stages and guidelines (AHA/ACC, ADA, WHO).
* **Step 4 — UI Components (`ui/details/guidelines/`):**
  * `StageBadge`: Compact chip with a soft tinted background indicating the stage (e.g. green for Normal, amber for Elevated, red for Stage 2).
  * `GuidelineReferenceCard`: Expandable card displaying the full tier table with the user's current reading row highlighted.
* **Step 5 — Wiring into Details Screen:**
  * Pass `GuidelineReferenceCard` into `MeasurementDetailsScreen(guidelinesSlot = { ... })`.
* **Step 6 — Automated UI Tests:**
  * Add assertions to `MeasurementDetailsScreenTest.kt` verifying stage badge rendering and expandable table interaction.

#### Milestone 1.2: Educational "How to Measure" Guide (Completed)
* **Goal:** Guide users on proper clinical measurement protocols to eliminate inaccurate home readings.
* **Step 1 — Clinical Protocol Definition (`guidelines/protocols/MeasurementProtocol.kt`):**
  * Standardized clinical protocols for all 5 metrics (BP: ISH 2020 / AHA; Glucose: WHO / ADA; Pulse: WHF; Weight: WHO / CDC; Height: CDC).
  * Comprehensive unit test coverage in `MeasurementProtocolTest.kt`.
* **Step 2 — Contextual `(?)` Helper in Entry Dialog:**
  * Added `(?)` info button on `NewEntryDialog` title bar navigating to `Routes.GUIDE`.
  * Dedicated full-screen `MeasurementGuideScreen` with focused single-metric mode and tab browsing support.
  * Back button `←` pops back directly to the open entry dialog with all typed values preserved.
* **Step 3 — Conversational Persona & Speech Cloud UI (`guidelines/protocols/`):**
  * Friendly native doctor avatar vector (`ic_doctor_guide.xml`).
  * Custom `LeftSpeechBubbleShape` with speech tail delivering the clinical intro.
  * Two conversational speech cards (`SpeechBubbleCard`): Step-by-Step checklist and Important Tips & Rules.
* **Step 4 — Automated Testing & Localization:**
  * Full English (`values/strings.xml`) and Greek (`values-el/strings.xml`) translations.
  * Unit tests (`MeasurementProtocolTest.kt`) and instrumented UI tests (`MeasurementGuideScreenTest.kt`, `NewEntryDialogTest.kt`) all passing.

#### Milestone 1.3: Condition Breakdown Statistics & Clinical Insights (Completed)
* **Goal:** Present aggregate percentages of measurements taken under various clinical conditions over the selected chart period, accompanied by balanced clinical advice.
* **Step 1 — Aggregation Calculation & Domain Models (`ui/history/chart/stats/PeriodStatsCalculation.kt`):**
  * Added `ConditionDistribution<T>`, `ArrhythmiaStats`, `ClinicalAdviceNote`, and `ConditionBreakdown`.
  * Implemented pure `periodConditionStats()` calculating counts, percentages, and condition breakdowns for arm, posture, arrhythmia, and meal context.
  * Added evidence-based clinical advice evaluators (`evaluateArmAdvice`, `evaluatePositionAdvice`, `evaluateArrhythmiaAdvice`, `evaluateMealAdvice`) with false-positive prevention thresholds (e.g. minimum 5 readings and $\ge 25\%$ or $\ge 3$ detections for arrhythmia alerts).
* **Step 2 — Unit Tests (`PeriodStatsCalculationTest.kt`):**
  * 18 unit tests covering empty periods, pure/mixed arm distributions, posture distributions, meal distributions, arrhythmia frequency, threshold gating, and clinical advice evaluations.
* **Step 3 — UI Presentation & Localization (`ui/history/chart/ConditionInsightsCard.kt`):**
  * Created `ConditionInsightsCard` placed directly below `PeriodCard` on the Chart page (hidden for metrics without clinical conditions like Weight/Height and for empty periods).
  * Segmented distribution progress bars showing proportional visual fills with localized tags and counts/percentages.
  * Inlined conversational clinical advice warnings placed directly below their relevant condition section using an attentive doctor avatar (`ic_doctor_warning.xml`) and `LeftSpeechBubbleShape` with natural left text alignment.
  * Full localization in English (`values/strings.xml`) and Greek (`values-el/strings.xml`).
* **Step 4 — Instrumented Tests (`ChartPageTest.kt`):**
  * Added UI tests verifying condition breakdown rendering (arm, position, arrhythmia), dynamic updates when switching chart periods, and verifying cards remain hidden for Weight and empty periods.

---

### Phase 2: Discovery & Doctor Communication

#### Milestone 2.1: Chart Target Zones & Stage-Colored Data Points (Completed)
* **Goal:** Display shaded clinical target bands, contextual dot shapes, stage-colored data points, isolated neutral canvas, and stage-tinted tooltip callouts on Vico line charts.
* **Step 1 — Clinical Stage Bands & Canvas Isolation (`ui/history/chart/vico/TargetBandDecoration.kt`):**
  * Created custom `TargetBandDecoration` rendering horizontal semi-transparent clinical bands on Pulse and Weight charts.
  * **Pulse:** Bands for Bradycardia (<60 bpm, blue), Normal (60–100 bpm, green), and Tachycardia (>100 bpm, red).
  * **Weight:** Dynamic bands calculated from latest user height for Underweight (<18.5, blue), Normal (18.5–25, green), Overweight (25–30, orange), and Obese (≥30 BMI, red).
  * **Vivid Opacity Calibration:** Calibrated fill and stroke alphas for red (`0.38f`/`0.75f`) and orange (`0.34f`/`0.70f`) alert zones to prevent washed-out pinkish appearances on light canvas backgrounds.
  * **Isolated Canvas:** Wrapped the chart and legends in a dedicated `surfaceContainerHighest` card canvas (`RoundedCornerShape(16.dp)`) in `PeriodCard.kt` to prevent card tint bleed.
* **Step 2 — Contextual Shapes & Stage-Colored Data Points (`ui/history/chart/vico/EntryChart.kt`):**
  * **Blood Pressure:** Systolic Circle and Diastolic Diamond dots dynamically tinted by ISH stage color (Normal, High-Normal, Grade 1, Grade 2, Crisis).
  * **Blood Sugar:** Before-meal Circle and Post-meal Square dots dynamically tinted by WHO category (Hypoglycemia, Normal, Prediabetes, Diabetes).
  * **Pulse & Weight:** Dynamic `PulsePointProvider` and `WeightPointProvider` coloring dots by pulse zone and WHO BMI category matching target bands and reference tables.
  * **Contrast Halos:** 1.5 dp surface halos on every point to guarantee visibility over lines and bands.
  * **Shape Legends:** Added accessible contextual shape legends below Blood Pressure and Blood Glucose charts.
* **Step 3 — Stage-Tethered Tooltips & Color Harmonization (`ChartTooltip.kt`, `ReferenceColors.kt`):**
  * Chart tooltip displays the clinical stage chip dynamically tinted in the pinpoint's exact condition-based color.
  * Harmonized single source of truth across chart bands, dots, tooltips, and Details screen reference tables.
  * Calibrated indicator palette: softened `NORMAL` green (`#387A40` / `#66BB6A`) and golden-amber (`#FF8F00` / `#FFB300`) with zero ambiguity against red.
* **Step 4 — Automated Testing & Verification:**
  * 21 dedicated unit tests in `TargetRangeBandTest.kt` verifying band ranges, BMI math, point providers, and color resolution (all 199 unit tests passing).
  * 17 instrumented tests in `ChartPageTest.kt` verifying chart rendering, stats, condition insights, and the new contextual shape legends.

#### Milestone 2.2: History List Filtering (Completed)
* **Goal:** Allow users to filter the history entry list to quickly discover abnormal, contextual, or time-bounded readings without cluttering screen real estate.
* **Step 1 — Clinical Stages & Physical Conditions Domain Filtering:**
  * Implemented `ClinicalStageFilter` covering Blood Pressure stages (ISH 2020), Glucose categories (WHO/IDF), Pulse zones (WHF/WHO), and BMI categories (WHO) with multi-stage selection.
  * Implemented `MeasurementConditionFilter` covering Arm (`Left`, `Right`), Body Posture (`Sitting`, `Standing`, `Lying down`), Meal Context (`Before meal`, `2h After meal`), and Arrhythmia (`Yes` / detected, `No` / not detected).
  * Implemented compound filter matching in `HistoryFilterState`: multi-selection OR matching within the same category and AND matching across different categories.
* **Step 2 — Date Range Filtering & Picker Dialog:**
  * Implemented `DateFilter` with rolling presets (`All Time`, `Today`, `Last 7 Days`, `Last 30 Days`) and `CustomRange(startDate, endDate)`.
  * Material 3 `DateRangePicker` dialog with optimized concise labels in English (`Start date – End date`) and Greek (`Έναρξη – Λήξη`) preventing layout overflow.
  * Seamless UTC midnight conversion to local calendar days without timezone drift.
* **Step 3 — Space-Saving FilterBar & FilterBottomSheet:**
  * Slim (~40dp) `FilterBar` placed directly above `EntryList` keeping 85%+ of the screen clear for measurement cards. Shows active filter count badge (`Filter (N)`), active date chip, stage chips with clinical badge colors, condition chips with multi-color icons, and a quick one-tap `Reset` action.
  * Material 3 `ModalBottomSheet` with deferred application: draft selections are only applied to the background history list once confirmed with "Done".
  * Live matching results counter on confirmation button (`Show all (52)`, `Show 12 entries`, `No matching entries (0)`).
  * Full landscape scrollability and device rotation resilience using custom bundle-safe `Saver` implementations (`StageSetSaver`, `ConditionSetSaver`, `DateFilterSaver`).
* **Step 4 — Comprehensive Automated Testing:**
  * 22 dedicated unit tests in `HistoryFilterTest.kt` verifying stage filtering, condition combinations, date spans, and rotation roundtrip serialization (all 222 app unit tests passing).
  * 6 dedicated instrumented Compose tests in `HistoryFilterUiTest.kt` verifying FilterBar toggling, bottom sheet opening, deferred apply, chip removal, quick reset, and dynamic button counter (all 16 connected history tests passing on emulator).

#### Milestone 2.3: Doctor Report Export (PDF & CSV) (Completed)
* **Goal:** Enable users to export clean medical reports (PDF & CSV) directly to the device's Downloads folder without third-party share popups or cloud dependencies (100% on-device, private, zero cost).
* **Step 1 — Options Dialog (`ui/export/ExportDialog.kt`):**
  * Modal bottom sheet for configuring export format (PDF vs. CSV), measurement scope (Current metric vs. All metrics), and date range (Last 7 days, Last 30 days, All time, Custom date range picker).
  * Live matching entries validation that disables the download action when 0 records match.
  * Added download vector button (`ic_download.xml`) in History top app bar and full English/Greek localization.
* **Step 2 — Native Android PDF Generation (`ui/export/PdfReportGenerator.kt` & `ExportFileManager.kt`):**
  * Native `android.graphics.pdf.PdfDocument` implementation formatted for standard A4 printing.
  * Clinical layout:
    * Header: App brand, report title, generation date, metric scope, and date range.
    * Summary box: Total readings, average, lowest, and highest values.
    * Trajectory graph: Continuous vital signs trend line with background fill and min/max markers.
    * Chronological table: Date, Time, formatted values (e.g. `120/80 (72 bpm)`), evaluated clinical stages with stage coloring, and condition flags (Arm, Posture, Meal Context, Arrhythmia).
    * Multi-page pagination: Automatic page overflow detection, repeating headers, and bottom page numbering (`Page X`).
  * Direct persistence to device's public `Downloads/HealthLog/` folder via `MediaStore.Downloads` on API 29+ with legacy storage fallback.
  * Clean finish snackbar (`"PDF report saved to Downloads"` / `"Η αναφορά PDF αποθηκεύτηκε στις Λήψεις"`).
* **Step 3 — CSV Exporter (`ui/export/CsvReportGenerator.kt`):**
  * RFC 4180 compliant CSV stream containing all reading values, secondary values, pulse, units, clinical stages, and condition flags.
  * UTF-8 Byte Order Mark (BOM: `EF BB BF`) for out-of-the-box Excel compatibility with international characters.
  * Direct save to `Downloads/HealthLog/` with clean finish snackbar (`"CSV data saved to Downloads"` / `"Τα δεδομένα CSV αποθηκεύτηκαν στις Λήψεις"`).
* **Testing & Verification:**
  * 3 dedicated JVM unit tests in `ExportConfigTest.kt`, `ExportConfigPdfTest.kt`, and `CsvReportGeneratorTest.kt` (all 230 app unit tests passing).
  * 7 dedicated instrumented Compose tests in `ExportDialogUiTest.kt` verifying format switching, scope selection, chip selection, live validation, cancel, and close actions.
  * 3 dedicated on-device generator tests in `ExportGeneratorsDeviceTest.kt` verifying real A4 PDF creation (`%PDF` magic header), CSV creation with UTF-8 BOM, and MediaStore Downloads insertion.

---

### Phase 3: Medications & Symptom Calendar (2-Branch Strategy)

#### Milestone 3.1: Bottom Navigation & Medication Management (Branch 1: `feature/medications-and-reminders`) (Completed)
* **Goal:** Introduce the global 3-tab Bottom Navigation bar (Vitals, Medications, Calendar), build complete local medication tracking with intake schedules, and configure reliable offline alarms.
* **Step 1 — Global Navigation Scaffold (`ui/HealthLogNavHost.kt`):**
  * Introduce Material 3 `NavigationBar` with 3 destinations:
    * `Vitals`: Hosts the current measurement logging cards, history, and Vico charts.
    * `Medications`: Daily pill schedule, adherence checklist, and prescription management.
    * `Calendar`: Monthly/weekly calendar tracker and symptom correlation.
  * Ensure smooth tab switching preserving backstack state.
* **Step 2 — Database Entities & DAO (`database/`):**
  * `Medication` entity (`id`, `name`, `dosage`, `unit`, `frequency`, `instructions`, `isActive`, `createdAt`).
  * `MedicationReminder` entity (`id`, `medicationId`, `hour`, `minute`, `daysOfWeekMask`, `isEnabled`).
  * `MedicationDoseLog` entity (`id`, `medicationId`, `scheduledTimestamp`, `actionTimestamp`, `status` [TAKEN, SKIPPED]).
  * Create `MedicationDao` with reactive `Flow` queries and intake adherence tracking.
* **Step 3 — Offline Local Notifications & Alarm Architecture:**
  * Implement `MedicationAlarmScheduler` using Android `AlarmManager` with exact alarm capabilities (`setExactAndAllowWhileIdle` / `SCHEDULE_EXACT_ALARM`).
  * `MedicationAlarmReceiver`: BroadcastReceiver triggering high-priority notification with instant actions ("Mark Taken", "Snooze 30m").
  * `BootReceiver`: Reschedules active medication alarms upon device reboot (`ACTION_BOOT_COMPLETED`).
  * Android 13+ runtime permission flow for `POST_NOTIFICATIONS`.
* **Step 4 — Medication Management UI (`ui/medications/`):**
  * **Today's Schedule Section:** Grouped by time (Morning, Afternoon, Evening) with quick one-tap `[Mark as Taken]` buttons.
  * **My Prescriptions Section:** Active medications list showing dosages, schedules, and active alarm bells.
  * **Add/Edit Medication Bottom Sheet:** Name, dosage amount, unit picker (tablet, mg, ml, drops), daily reminder times, and frequency picker.
* **Step 5 — Testing & Localization:**
  * Unit tests for DAO, adherence logic, and alarm scheduling.
  * Instrumented Compose UI tests for bottom nav tab switching and medication form validation.
  * Full English and Greek localization.

#### Milestone 3.2: Symptom Logging & Clinical Correlation Calendar (Branch 2: `feature/symptoms-and-calendar`) (Completed)
* **Goal:** Enable discrete symptom logging with severity ratings and an interactive calendar view that correlates symptom spikes with vital sign readings and medication intake.
* **Step 1 — Database Entity & Migration (`database/`):**
  * `SymptomEntry` entity (`id`, `symptomType` [enum name or custom], `customName`, `severity` [1–5 Int], `timestamp`, `notes`).
  * `SymptomDao` with range queries for calendar days and month intervals (`getSymptomsInRange`, `getAllSymptoms`, `getSymptomById`).
  * `SymptomRepository` integrated into Room `HealthDatabase` with automated migration `MIGRATION_4_5` preserving all measurement and medication data.
* **Step 2 — Symptom Catalog & Clinical Categories (`model/` & `ui/calendar/`):**
  * Predefined common symptoms: Headache, Dizziness, Palpitations, Chest Discomfort, Shortness of Breath, Fatigue, Nausea, and Other / Custom.
  * Severity scale: 1 (Very Mild) to 5 (Very Severe) with soft tinted badges and level numbers.
  * `CalendarDaySummary` & pure `CalendarAggregator`: aggregates measurements, medication dose logs, and symptoms per calendar day with date-range parsing and timezone resilience.
* **Step 3 — Interactive Calendar Grid (`ui/calendar/`):**
  * Monthly grid with weekday headers, previous/next month navigation arrows, and a clickable month/year title opening a 4×3 `MonthYearPickerDialog`.
  * "Today" quick-jump action in the top app bar.
  * Three category indicator dots beneath calendar dates matching the legend:
    * 🟢 Green dot = Vitals recorded on this date.
    * 🟠 Amber dot = Symptom logged on this date.
    * 🔵 Blue dot = Medication doses taken on this date.
  * Selected day pill retains crisp category indicator dot colors.
* **Step 4 — Day Detail & Clinical Correlation View:**
  * Selecting any date renders a unified chronological timeline:
    * **Symptoms:** severity badge, symptom title, timestamp, optional notes, with Edit and Delete actions.
    * **Vital Signs:** formatted values, time, and multi-colored measurement type icon (`MeasurementType.icon`) matching the home screen; companion pulse readings automatically filtered out on the calendar page when part of a blood pressure reading to prevent duplicate cards; tapping opens full measurement details.
    * **Medications Taken:** scheduled time, dose status (taken/skipped), and medication icon.
  * `AddEditSymptomSheet`: modal bottom sheet with filter chips for predefined symptoms, custom symptom name input, live 1–5 severity slider with color-coded badge, date & time picker, and optional notes.
* **Step 5 — Testing & Localization:**
  * 264 passing unit tests covering database operations, migrations, aggregation calculations, ISO date parsing, companion pulse deduplication, and ViewModel state transitions.
  * 7 instrumented Compose UI tests in `CalendarUiTest.kt` verifying calendar grid navigation, month/year dialog selection, day detail cards, symptom creation, custom symptom validation, and deletion dialog.
  * 100% bilingual parity in English (`values/strings.xml`) and Greek (`values-el/strings.xml`).

---

### Phase 4: Personal Profile, Security & Data Portability

#### Milestone 4.1: Personal Profile & Biometric App Lock
* **Goal:** Provide a lean, guest-first personal profile (Name, Date of Birth / Age, Gender) integrated with Doctor PDF reports, paired with optional device-native biometric/credential app lock for privacy.
* **Step 1 — Database Entity & Migration (`database/` & `repository/`):**
  * Create single-row `UserProfile` entity (`id = 1`, `name`, `dateOfBirth` [ISO `YYYY-MM-DD` nullable], `gender` [enum name nullable], `updatedAt`).
  * Add `UserProfileDao` and `UserProfileRepository` exposing reactive `Flow<UserProfile?>` and upsert/clear operations.
  * Add Room `MIGRATION_5_6` creating `user_profile` without altering existing tables.
  * Pure helper to compute current Age from `dateOfBirth`.
* **Step 2 — Biometric App Lock & Privacy Protection (`MainActivity` & `security/`):**
  * Update `MainActivity` to extend `FragmentActivity` to host AndroidX `BiometricPrompt` (`BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL`).
  * Store `isAppLockEnabled` in device-local `SharedPreferences` (`app_settings`) so it is read synchronously in `onCreate` before the first frame renders (zero flash of sensitive data) and stays strictly device-local.
  * Lock on cold launch and when returning from background after a **30-second grace period**.
  * Apply `WindowManager.LayoutParams.FLAG_SECURE` while App Lock is enabled to prevent screenshots and Recents thumbnail previews.
  * Verify device biometric/credential availability before allowing the user to enable App Lock, and require authentication before disabling it.
* **Step 3 — Profile Screen & Navigation (`ui/profile/` & `ui/HealthLogNavHost.kt`):**
  * Add `Routes.PROFILE` and a Profile action button in the Home top app bar (`HomeContent`).
  * Clean Material 3 Profile screen with:
    * Personal Info section: Name input, Date of Birth picker displaying calculated Age, and Gender selector (all optional).
    * Security section: Biometric / Device Lock toggle with status/availability helper text.
    * Natural placement for the upcoming Backup & Restore section in Milestone 4.2.
* **Step 4 — Doctor PDF Report Integration (`ui/export/`):**
  * Render a compact Patient Info strip (Name, Age/DOB, Gender — omitting unset fields) in the `PdfReportGenerator` header when enabled.
  * Add an `"Include profile details"` toggle in `ExportDialog` (shown/defaulted ON when at least one profile field is filled, allowing anonymous exports in one tap).
* **Step 5 — Testing & Localization:**
  * Room migration test (`MIGRATION_5_6`) in `MigrationTest.kt`.
  * Unit tests for `UserProfileRepository`, age calculation, lock timeout logic, and PDF export config/header handling.
  * Instrumented UI tests for Profile editing, persistence, lock toggle flow, and ExportDialog profile toggle.
  * Full English (`values/strings.xml`) and Greek (`values-el/strings.xml`) localization.

#### Milestone 4.2: Data Backup & Restore
* **Goal:** Allow users to export and import their entire personal health log (measurements, medications, reminders, dose logs, schedule overrides, symptoms, and personal profile) securely to protect against device loss or upgrades.
* **Step 1 — JSON Serialization Engine:**
  * Serialize and deserialize all Room database tables (`measurements`, `medications`, `medication_reminders`, `medication_schedule_overrides`, `medication_dose_logs`, `symptoms`, `user_profile`) with schema version metadata to/from structured JSON.
  * Keep device-local security flags (`isAppLockEnabled`) excluded from backups so restoring on a new phone never locks out the user.
* **Step 2 — Encrypted Export:**
  * Optional password-based encryption (AES-256-GCM with PBKDF2 key derivation) for the exported backup file.
* **Step 3 — Storage Access Framework (SAF) & Profile UI Integration:**
  * Surface Backup & Restore controls inside the Profile screen introduced in Milestone 4.1.
  * Use `ActivityResultContracts.CreateDocument` to export to local storage, SD card, or Google Drive.
  * Use `ActivityResultContracts.OpenDocument` to import and restore.
* **Step 4 — Safe Import Transaction & Alarm Rescheduling:**
  * Validate JSON integrity, decrypt if password-protected, and execute database restoration atomically inside a Room transaction, preventing partial or corrupted imports.
  * Automatically reschedule active medication alarms after a successful restore.

