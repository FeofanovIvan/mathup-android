# MathUp Android

MathUp is an Android math exam preparation app for Russian students preparing for OGE/EGE. The app is built around offline access to learning content, step-by-step tasks, formulas, exam sessions, statistics, custom math input, and interactive learning mechanics.

## Features

- Topic-based preparation: blocks, tasks, steps, hints, and formulas.
- Exam mode with session statistics and saved answers.
- Custom math keyboard for formula input.
- Draft canvas for handwritten calculations in Compose.
- Reference materials, formulas, videos, and OGE/EGE profiles.
- Offline-first content access through local Room databases.
- Game-style learning elements with math characters.
- Sound settings stored locally.

## Tech Stack

| Area | Technologies |
| --- | --- |
| Language | Kotlin |
| UI | Jetpack Compose, Material 3, Navigation Compose, custom Compose components |
| State | ViewModel, Compose state, Kotlin Coroutines |
| Local storage | Room, DataStore Preferences, EncryptedSharedPreferences |
| Sync / content | Asset/Firebase Storage JSON import into Room, snapshot-style sync |
| Backend / services | Firebase Auth, Firestore, Storage, Realtime Database, Crashlytics |
| Background work | WorkManager |
| Build | Gradle Kotlin DSL, version catalog, KSP, R8 minify, resource shrinking |

## Architecture

The project is organized around UI screens, reusable Compose components, local Room data sources, statistics storage, game data, and synchronization managers.

```text
app/src/main/java/com/feofanova/mathup/
  ui/
    screens/        Compose screens and screen-level ViewModels
    components/     reusable UI components, custom keyboard, draft canvas
    navigation/     Navigation Compose graph
    theme/          Material theme
  data/
    local/          Room databases, DAO, entities for learning content
    stats/          Room storage for exam/session statistics
    characters/     Room storage and sync for game characters
    remote/         exported JSON contract models
    repository/     DataSyncManager
  sound/            sound player and DataStore settings
```

Data flow for learning content:

```text
Firebase/assets JSON -> DataSyncManager -> Room -> DAO -> ViewModel -> Compose UI
```

Room is the main local source of truth for learning content, so the app can work without network after data is loaded.

## Security Notes

The following files are intentionally not committed:

- `google-services.json`
- `local.properties`
- `keystore.properties`
- `keystore/`
- `*.jks`
- release APK/AAB outputs

Use `keystore.properties.example` as a template for release signing.

## Build

```bash
./gradlew assembleDebug
```

Release builds require local signing configuration and Firebase configuration files.

## Current Improvement Areas

- Move remaining manual dependency creation toward a clearer DI setup.
- Standardize screen state around `UiState` / `UiEffect`.
- Expand unit tests for answer checking and exam/session logic.
- Consider replacing Gson with kotlinx.serialization for stricter JSON contracts.
- Improve content sync from snapshot import toward delta/version-based synchronization.
