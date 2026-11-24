# HealthLog

HealthLog is an Android app for recording everyday health measurements (blood pressure, pulse,
blood sugar, weight and height) and looking back at them in a history and in charts.
The app is available in Greek and English.

## Features

- **Five measurement types:** blood pressure (systolic, diastolic and pulse), pulse, blood sugar,
  weight and height
- **Clinical measurement context:** single-choice compact icon chips to specify arm (left/right),
  body position (sitting, lying down, standing), arrhythmia presence (yes/no), and meal context
  (before meal, 2h after meal)
- **Automatic or custom date and time:** records current time by default or allows picking a custom
  date and time via Material 3 date and time pickers
- **History per measurement type:** one page per type, chosen with tabs or by swiping left and
  right; newest first. Each entry is a card with its date and time, its value, a "Details"
  button to view the full entry, and a bin to delete it. The last selected type is remembered
  while the app is open
- **History list filtering & quick discovery:**
  - **Space-saving FilterBar:** slim 40dp top bar placed directly above the entry list leaving 85%+ of the screen clear for measurement cards. Shows an active filter badge (`Filter (N)`), active date chip, stage chips with clinical badge colors, condition chips with multi-color icons, and a quick one-tap `Reset` action.
  - **Filter bottom sheet with deferred apply:** taps inside the sheet remain in draft state until the user clicks the confirmation button, keeping the background list undisturbed while browsing.
  - **Multi-stage clinical filtering:** filter by medical classification (e.g. Underweight, Normal, Overweight, Obese, Grade 1/2 Hypertension, Tachycardia, etc.) with support for multi-stage selection within a metric.
  - **Physical condition filtering:** filter by Arm (Left/Right), Body Posture (Sitting/Standing/Lying), Meal context (Before/After), and Arrhythmia (`Yes` / detected, `No` / not detected).
  - **Date range filtering & picker dialog:** rolling presets (`All Time`, `Today`, `Last 7 Days`, `Last 30 Days`) plus custom range selection via Material 3 `DateRangePicker` dialog with optimized localization for English and Greek.
  - **Live matching results counter:** dynamic confirmation button displaying real-time matching results (`Show all (52)`, `Show 12 entries`, `No matching entries (0)`) before committing.
  - **Adaptive & rotation resilient:** fully scrollable in landscape mode, with uncommitted draft selections preserved across device rotation via custom bundle-safe state savers.
- **Measurement details screen:** a dedicated screen displaying a single measurement in full detail:
  type-themed header, prominent values, absolute and relative timestamp, and compact clinical
  condition tags with icons (arm, posture, meal context, arrhythmia). Delete with confirmation
  is also supported directly from details. The screen is structured to easily host future medical
  guideline tables and stage classifications
- **Interactive chart stats:** tapping the highest, lowest or last entry stat cards on the chart
  page navigates directly into that measurement's details
- **Medical reference standards & clinical stage badges:**
  - **Stage badges:** prominent, color-coded stage badge displayed directly beneath the reading card
    for immediate classification.
  - **International standards:**
    - **Blood Pressure:** International Society of Hypertension 2020 (ISH 2020) classification (Low,
      Normal, High-Normal, Grade 1 Hypertension, Grade 2 Hypertension, Hypertensive Crisis) paired
      with resting pulse evaluation.
    - **Blood Glucose:** World Health Organization & International Diabetes Federation (WHO / IDF)
      clinical thresholds correlated with meal timing (Hypoglycemia, Normal, Prediabetes, Diabetes
      for Fasting and 2h Post-meal).
    - **Pulse:** World Heart Federation & WHO consensus resting heart rate zones (Bradycardia, Normal,
      Tachycardia) with athlete resting pulse footnote.
    - **Weight & BMI:** World Health Organization (WHO) Body Mass Index (Underweight, Normal,
      Overweight, Obese) alongside Oxford University "New BMI" ($1.3 \times \text{weight} / \text{height}^{2.5}$),
      healthy weight target range ($18.5 \times H^2 .. 24.9 \times H^2$) with delta, and WHO Asian
      population risk consultation footnote.
  - **Visual gauges & scales:** discrete segmented meters with active glow callout for blood pressure,
    glucose, and pulse; continuous spectrum gauge with needle pointer for standard BMI and Oxford New BMI.
  - **Inline reference tables:** clean, expandable reference tables placed directly below each metric's
    gauge scale, highlighting the active tier and criteria.
