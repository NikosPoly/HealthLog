# HealthLog

HealthLog is an Android app for recording everyday health measurements (blood pressure, pulse,
blood sugar, weight and height) and looking back at them in a history and in charts.
The app is available in Greek and English.

## Features

- **Five measurement types:** blood pressure (systolic, diastolic and pulse), pulse, blood sugar,
  weight and height
- **Automatic date and time** for every entry
- **History per measurement type:** one page per type, chosen with tabs or by swiping left and
  right; newest first. Each entry is a card with its date and time, its value and a bin to
  delete it. The last selected type is remembered while the app is open
- **Blood pressure includes the pulse:** saving a blood pressure also saves its pulse as a
  separate Pulse entry, so it appears in both histories. The two entries are independent:
  deleting one keeps the other
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
- **A color per type:** red for blood pressure, pink for pulse, orange for blood sugar, green for
  weight and blue for height. Each type's history page, home card and entry dialog are in its
  color, in light and dark mode. The rest of the app follows the phone's colors (Android 12+)
- **Own icons:** each type, each stat and the buttons have their own multicolored icons
- **Delete with confirmation:** the bin on a card asks before deleting
- **Input validation:** every value is checked against a range; errors are shown under the field
  and saving is only possible when everything is valid
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
└── ui/                      navigation (HealthLogNavHost)
    ├── home/                home screen: the measurement type cards, with their icons
    │   └── newmeasurement/  adding a measurement: the entry dialog, its state and inputs
    ├── history/             history screen: the type tabs, the list/chart switch, deleting
    │   ├── list/            the list mode: the entry cards
    │   └── chart/           the chart mode: the chart page, the period card, the last entry card,
    │       │                periods, screen taps
    │       ├── stats/       the stats in the period card: calculations, texts, the stat boxes
    │       └── vico/        the chart itself, its tooltip and axes (the only place Vico is used)
    ├── format/              display text: values, dates, "time ago", messages, per-language formats
    └── theme/               colors, typography and theme, plus the type colors (TypeTheme)
                             and the type icons (TypeIcon)
```

Each screen is split into a **Screen** (connects the ViewModel to the UI) and a **Content**
(layout only, previewable in Android Studio).

Larger components are split by responsibility. For example, the entry dialog consists of
`MeasurementEntryDialog` (layout) and `MeasurementEntryState` (its state and rules, unit-tested)
plus `MeasurementFieldInput`, `NumberInput` and `FocusModifiers`, all in `ui/home/newmeasurement/`.
Most components have a `@Preview` (dialogs can't be shown in the preview).

Each type's color scheme comes from `TypeTheme` in `ui/theme/`: wrapping a part of the screen in
`TypeTheme(type)` gives it that type's colors, so its cards, chips, buttons and chart follow by
themselves. The seed color of each type and the style of the schemes are set there.

The icons in `app/src/main/res/drawable/` were drawn for HealthLog. Most are multicolored, so
they are shown with `tint = Color.Unspecified` to keep their colors.

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
- `NumberInputTest`: what can be typed into number fields
- `MeasurementEntryStateTest`: the entry dialog's state and rules
- `MeasurementFormattingTest`: how measurement values are shown (units, decimals, separator)
- `DateFormattingTest`: the date format of each language
- `ValidationMessagesTest`: how range limits are shown in error messages
- `MeasurementRepositoryTest`: what is stored when a measurement is saved
- `ChartPeriodTest`: which measurements each chart period includes
- `PeriodStatsCalculationTest`: count, highest, lowest, average and their rounding
- `TimeAgoTest`: the "time ago" of the last measurement (hours, yesterday, days, months, years)

**Instrumented tests** (`app/src/androidTest`, run on an emulator or device):
- `MigrationTest`: the database migrations
- `MeasurementEntryDialogTest`: the entry dialog's UI
- `HistoryContentTest`: the history's tabs, swiping, empty pages, deleting, the See Chart
  button and the period chips
- `ChartPageTest`: the chart page: the period card (chart or empty message, stats) and the last
  entry card

To run them:

- **Android Studio:** right-click the test file or folder → **Run**
- **Terminal:** `./gradlew testDebugUnitTest` (unit tests), `./gradlew connectedDebugAndroidTest` (instrumented tests)

**Note:** after instrumented tests, Gradle uninstalls the app from that device, including its data.
Run them on a separate emulator, not on a device with data you want to keep.

## Database

Measurements are stored locally with Room. The database is currently at **version 3**.

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

## Roadmap

- Own app colors and app icon (the type colors are done)
- History list: a filter for out-of-range values
- Charts: normal ranges shown on the chart, and exporting a report
- User profiles with sign-up and log-in (including choosing the language in the app)
- Wider test coverage
