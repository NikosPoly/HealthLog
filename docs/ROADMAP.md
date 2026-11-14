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

### B. Educational "How to Measure" Guide
* **Purpose:** Educate users on clinical measurement protocol to eliminate inaccurate home readings (e.g. resting 5 minutes, cuff at heart level, avoiding caffeine/talking, fasting vs. postprandial timing).
* **UI Integration:**
  * Contextual `(?)` info icon on each metric's New Entry dialog.
  * Central guide sheet/screen paired with medical guideline tables.

### C. Condition Breakdown Statistics (Insights)
* **Purpose:** Calculate aggregate percentages of measurements taken under various clinical conditions over the selected chart period (e.g., `% Left vs. Right arm`, `% Sitting vs. Standing`, `% Before vs. After meal`, arrhythmia detection frequency).
* **UI Integration:** Insights summary card or modal bottom sheet on the History/Chart screen.

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
│ PHASE 1: Clinical Guidance & Reference Standards (Current Focus)       │
│ 1.1 Medical Organization Guidelines & Stage Badges (Details screen)    │
│ 1.2 "How to Measure" Educational Guide (Contextual dialogs & sheet)    │
│ 1.3 Condition Breakdown Statistics (Arm, Meal, Posture insights)       │
├────────────────────────────────────────────────────────────────────────┤
│ PHASE 2: Data Visualization, Filtering & Doctor Reporting              │
│ 2.1 Chart Normal Ranges / Target Bands (Vico charts)                   │
│ 2.2 History List Filtering (All, Elevated/Abnormal, Conditions)        │
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
2. **Measurement Guide ("How to Measure"):**
   * Can be combined with the guidelines into a unified "Reference & Guide" sheet/page, plus quick `(?)` info helpers on the entry dialogs.
3. **Condition Breakdown Statistics:**
   * Aggregate insights (% left vs. right arm, % sitting vs. standing, meal context, arrhythmia rate) on the History/Chart screen.

#### **Phase 2: Discovery & Doctor Communication**
4. **Chart Target Zones & Stage Coloring (2.1):**
   * Shaded normal bands on Vico line charts using the thresholds established in Phase 1.
   * Stage-colored data points (dots) and stage-tinted tooltip callouts when tapping points.
5. **History List Filtering (2.2):**
   * Filter entries by status (*All*, *Elevated/High*, *Specific Condition*).
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

#### Milestone 1.2: Educational "How to Measure" Guide
* **Goal:** Guide users on proper clinical measurement protocols to eliminate inaccurate home readings.
* **Step 1 — Guidance Content Definition:**
  * Standardized clinical protocols for each type (rest 5 mins seated before BP, cuff on bare arm at heart level, no speaking, fasting interval for blood sugar, morning and barefoot weight).
* **Step 2 — Contextual `(?)` Helper in Entry Dialog:**
  * Add an info icon in `NewEntryDialog` title bar that opens a dialog or bottom sheet explaining how to measure that specific type accurately.
* **Step 3 — Reference Section:**
  * Add an expandable "Measurement Protocol" card within the Details reference section.
* **Step 4 — Automated Tests:**
  * Add unit and instrumented tests for dialog info button interaction.

#### Milestone 1.3: Condition Breakdown Statistics
* **Goal:** Present aggregate percentages of measurements taken under various clinical conditions over the selected chart period.
* **Step 1 — Aggregation Calculation:**
  * Pure calculation functions in `PeriodStatsCalculation.kt` calculating percentages:
    * Arm distribution: % Left vs % Right.
    * Posture distribution: % Sitting vs % Lying vs % Standing.
    * Meal context: % Before vs % After meal.
    * Arrhythmia detection frequency.
* **Step 2 — Unit Tests:**
  * Comprehensive tests in `PeriodStatsCalculationTest.kt` verifying rounding and empty/mixed data sets.
* **Step 3 — UI Presentation:**
  * Add an "Insights" section / card below the Period Card in `HistoryContent.kt`.
* **Step 4 — Instrumented Tests:**
  * Verify in `ChartPageTest.kt` that condition percentages update when switching chart periods.

---

### Phase 2: Discovery & Doctor Communication

#### Milestone 2.1: Chart Target Zones & Stage-Colored Data Points
* **Goal:** Display shaded normal target bands, stage-colored data points, and stage-tinted tooltip callouts on Vico line charts.
* **Step 1 — Target Band Decoration (`ui/history/chart/vico/`):**
  * Use Vico's threshold range decorations / shaded line ranges matching the normal guideline thresholds established in Phase 1 (e.g. green zone for systolic 90–129 mmHg and diastolic 60–84 mmHg, fasting glucose 70–99 mg/dL).
* **Step 2 — Dynamic Bounds per Metric:**
  * Dynamically assign target band Y-ranges based on current `MeasurementType` (e.g. 70–99 mg/dL for blood sugar, 60–100 bpm for pulse, 18.5–24.9 for BMI).
* **Step 3 — Stage-Colored Data Points & Tooltip Badge:**
  * Dynamically tint individual chart data points (dots) using their clinical stage color (e.g. green for normal, orange for high-normal/prediabetes, red for hypertension/diabetes).
  * Display the clinical stage chip inside the tap tooltip / detail callout bubble alongside the value, timestamp, and clinical conditions.
* **Step 4 — Visual Testing & Verification:**
  * Test rendering with different chart periods and data sets in `ChartPageTest.kt`.

#### Milestone 2.2: History List Filtering
* **Goal:** Allow users to filter the history list to quickly find abnormal or specific readings.
* **Step 1 — Filter State Model (`ui/history/`):**
  * Define `HistoryFilter`: `All`, `FlaggedHigh` (Stage 1/2 or Crisis), or `Condition(MealContext/Arm/Posture)`.
* **Step 2 — ViewModel Filtering Pipeline:**
  * Expose filtered `StateFlow<List<Measurement>>` in `HistoryViewModel` reacting to the selected filter chip.
* **Step 3 — Filter Chips UI (`ui/history/`):**
  * Horizontally scrollable `FilterChip` row placed above `EntryList`.
* **Step 4 — Empty State & Automated Tests:**
  * Handle and test empty filter results (e.g. "No high readings in this period") in `HistoryContentTest.kt`.

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