- **Educational "How to Measure" measurement guides:**
  - **Contextual access:** tap the `(?)` info button on any entry dialog's title bar to open a dedicated,
    full-screen guide on how to take an accurate clinical reading for that specific vital sign.
  - **Conversational doctor persona:** a friendly vector doctor avatar (`ic_doctor_guide.xml`) speaks
    directly to the user through speech bubble clouds (`LeftSpeechBubbleShape`), breaking the monotony of
    plain clinical text.
  - **Structured two-cloud guidance:** Cloud 1 contains the numbered, step-by-step preparation checklist;
    Cloud 2 highlights essential clinical tips and rules (e.g. 5-min rest, cuff at heart level, avoiding
    caffeine/exercise/smoking, morning fasting, proper barefoot posture).
  - **Evidence-based protocols:** clinically verified guidance based on ISH 2020, WHO, ADA, WHF, and CDC
    standard measurement procedures.
  - **Non-disruptive navigation:** pressing the back button `←` returns directly to the open entry dialog
    with all typed values and clinical selections preserved.
- **Blood pressure includes the pulse:** saving a blood pressure also saves its pulse as a
  separate Pulse measurement, so it appears in both histories. The two measurements are
  independent: deleting one keeps the other
- **Interactive Vico charts & clinical target zones:**
  - **Horizontal target bands:** shaded clinical bands rendered across the chart background for **Pulse** (Bradycardia <60 bpm, Normal 60–100 bpm, Tachycardia >100 bpm) and **Weight** (WHO BMI categories: Underweight, Normal, Overweight, Obese, dynamically computed from the user's latest height).
  - **Calibrated vivid alert opacities:** alert zones (Red Tachycardia/Obesity and Orange Overweight) use calibrated vivid fills and strokes to prevent washed-out or pinkish appearances over light canvas backgrounds.
  - **Isolated neutral canvas:** wrapped in a dedicated `surfaceContainerHighest` card canvas (`RoundedCornerShape(16.dp)`) preventing card tint bleed from the surrounding theme.
  - **Contextual dot shapes & dynamic stage coloring:**
    - **Blood Pressure:** Systolic Circle and Diastolic Diamond dots dynamically tinted by ISH stage color (Low, Normal, High-Normal, Grade 1, Grade 2, Crisis).
    - **Blood Sugar:** Fasting/Before-meal Circle and Postprandial Square dots dynamically tinted by WHO category (Hypoglycemia, Normal, Prediabetes, Diabetes).
    - **Pulse & Weight:** Dynamic point providers tinting dots by resting heart rate zone and WHO BMI category matching target bands and reference tables.
    - **High-contrast halos:** 1.5 dp surface halos on every point to guarantee high contrast over lines and target bands.
  - **Stage-tethered interactive tooltips:** tapping any point reveals an interactive tooltip bubble with exact timestamp, values, clinical conditions, and a stage chip dynamically tinted in the pinpoint's exact condition color.
  - **Chart shape legends:** clean, accessible legend directly below Blood Pressure (Systolic Circle / Diastolic Square) and Blood Glucose (Before meal Circle / 2h after meal Square) charts.
  - **Mode toggle:** a "See Chart" / "See Entries" button switches the history between the list and a line chart over time.
- **Doctor Report Export (PDF & CSV to Downloads):**
  - **Quick access:** dedicated download action button (`ic_download.xml`) in the History top app bar.
  - **Export options bottom sheet:** clean modal dialog allowing configuration of format (**Doctor Report PDF** or **Spreadsheet CSV**), measurement scope (**Current metric** or **All metrics**), and date span (**Last 7 Days**, **Last 30 Days**, **All Time**, or **Pick Dates** via custom date range picker).
  - **Live matching entries counter & validation:** dynamically computes the matching records count and disables the download button when 0 entries match the criteria.
  - **Native A4 clinical PDF report:**
    - Formatted with standard A4 margins (595 × 842 pt) and clean clinical typography.
    - **Header:** HealthLog branding, title, generated timestamp, metric scope, and date range subtitle.
    - **Summary statistics box:** total entries count, clinical average, lowest, and highest readings.
    - **Trajectory mini-graph:** smooth vital signs trajectory chart with shaded canvas and min/max markers.
    - **Chronological table:** Date, Time, formatted values (e.g. `120/80 (72 bpm)`), clinical guideline stage evaluations with stage color badges, and physical condition tags (Arm, Body Posture, Meal Context, Arrhythmia).
    - **Pagination:** automatic multi-page detection with repeating headers and bottom page numbers (`Page X`).
  - **Raw CSV spreadsheet export:**
    - RFC 4180 compliant CSV stream containing all readings, pulse, units, clinical stages, and condition tags.
    - Encoded with UTF-8 Byte Order Mark (BOM: `EF BB BF`) for instant compatibility with Microsoft Excel without mojibake/garbled text.
  - **Direct Downloads save:** saves exports directly into the public `Downloads/HealthLog/` device folder via `MediaStore.Downloads` without intrusive system share sheets (no third-party Gmail, Messages, or Quick Share popups).
  - **Instant completion feedback:** shows a concise finish snackbar upon download completion in English and Greek.
- **Chart periods:** 7 days, 1 month, 6 months, 1 year or all, as chips in one rounded track
- **Period card:** the chart and its stats in one rounded card: entries and average above the
  chart, highest and lowest below it (for blood pressure, the highest and lowest systolic and
  diastolic with their pairs), each in its own box with an icon. An empty period shows only a
  message
- **Condition breakdown statistics & clinical insights:**
  - **Aggregate condition metrics:** on the chart page, a dedicated `ConditionInsightsCard` summarizes
    the distribution of readings across clinical conditions for the selected period (e.g. % Left vs. % Right
    arm, % Sitting vs. % Lying vs. % Standing posture, % Before vs. % After meal).
  - **Visual distribution bars:** clean progress indicator bars show proportional fills with count and percentage
    breakdowns for each condition.
  - **Arrhythmia frequency tracking:** displays total readings with detected irregular heartbeat and percentage
    over the period.
  - **Conversational doctor clinical advice:** when meaningful condition patterns or repeated arrhythmia occurrences
    are detected (with strict false-positive prevention thresholds), contextual doctor advice is displayed directly
    beneath the relevant section in a talking speech bubble (`LeftSpeechBubbleShape`) featuring a serious, attentive
    doctor avatar (`ic_doctor_warning.xml`).
  - **Smart visibility:** hidden automatically for metrics without recorded clinical conditions (weight and height)
    or when a period has no entries.
- **Last entry card:** below the period card, whatever the period: the last entry's value and when
  it was recorded (e.g. "3 hours ago", "yesterday", "2 months ago") with its date and time
- **Global 3-tab Bottom Navigation:** Material 3 `NavigationBar` seamlessly switching between:
  - **Vitals:** health measurements logging, history per type, and Vico charts.
  - **Medications:** dedicated "My Medicines" prescription management, dosage, reminder configurations, and one-time medication logging.
  - **Calendar:** interactive monthly health tracker, day summary with actionable doses, and full 24-hour expandable day timeline.
  - State preservation across tabs: scrolling position, sub-tabs, and form drafts are cleanly preserved.
- **Health Calendar & 24-Hour Day Timeline:**
  - **Interactive monthly grid:** month overview with weekday headers, previous/next chevron navigation, and a clickable month/year header with a 4×3 grid dialog to jump to any month and year.
  - **Today quick-jump:** one-tap top bar action returning instantly to the current month and today's date.
  - **Category indicator dots:** distinct dots under each calendar day matching the legend (🟢 Vitals, 🟠 Symptoms, 🔵 Medications taken), preserved with high contrast on selected days.
  - **Day summary card:** selecting a calendar day displays its quick event summary with scheduled medication doses, allowing instant Take, Skip, Move Time, and Delete actions, plus a "Full Day" button to open the detailed timeline.
  - **24-Hour Day Timeline Screen (`DayTimelineScreen`):**
    - **Hour-by-hour view (00:00 to 23:00):** chronologically groups all daily events (vitals, symptoms, medication doses) into hour slots. Empty slots can be collapsed or expanded, with a `+` action on any slot to log a symptom or medication directly at that specific hour.
    - **Interactive medication dose cards:** displays medication name, dosage, scheduled time, and status badge (Pending, Taken, Skipped). Users can mark as Taken or Skipped directly. Recurring doses support Undo (reverting status to Pending). One-time doses hide the Undo button and show a direct Delete (trash bin) action.
    - **Move Time dialog:** reschedule a dose to a new hour and minute, with a scope dialog for recurring medications ("This day only" vs. "This and all future days").
    - **Delete Dose dialog:** remove a scheduled dose, with scope selection for recurring medications ("This day only" vs. "This and all future days").
    - **Read-only vital cards:** formatted measurement values, timestamps, and multi-colored metric icons (`MeasurementType.icon`), filtering out companion pulse records from blood pressure measurements. Tapping navigates to full measurement details.
    - **Interactive symptom cards:** symptom name, severity badge (1–5), timestamp, and notes, with inline Edit (modal sheet) and Delete (confirmation dialog) actions.
    - **Timeline filter chips:** filter events dynamically by All, Vitals, Medications, or Symptoms with real-time event counts.
    - **Day-to-day navigation:** previous/next day chevrons and a "Today" quick-jump action in the top app bar.
  - **Symptom logging modal sheet:** quick-access sheet to log symptoms with predefined chips (Headache, Dizziness, Palpitations, Chest Discomfort, Shortness of Breath, Fatigue, Nausea) or custom symptom input, live 1–5 severity slider with descriptive badges, Material date/time picker, and optional notes.
- **Medication tracking & offline local reminders:**
  - **My Medicines:** unified single-screen management of active recurring prescriptions and as-needed / one-time medications.
  - **Soft-delete history preservation:** deleting a prescription removes it from future schedules and stops alarms, but preserves all past intake logs (`isDeleted = true`) in the calendar and timeline history.
  - **Medication Schedule Resolver:** computes daily scheduled doses based on start/end date bounds, recurrence frequency, and per-day overrides.
  - **As-needed & one-time medication logging:** dedicated quick-entry flow for symptomatic or ad-hoc medication intake directly from prescriptions or the form sheet.
  - **Offline AlarmManager notifications:** scheduled via Android `AlarmManager` (`SCHEDULE_EXACT_ALARM`) with high-priority heads-up notifications and instant *Mark as Taken* actions, requiring zero cloud services or battery-draining polling.
  - **Reboot resilience:** `BootReceiver` automatically reschedules all active medication alarms on device boot (`ACTION_BOOT_COMPLETED`).
  - **Add/edit medication modal sheet:** customizable name, dosage, measurement units (mg, ml, tablet, drops, puff, g, mcg, patch, unit), daily reminder time pickers, frequency selection, doctor instructions, and seamless Android 13+ `POST_NOTIFICATIONS` permission request & resume handling.
- **A color per type and fixed brand identity:** red for blood pressure, pink for pulse, orange for blood sugar, green for
  weight and blue for height. Each type's history page, home card and entry dialog are in its
  color, in light and dark mode. The app's overall theme is built around a dedicated brand pink
  (`BrandSeedColor`), matching the app icon across all devices
- **Brand identity & icon:** custom adaptive app icon (ring, heart outline, heartbeat line)
  with support for Android 13+ themed icons. The home screen features a polished top bar with
  the brand icon and a two-tone "HealthLog" title
- **Own icons:** each type, each stat and the buttons have their own multicolored icons
- **Delete with confirmation:** the bin on a card asks before deleting
- **Input validation & feedback:** every value is checked against a range with inline messages.
  Tapping Save or completing numeric inputs highlights omitted required option rows in red with
  subtle borders and soft tinting, persisting until selected. Saving is enabled only when all
  numeric fields and required options are complete and valid
- **Rotation-safe:** typed values, dialogs and errors survive screen rotation
- **Greek and English:** the app follows the phone's language (English for any other language);
  on Android 13+ it can also be set per app. Numbers and dates follow the language
  (72,50 kg and 27/10/2026 18:41 in Greek, 72.50 kg and Oct 27, 2026, 6:41 PM in English)

## Tech stack

- [Kotlin](https://kotlinlang.org/)
- [Jetpack Compose](https://developer.android.com/compose) with Material 3
- [Room](https://developer.android.com/training/data-storage/room) for local storage
- [Navigation Compose](https://developer.android.com/develop/ui/compose/navigation)
- [Vico](https://github.com/patrykandpatrick/vico) for the charts
- [MaterialKolor](https://github.com/jordond/materialkolor) for the type colors: a full Material
  color scheme from one color per type
- Android `AlarmManager` and `BroadcastReceiver` for offline medication reminders and reboot rescheduling
- MVVM: ViewModels with `StateFlow`, and a repository between the ViewModels and the database

## Project structure

All source code is in `app/src/main/java/com/example/healthlog/`:

```
healthlog/
├── database/                Room entities (Measurement, Medication, Reminder, DoseLog), DAO, migrations
├── repository/              MeasurementRepository and MedicationRepository, data access points
├── model/                   MedicationScheduleResolver, ScheduledDoseItem, measurement types & validation
├── guidelines/              clinical evaluation logic & medical standards (ISH, WHO, WHF, Oxford)
│   └── protocols/           evidence-based clinical measurement protocols & doctor persona visual
├── notifications/           MedicationAlarmScheduler, MedicationAlarmReceiver, and BootReceiver
└── ui/                      navigation (HealthLogNavHost, AppBottomBar, NavDestination)
    ├── home/                home screen: the type picker cards, with their icons
    │   └── newentry/        adding an entry: the new-entry dialog, its state and inputs
    ├── history/             history screen: the type tabs, the list/chart switch, deleting
    │   ├── list/            the list mode: the entry cards
    │   ├── filter/          history filtering: FilterBar, FilterBottomSheet, and filter criteria
    │   └── chart/           the chart mode: the chart page, the period card, the last entry card,
    │       │                periods, screen taps
    │       ├── stats/       the stats in the period card: calculations, texts, the stat boxes
    │       └── vico/        the chart itself, its tooltip and axes (the only place Vico is used)
    ├── details/             measurement details screen: single-entry breakdown, clinical context tags
    │   └── references/      clinical reference cards, visual gauge meters, and stage badges
    ├── medications/         medications screen: My Prescriptions, Add/Edit sheet, one-time meds,
    │                        dose status actions and dialog state
    ├── calendar/            calendar & timeline: CalendarScreen, DayTimelineScreen, DoseActionDialogs,
    │   │                    MonthHeader, MonthYearPickerDialog, CalendarDayCell, AddEditSymptomSheet
    │   └── timeline/        24-hour hour-by-hour timeline: DayTimelineContent, timeline filter chips,
    │                        hour slots, slot chooser dialog, interactive dose & symptom cards
    ├── export/              doctor reports & data export: ExportDialog, PdfReportGenerator,
    │                        CsvReportGenerator, ExportFileManager, ExportConfig
    ├── format/              display text: values, dates, "time ago", messages, per-language formats
    └── theme/               colors, typography and theme, plus the type colors (TypeTheme)
                             and the type icons (TypeIcon)
```

Each screen is split into a **Screen** (connects the ViewModel to the UI) and a **Content**
(layout only, previewable in Android Studio).

The Home screen provides direct entry creation from each metric card without a redundant floating action button.
In the History screen, tab switching and page swipes are decoupled so swipe transitions remain fluid, updating
the selected type state only once the gesture settles.

Each type's color scheme comes from `TypeTheme` in `ui/theme/`: wrapping a part of the screen in
`TypeTheme(type)` gives it that type's colors, so its cards, chips, buttons and chart follow by
themselves. The seed color of each type and the style of the schemes are set there.
The overall app theme (`HealthLogTheme`) uses `BrandSeedColor` as its seed color, providing a consistent brand
appearance across light and dark modes.

The icons in `app/src/main/res/drawable/` were drawn for HealthLog. Most are multicolored, so
they are shown with `tint = Color.Unspecified` to keep their colors. The app launcher icon and in-app brand logo
(`ic_brand_logo.xml`) represent the core identity.

Dedicated vector icons illustrate every clinical option and dialog action:
- **Measurement Arm:** `ic_arm_left.xml`, `ic_arm_right.xml`
- **Body Position:** `ic_position_sitting.xml`, `ic_position_lying.xml`, `ic_position_standing.xml`
- **Arrhythmia Presence:** `ic_arrhythmia_no.xml`, `ic_arrhythmia_yes.xml`
- **Meal Context:** `ic_meal_before.xml`, `ic_meal_after.xml`
- **Filtering Actions:** `ic_filter.xml` (filter action and badge), `ic_close.xml` (chip dismissal)
- **Conversational Doctor Avatars:** `ic_doctor_guide.xml` (friendly educational guide avatar), `ic_doctor_warning.xml` (serious, attentive avatar with alert badge for clinical warnings)
- **Custom Date & Time:** `ic_calendar.xml`
- **Export & Download Actions:** `ic_download.xml` (top app bar export action), `ic_format_pdf.xml` (doctor PDF report badge), `ic_format_csv.xml` (spreadsheet CSV data badge)

These are rendered inside `OptionSelectorGroup` as compact, single-choice chips with icon presentation and accessible content descriptions, keeping the entry dialog uncluttered and easy to tap.

### Naming: measurement and entry

- A **measurement** is the values themselves, e.g. 130/80 mmHg with a pulse of 72, taken at a
  certain time. It is what gets stored, so the data side uses this word: `Measurement`,
  `MeasurementType`, `MeasurementRepository`, `validateMeasurement`, and code that works on the
  values, such as `formatMeasurementValue` and the stats.
- An **entry** is how the screens show a measurement: the entry cards, the entries list, the
  new-entry dialog. So these components are named `Entry…` (`EntryCard`, `EntryList`,
  `NewEntryDialog`, `NewEntryState`, `EntryFieldInput`, `EntryChart`), and the texts say "entry".

So a line like `EntryCard(measurement: Measurement)` reads as "an entry card showing a measurement".

All displayed texts are in `app/src/main/res/values/strings.xml` (English, the default)
and `app/src/main/res/values-el/strings.xml` (Greek). Each language also defines its own
decimal separator and date format there.

## Requirements and setup

- Android Studio (a recent version)
- Android 8.0 (API 26) or newer on the device or emulator (`minSdk = 26`)

1. Clone the repository
2. Open the project folder in Android Studio and let Gradle sync
3. Run the `app` configuration on an emulator or a device

## Tests
There are two kinds of tests:

**Unit tests** (`app/src/test`, run on your computer):
- `MeasurementValidationTest`: the validation rules
- `MeasurementTypeTest`: the rules of the measurement types (stored names, fields, ranges)
- `MeasurementContextTest`: icon and label bindings for clinical context enums and safe parsers
- `NumberInputTest`: what can be typed into number fields
- `NewEntryStateTest`: the new-entry dialog's state and rules
- `MeasurementFormattingTest`: how measurement values are shown (units, decimals, separator)
- `DateFormattingTest`: the date format of each language
- `ValidationMessagesTest`: how range limits are shown in error messages
- `MeasurementRepositoryTest`: what is stored when a measurement is saved and ID lookups
- `ChartPeriodTest`: which measurements each chart period includes
- `TargetRangeBandTest`: clinical target band ranges, dynamic BMI height calculations, point providers (Pulse, Weight, Glucose, Blood Pressure), and stage color resolution
- `PeriodStatsCalculationTest`: count, highest, lowest, average, and condition breakdowns (arm, posture, arrhythmia, meal context distributions and balanced clinical advice rules)
- `MeasurementProtocolTest`: evidence-based clinical preparation checklist and tips per vital sign
- `HistoryFilterTest`: clinical stage filtering, physical conditions, rolling and custom date ranges, compound matching (OR/AND), and rotation state saver serialization
- `ExportConfigTest`: export configuration models, metric scope filtering, and chronological sorting
- `ExportConfigPdfTest`: PDF export configuration defaults and date range filtering
- `CsvReportGeneratorTest`: RFC 4180 CSV escaping (commas, quotes, newlines) and multi-metric filtering
- `TimeAgoTest`: the "time ago" of the last measurement (hours, yesterday, days, months, years)
- `MedicationScheduleResolverTest`: daily schedule resolution, interval calculations, start/end date bounds, per-day move/delete overrides, and soft-delete dose history preservation
- `MedicationsViewModelTest`: active prescriptions list, add/edit sheet open/close state, one-time medications, and delete confirmation modal state
- `MedicationAlarmSchedulerTest`: exact alarm computation, intent extras, reminder date bounds, and cancellation logic
- `MedicationRepositoryTest`: database queries for medications, reminders, soft-delete dose history preservation, and dose logs
- `SymptomDomainTest`: symptom type enum matching and 1–5 severity level mapping with graceful fallbacks
- `SymptomRepositoryTest`: symptom insertion, updates, range queries, and ID deletion
- `CalendarAggregatorTest`: monthly event aggregation, ISO date string parsing, companion pulse deduplication from blood pressure, and multi-metric clinical rank evaluation
- `CalendarViewModelTest`: month navigation, Month/Year dialog selection with day-clamping, jump to today, scheduled doses resolution, and symptom CRUD workflows
- `DayTimelineViewModelTest`: 24-hour hour slot grouping, event filtering (All, Vitals, Medications, Symptoms), move/delete dose scoping, and slot-directed logging
- `LocalizationParityTest`: programmatic verification of 100% string and plural parity between `values/strings.xml` and `values-el/strings.xml`

**Instrumented tests** (`app/src/androidTest`, run on an emulator or device):
- `MigrationTest`: the database migrations (including version 4 → 5 with medications, reminders, dose logs, and symptoms tables)
- `NewEntryDialogTest`: the new-entry dialog's UI
- `HistoryContentTest`: the history's tabs, swiping, empty pages, entry details navigation, deleting, the See Chart
  button and the period chips
- `HistoryFilterUiTest`: history list filtering, FilterBar toggling, bottom sheet opening, deferred apply on Done, individual chip removal, quick reset, and dynamic live result count
- `ExportDialogUiTest`: export bottom sheet format selection (PDF/CSV), metric scope (Current/All), date range chips, custom date picker dialog, dynamic action validation, and dismiss handling
- `ExportGeneratorsDeviceTest`: on-device creation and validation of standard A4 PDF documents (`%PDF` magic header), RFC 4180 CSV files with UTF-8 BOM, and MediaStore public Downloads insertion
- `ChartPageTest`: the chart page: the period card (chart or empty message, stats), contextual shape legends, condition breakdown statistics and clinical insights, and the last entry card
- `MeasurementDetailsScreenTest`: the details screen layout, clinical condition tags, back navigation and deletion
- `MeasurementGuideScreenTest`: the educational guide screen, tabs, doctor speech bubble cards, and back navigation
- `MedicationsUiTest`: medication management UI: My Prescriptions list, one-time medication chips, and add/edit medication sheet validation
- `CalendarUiTest`: health calendar UI: month navigation, Month/Year picker grid dialog, Today quick-jump, day summary with scheduled doses (Take, Skip, Move, Delete), symptom creation sheet, custom name validation, and deletion dialog
- `DayTimelineUiTest`: 24-hour day timeline UI: header navigation and Today jump, interactive medication take/skip actions, one-time dose delete action without undo, move/delete scope dialogs, symptom sheet integration, and Greek localization parity
- `BottomNavigationUiTest`: bottom navigation bar tab switching (Vitals, Medications, Calendar) and placeholder screen verification

To run them:

- **Android Studio:** right-click the test file or folder → **Run**
- **Terminal:** `./gradlew testDebugUnitTest` (unit tests), `./gradlew connectedDebugAndroidTest` (instrumented tests)

**Note:** after instrumented tests, Gradle uninstalls the app from that device, including its data.
Run them on a separate emulator, not on a device with data you want to keep.

## Database

Measurements are stored locally with Room. The database is currently at **version 5**.

### Schema versions & migrations

- **Version 1:** Initial database storing `id`, `type`, `value`, `secondaryValue`, `timestamp`.
- **Version 1 → 2 (`MIGRATION_1_2`):** Added nullable `tertiaryValue` column to store pulse for blood pressure measurements.
- **Version 2 → 3 (`MIGRATION_2_3`):** Converted stored Greek type names to enum constant strings (`BLOOD_PRESSURE`, `PULSE`, `BLOOD_SUGAR`, `WEIGHT`, `HEIGHT`).
- **Version 3 → 4 (`MIGRATION_3_4`):** Added nullable clinical context columns (`hasArrhythmia` INTEGER, `mealContext` TEXT, `arm` TEXT, `bodyPosition` TEXT). Older rows keep their data and have `null` for these columns.
- **Version 4 → 5 (`MIGRATION_4_5`):** Added medication management and symptom tracking tables:
  - `medications`: stores active, archived, and soft-deleted prescriptions (`id`, `name`, `dosage`, `unit`, `frequency`, `instructions`, `isActive`, `isDeleted`, `createdAt`).
  - `medication_reminders`: stores recurring daily reminder alarms (`id`, `medicationId`, `hour`, `minute`, `daysOfWeekMask`, `isEnabled`, `startDate`, `endDate`) with cascading foreign key delete on `medicationId`.
  - `medication_dose_logs`: stores intake history and overrides (`id`, `medicationId`, `reminderId`, `scheduledDate`, `scheduledTime`, `status`, `actionTimestamp`).
  - `symptoms`: stores logged symptoms (`id`, `symptomType`, `customName`, `severity`, `timestamp`, `notes`) with an index on `timestamp` for fast date-range queries.

Any change to what is stored needs a database migration in `HealthDatabase.kt`, for example:
- adding, removing or renaming a column in `Measurement`
- renaming or removing a `MeasurementType` constant (its name is what is stored)
- reordering the fields of a measurement type (their position decides the column they are stored in)

Changes to labels, units, ranges or the UI do not need a migration.

Room writes a JSON description of every database version into `app/schemas/` (committed to Git).
The migration tests use these files to create databases of older versions.
`1.json` and `2.json` were reconstructed from `3.json`, since schema export was only enabled at version 3.

When changing the database:
1. increase `version` in `HealthDatabase.kt` and add a migration
2. build the project, which creates the new schema file in `app/schemas/`
3. add a test for the new migration in `MigrationTest`

## Build Variants & Performance Testing

The project defines three build types:
- **`debug`:** default debuggable variant used during day-to-day development.
- **`release`:** full R8 optimization and code shrinking enabled for production.
- **`benchmark`:** mirrors the release configuration (full R8 optimization and shrinking) but is signed with the debug keystore. This allows testing real production performance and speed on any device without requiring a release signing key.

To assemble the benchmark build:
```bash
./gradlew :app:assembleBenchmark
```

## Roadmap

HealthLog follows an offline-first, privacy-by-design architecture with zero server operating costs.
For detailed specifications and architectural decisions, see the complete [Product Roadmap](docs/ROADMAP.md).

### Phase 1: Clinical Guidance & Reference Standards (Completed)
- [x] Dedicated measurement details screen with clinical condition tags and interactive chart navigation
- [x] Medical organization guidelines & reference tables on details screen (ISH 2020 blood pressure stages, WHO/IDF glucose ranges, WHO BMI, resting pulse zones)
- [x] Educational "How to Measure" guide (contextual helpers on entry dialogs and reference sheets)
- [x] Condition breakdown statistics (aggregate % of measurements taken under various conditions: arm, meal context, posture, arrhythmia rate)

### Phase 2: Data Visualization, Filtering & Doctor Reporting (Completed)
- [x] Charts: normal target ranges, shaded clinical bands, contextual dot shapes, and stage coloring on Vico line charts (Completed)
- [x] History list: filtering for out-of-range / flagged values and clinical conditions (Completed)
- [x] Doctor reporting: on-device PDF generation, printing, and CSV export for medical appointments (100% local and private) (Completed)

### Phase 3: Medications & Symptom Calendar (Completed)
- [x] Global 3-tab Bottom Navigation (Vitals, Medications, Calendar) (Completed)
- [x] Medication tracking and offline local reminders (AlarmManager exact alarms & BootReceiver) (Completed)
- [x] Symptom logging and clinical correlation calendar (Completed)
- [x] Unified 24-hour Day Timeline, single-view My Medicines, and schedule overrides resolver (Completed)
*(Note: Per-measurement free-text notes were dropped/superseded in favor of discrete symptoms and medication adherence records).*

### Phase 4: Local Profiles & Data Portability
- [ ] Local multi-user profiles (device profile switcher with guest-first experience)
- [ ] Data backup & restore (encrypted export to personal storage or Google Drive)
- [ ] Wider automated test coverage
