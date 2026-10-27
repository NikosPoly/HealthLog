# HealthLog

HealthLog is an Android app for recording everyday health measurements (blood pressure, pulse,
blood sugar, weight and height) and looking back at them in a simple history.
The app's interface is currently in Greek.

## Features

- **Five measurement types:** blood pressure (systolic, diastolic and pulse), pulse, blood sugar,
  weight and height
- **Automatic date and time** for every entry
- **History** of all measurements, newest first
- **Delete with confirmation**
- **Input validation:** every value is checked against a range; errors are shown under the field
  and saving is only possible when everything is valid
- **Rotation-safe:** typed values, dialogs and errors survive screen rotation

## Tech stack

- [Kotlin](https://kotlinlang.org/)
- [Jetpack Compose](https://developer.android.com/compose) with Material 3
- [Room](https://developer.android.com/training/data-storage/room) for local storage
- [Navigation Compose](https://developer.android.com/develop/ui/compose/navigation)
- MVVM: ViewModels with `StateFlow`, and a repository between the ViewModels and the database

## Project structure

All source code is in `app/src/main/java/com/example/healthlog/`:

| Package | Contents |
|---|---|
| `database/` | Room entity (`Measurement`), DAO, database and migrations |
| `repository/` | `MeasurementRepository`, the single access point to the data |
| `model/` | `MeasurementType` (the kinds of measurement, their fields, units and ranges) and the validation rules |
| `ui/` | Navigation (`HealthLogNavHost`) |
| `ui/home/` | Home screen: picking a measurement type and entering a value |
| `ui/history/` | History screen: the list of measurements and deleting them |
| `ui/components/` | Reusable UI parts: cards and dialogs |
| `ui/format/` | Turning data into display text: values, dates and validation messages |
| `ui/theme/` | Colors, typography and theme |

Each screen is split into a **Screen** (connects the ViewModel to the UI) and a **Content**
(layout only, previewable in Android Studio).

All displayed texts are in `app/src/main/res/values/strings.xml`.

## Requirements and setup

- Android Studio (a recent version)
- Android 8.0 (API 26) or newer on the device or emulator (`minSdk = 26`)

1. Clone the repository
2. Open the project folder in Android Studio and let Gradle sync
3. Run the `app` configuration on an emulator or a device

## Tests

There are two kinds of tests:

| Kind | Folder | Runs on | Covers |
|---|---|---|---|
| Unit tests | `app/src/test` | your computer | the validation rules (`MeasurementValidationTest`) |
| Instrumented tests | `app/src/androidTest` | an emulator or device | the database migrations (`MigrationTest`) |

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

- English translation
- Own color theme and app icon
- User profiles with sign-up and log-in
- Wider test coverage

## License

This project is licensed under the [Apache License 2.0](LICENSE).
