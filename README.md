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
- **Blood pressure includes the pulse:** saving a blood pressure also saves its pulse as a
  separate Pulse measurement, so it appears in both histories. The two measurements are
  independent: deleting one keeps the other
- **Charts per measurement type:** a "See Chart" / "See Entries" button switches the history
  between the list and a line chart over time (blood pressure: systolic and diastolic). Tapping a
  point shows its value, date and time
- **Chart periods:** 7 days, 1 month, 6 months, 1 year or all, as chips in one rounded track
- **Period card:** the chart and its stats in one rounded card: entries and average above the
  chart, highest and lowest below it (for blood pressure, the highest and lowest systolic and
  diastolic with their pairs), each in its own box with an icon. An empty period shows only a
  message
- **Last entry card:** below the period card, whatever the period: the last entry's value and when
  it was recorded (e.g. "3 hours ago", "yesterday", "2 months ago") with its date and time
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
- MVVM: ViewModels with `StateFlow`, and a repository between the ViewModels and the database

## Project structure

All source code is in `app/src/main/java/com/example/healthlog/`:

```
healthlog/
├── database/                Room entity (Measurement), DAO, database and migrations
├── repository/              MeasurementRepository, the single access point to the data
├── model/                   measurement types (fields, units, ranges) and validation rules
├── guidelines/              clinical evaluation logic & medical standards (ISH, WHO, WHF, Oxford)
└── ui/                      navigation (HealthLogNavHost)
    ├── home/                home screen: the type picker cards, with their icons
    │   └── newentry/        adding an entry: the new-entry dialog, its state and inputs
    ├── history/             history screen: the type tabs, the list/chart switch, deleting
    │   ├── list/            the list mode: the entry cards
    │   └── chart/           the chart mode: the chart page, the period card, the last entry card,
    │       │                periods, screen taps
    │       ├── stats/       the stats in the period card: calculations, texts, the stat boxes
    │       └── vico/        the chart itself, its tooltip and axes (the only place Vico is used)
    ├── details/             measurement details screen: single-entry breakdown, clinical context tags
    │   └── references/      clinical reference cards, visual gauge meters, and stage badges
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
- **Custom Date & Time:** `ic_calendar.xml`

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
- `PeriodStatsCalculationTest`: count, highest, lowest, average and their rounding
- `TimeAgoTest`: the "time ago" of the last measurement (hours, yesterday, days, months, years)

**Instrumented tests** (`app/src/androidTest`, run on an emulator or device):
- `MigrationTest`: the database migrations
- `NewEntryDialogTest`: the new-entry dialog's UI
- `HistoryContentTest`: the history's tabs, swiping, empty pages, entry details navigation, deleting, the See Chart
  button and the period chips
- `ChartPageTest`: the chart page: the period card (chart or empty message, stats) and the last
  entry card
- `MeasurementDetailsScreenTest`: the details screen layout, clinical condition tags, back navigation and deletion

To run them:

- **Android Studio:** right-click the test file or folder → **Run**
- **Terminal:** `./gradlew testDebugUnitTest` (unit tests), `./gradlew connectedDebugAndroidTest` (instrumented tests)

**Note:** after instrumented tests, Gradle uninstalls the app from that device, including its data.
Run them on a separate emulator, not on a device with data you want to keep.

## Database

Measurements are stored locally with Room. The database is currently at **version 4**.

### Schema versions & migrations

- **Version 1:** Initial database storing `id`, `type`, `value`, `secondaryValue`, `timestamp`.
- **Version 1 → 2 (`MIGRATION_1_2`):** Added nullable `tertiaryValue` column to store pulse for blood pressure measurements.
- **Version 2 → 3 (`MIGRATION_2_3`):** Converted stored Greek type names to enum constant strings (`BLOOD_PRESSURE`, `PULSE`, `BLOOD_SUGAR`, `WEIGHT`, `HEIGHT`).
- **Version 3 → 4 (`MIGRATION_3_4`):** Added nullable clinical context columns (`hasArrhythmia` INTEGER, `mealContext` TEXT, `arm` TEXT, `bodyPosition` TEXT). Older rows keep their data and have `null` for these columns.

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

### Phase 1: Clinical Guidance & Reference Standards (In Progress)
- [x] Dedicated measurement details screen with clinical condition tags and interactive chart navigation
- [ ] Medical organization guidelines & reference tables on details screen (AHA/ESC blood pressure stages, ADA glucose ranges, WHO BMI, resting pulse zones)
- [ ] Educational "How to Measure" guide (contextual helpers on entry dialogs and reference sheets)
- [ ] Condition breakdown statistics (aggregate % of measurements taken under various conditions: arm, meal context, posture, arrhythmia rate)

### Phase 2: Data Visualization, Filtering & Doctor Reporting
- [ ] Charts: normal target ranges and shaded zones on Vico line charts
- [ ] History list: filtering for out-of-range / flagged values and clinical conditions
- [ ] Doctor reporting: on-device PDF generation, printing, and CSV export for medical appointments (100% local and private)

### Phase 3: Health Journal & Medication Management
- [ ] Per-measurement contextual notes
- [ ] Medication tracking and local reminders (AlarmManager)
- [ ] Symptom logging and calendar tracker

### Phase 4: Local Profiles & Data Portability
- [ ] Local multi-user profiles (device profile switcher with guest-first experience)
- [ ] Data backup & restore (encrypted export to personal storage or Google Drive)
- [ ] Wider automated test coverage
