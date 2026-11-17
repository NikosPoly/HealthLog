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

### G. Measurement Notes & Health Journal
* **Purpose:** Allow users to record brief contextual observations directly on an entry (e.g. "felt dizzy after morning run", "forgot evening medication").
* **Architecture:** Add nullable `notes TEXT` column to `Measurement` via database migration.

### H. Medication Tracking & Local Reminders
* **Purpose:** Track active medications (name, dosage, frequency) and configure local notifications so patients take them consistently.
* **Architecture:**
  * Room entity: `Medication(id, name, doseUnit, frequency, instructions, isActive)`.
  * Scheduled notifications using Android `AlarmManager` + BroadcastReceiver with exact alarm permissions.

### I. Symptom Logging & Calendar Tracker
* **Purpose:** Log daily symptoms (headaches, chest pain, palpitations, fatigue) with severity ratings (1–5) on a calendar view.
* **Value:** Enables doctors to correlate symptom spikes with blood pressure or blood sugar variations.

### J. User Profiles & Data Portability
* **Local Multi-Profile Strategy:**
  * Stored strictly in local Room database (e.g. `Profile` entity + `profileId` on entries).
  * Ideal for family members (e.g., tracking a parent's and a child's vitals on a shared device).
  * Guest/Default profile enabled by default with zero onboarding friction.
* **Data Portability:**
  * Encrypted backup and restore to JSON/SQLite file that users can save to their personal Google Drive or internal storage.

---

## 3. Phased Implementation Roadmap

