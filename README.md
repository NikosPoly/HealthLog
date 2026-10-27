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

| Package | Contents |
|---|---|
| `database/` | Room entity (`Measurement`), DAO, database and migrations |
| `repository/` | `MeasurementRepository`, the single access point to the data |
| `model/` | `MeasurementType` (the kinds of measurement, their fields, units and ranges) and the validation rules |
| `ui/` | Navigation (`HealthLogNavHost`) |
| `ui/home/` | Home screen: picking a measurement type and entering a value |
| `ui/history/` | History screen: the list of measurements and deleting them |
| `ui/components/` | Reusable UI parts: cards and dialogs |
| `ui/format/` | Turning data into display text: values, dates, validation messages and the number/date formats of each language |
| `ui/theme/` | Colors, typography and theme |

Each screen is split into a **Screen** (connects the ViewModel to the UI) and a **Content**
(layout only, previewable in Android Studio).

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

Unit tests run on your computer, without an emulator:

- **Android Studio:** right-click `app/src/test` → **Run 'Tests in…'**
- **Terminal:** `./gradlew testDebugUnitTest`

The tests currently cover the validation rules (`MeasurementValidationTest`).

## Database

Measurements are stored locally with Room. The database is currently at **version 3**.

Any change to what is stored needs a database migration in `HealthDatabase.kt`, for example:
- adding, removing or renaming a column in `Measurement`
- renaming or removing a `MeasurementType` constant (its name is what is stored)
- reordering the fields of a measurement type (their position decides the column they are stored in)

Changes to labels, units, ranges or the UI do not need a migration.

## Roadmap

- Own color theme and app icon
- User profiles with sign-up and log-in (including choosing the language in the app)
- Wider test coverage

## License

This project is licensed under the [Apache License 2.0](LICENSE).
