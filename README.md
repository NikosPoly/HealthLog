# HealthLog

HealthLog is an Android app for recording everyday health measurements (blood pressure, pulse,
blood sugar, weight and height) and looking back at them in a simple history.
The app is available in Greek and English.

## Features

- **Five measurement types:** blood pressure (systolic, diastolic and pulse), pulse, blood sugar,
  weight and height
- **Automatic date and time** for every entry
- **History** of all measurements, newest first
- **Delete with confirmation**
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
- MVVM: ViewModels with `StateFlow`, and a repository between the ViewModels and the database

## Project structure

All source code is in `app/src/main/java/com/example/healthlog/`:

All source code is in `app/src/main/java/com/example/healthlog/`:

```
healthlog/
├── database/         Room entity (Measurement), DAO, database and migrations
├── repository/       MeasurementRepository, the single access point to the data
├── model/            measurement types (fields, units, ranges) and validation rules
└── ui/               navigation (HealthLogNavHost)
    ├── home/         home screen: picking a type and entering a value
    ├── history/      history screen: the list of measurements and deleting them
    ├── components/   cards and dialogs, incl. the entry dialog and its state
    ├── input/        text input: one field, number input rules, focus helpers
    ├── format/       display text: values, dates, messages, per-language formats
    └── theme/        colors, typography and theme
```

Each screen is split into a **Screen** (connects the ViewModel to the UI) and a **Content**
(layout only, previewable in Android Studio).

Larger components are split by responsibility. For example, the entry dialog consists of
`MeasurementEntryDialog` (layout) and `MeasurementEntryState` (its state and rules, unit-tested)
in `ui/components/`, plus `MeasurementFieldInput`, `NumberInput` and `FocusModifiers` in `ui/input/`.
Most components have a `@Preview` (dialogs can't be shown in the preview).

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
- `NumberInputTest`: what can be typed into number fields
- `MeasurementEntryStateTest`: the entry dialog's state and rules

**Instrumented tests** (`app/src/androidTest`, run on an emulator or device):
- `MigrationTest`: the database migrations
- `MeasurementEntryDialogTest`: the entry dialog's UI
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

- Own color theme and app icon
- User profiles with sign-up and log-in (including choosing the language in the app)
- Wider test coverage