```
┌────────────────────────────────────────────────────────────────────────┐
│ PHASE 1: Clinical Guidance & Reference Standards (Completed)           │
│ 1.1 Medical Organization Guidelines & Stage Badges (Completed)         │
│ 1.2 "How to Measure" Educational Guide (Completed)                     │
│ 1.3 Condition Breakdown Statistics & Insights (Completed)              │
├────────────────────────────────────────────────────────────────────────┤
│ PHASE 2: Data Visualization, Filtering & Doctor Reporting (Next Focus) │
│ 2.1 Chart Normal Ranges / Target Bands (Vico charts) (Completed)       │
│ 2.2 History List Filtering (Stages, Conditions, Dates) (Completed)    │
│ 2.3 Doctor Report Export (On-device PDF generation, CSV, Print/Share)   │
├────────────────────────────────────────────────────────────────────────┤
│ PHASE 3: Health Journal & Medication Management                        │
│ 3.1 Per-Measurement Contextual Notes (DB migration)                    │
│ 3.2 Medication Management & Local Reminders (AlarmManager)             │
│ 3.3 Symptom Logging & Calendar View                                    │
├────────────────────────────────────────────────────────────────────────┤
│ PHASE 4: Local Profiles & Data Portability                             │
│ 4.1 Local Multi-User Profiles (device profile switcher, guest-first)    │
│ 4.2 Backup & Restore (Encrypted JSON/database export to Drive/Files)    │
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
6. **Doctor Export Report (PDF/CSV):**
   * The most effective answer to Item E: allows patients to export and share comprehensive reports with their doctor.

#### **Phase 3: Expanded Daily Tracking**
7. **Per-Measurement Notes:**
   * Add a nullable `notes` column to `Measurement` (Migration 4 → 5).
8. **Medications & Reminders:**
   * Medication list, dosages, schedules, and local push notifications.
9. **Symptom Tracker & Calendar:**
   * Dedicated calendar view to log daily symptoms and correlate with vital signs.

#### **Phase 4: User Accounts & Cloud Infrastructure**
10. **Authentication & User Profiles (with Guest Mode):**
    * Design whether accounts are local multi-profile (e.g., family members on one tablet) or cloud-based (Firebase Auth / Supabase).
    * Migrate database to associate records with `userId`.

---

## 4. Technical Architecture Decisions

| Decision | Selection | Rationale |
| :--- | :--- | :--- |
| **Backend / Cloud** | None (Local-First) | Zero server operating costs (€0.00); absolute user privacy; avoids GDPR/HIPAA cloud storage liabilities. |
| **Doctor Sharing** | On-Device PDF & Print | Eliminates need for custom doctor portals; universally supported by all healthcare clinics via email or paper. |
| **User Accounts** | Local Device Profiles | Frictionless zero-login startup for single users, with optional local profile switching for multi-user households. |
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

#### Milestone 2.3: Doctor Report Export (PDF & CSV)
* **Goal:** Enable users to export, print, or share a clean medical report directly with their physician (100% on-device, private, zero cost).
* **Step 1 — Options Dialog:**
  * Date range selector (Last 7 days, Last 30 days, Custom range) and format selector (PDF or CSV).
* **Step 2 — Native Android PDF Generation (`ui/export/`):**
  * Use `android.graphics.pdf.PdfDocument` to lay out a clean 1–2 page PDF report:
    * Header: App brand, patient name placeholder, export date, date range.
    * Summary box: Average, min, max readings, and % in elevated stages.
    * Mini chart snapshot or trajectory graph.
    * Chronological table: Date, Time, Values, Arm, Posture, Meal Context, Arrhythmia, and Notes.
* **Step 3 — CSV Exporter:**
  * Stream formatted comma-separated values for spreadsheet analysis.
* **Step 4 — Direct Print & Share Sheet:**
  * Integrate `PrintManager` via `PrintDocumentAdapter` for direct Wi-Fi printing.
  * Use `FileProvider` with `Intent(Intent.ACTION_SEND)` to share directly via Email, WhatsApp, or Drive.

---

### Phase 3: Expanded Daily Tracking

#### Milestone 3.1: Per-Measurement Notes
* **Goal:** Allow users to record brief contextual observations (e.g., "headache after gym", "forgot morning pill").
* **Step 1 — Database Migration (v4 → v5):**
  * Add nullable `notes TEXT NULL` column to `Measurement` entity.
  * Create `MIGRATION_4_5` in `HealthDatabase.kt` (`ALTER TABLE measurements ADD COLUMN notes TEXT`).
  * Increment database version to 5 and generate new schema in `app/schemas/`.
* **Step 2 — Migration Test:**
  * Add automated migration test in `MigrationTest.kt` verifying older rows retain existing data with null notes.
* **Step 3 — New Entry Dialog Input:**
  * Add an optional "Notes" field (single/multi-line text input) in `NewEntryDialog`.
* **Step 4 — Display in UI:**
  * Show note snippet on `EntryCard` and full note block in `MeasurementDetailsScreen`.

#### Milestone 3.2: Medication Tracking & Local Reminders
* **Goal:** Track daily medications and configure local notifications to take them on time.
* **Step 1 — Database Entities & DAO (`database/`):**
  * Create `Medication` entity (`id`, `name`, `dosage`, `unit`, `frequency`, `instructions`, `isActive`).
  * Create `MedicationReminder` entity (`id`, `medicationId`, `hour`, `minute`, `daysOfWeek`).
  * Create `MedicationDao` and repository methods.
* **Step 2 — Local Notification Scheduling:**
  * Implement `AlarmManager` scheduler with exact alarm support (`SCHEDULE_EXACT_ALARM`) and a `BroadcastReceiver` that triggers a high-priority notification with an action button ("Mark Taken").
* **Step 3 — Medication Management UI:**
  * Dedicated "Medications" screen/bottom sheet to add, edit, or deactivate medications and set alarm times.
* **Step 4 — Android Permissions:**
  * Request runtime `POST_NOTIFICATIONS` permission on Android 13+ (API 33).

#### Milestone 3.3: Symptom Logging & Calendar Tracker
* **Goal:** Log daily symptoms (headaches, chest pain, palpitations, fatigue) with severity ratings (1–5) on a calendar view.
* **Step 1 — Database Entity (`database/`):**
  * Create `SymptomEntry` (`id`, `symptomType`, `severity`, `timestamp`, `notes`).
* **Step 2 — Predefined Common Symptoms:**
  * Standardized health symptoms list (Dizziness, Headache, Palpitations, Shortness of breath, Fatigue, Custom).
* **Step 3 — Calendar UI View:**
  * Monthly/weekly calendar composable displaying dots on days with logged symptoms.
* **Step 4 — Clinical Correlation View:**
  * Cross-reference symptom dates with blood pressure or blood sugar readings to help identify triggers.

---

### Phase 4: Local Profiles & Data Portability

#### Milestone 4.1: Local Multi-User Profiles (Guest-First)
* **Goal:** Enable multi-user tracking on a single shared device (e.g. family members) without cloud servers or accounts.
* **Step 1 — Database Entity & Migration:**
  * Create `Profile` entity (`id`, `name`, `avatarColor`, `isDefault`, `createdAt`).
  * Database migration: add `profileId INTEGER NOT NULL DEFAULT 1` to `Measurement`.
  * Pre-populate database with a default "Personal" profile so existing user data is completely preserved.
* **Step 2 — Profile Management Repository:**
  * Manage active profile selection using DataStore / StateFlow.
  * Scope all DAO queries by `activeProfileId`.
* **Step 3 — Profile Switcher UI:**
  * Top bar profile dropdown / switcher sheet allowing one-tap switching between profiles and adding a new profile.

#### Milestone 4.2: Data Backup & Restore
* **Goal:** Allow users to export and import their entire health log (measurements, medications, symptoms, profiles) securely.
* **Step 1 — JSON Serialization Engine:**
  * Serialize and deserialize all Room database tables to/from structured JSON.
* **Step 2 — Encrypted Export:**
  * Optional password-based encryption (AES-256-GCM) for the backup file.
* **Step 3 — Storage Access Framework (SAF):**
  * Use `ActivityResultContracts.CreateDocument` to export to local storage, SD card, or Google Drive.
  * Use `ActivityResultContracts.OpenDocument` to import and restore.
* **Step 4 — Safe Import Transaction:**
  * Validate JSON integrity and execute database restoration inside a Room transaction, preventing partial or corrupted imports.

