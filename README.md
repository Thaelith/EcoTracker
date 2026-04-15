# EcoTracker

EcoTracker is an Android application for scanning product barcodes and viewing environmental impact data such as eco score and estimated carbon footprint. The app combines local persistence, public product APIs, Firebase-backed user features, and optional AI-assisted estimation for products with incomplete data.

## Features

- Barcode scanning with camera input
- Manual product entry fallback
- Local scan history
- Daily and weekly carbon statistics
- Product comparison
- Achievements and rank progression
- Firebase authentication
- Firestore-backed leaderboard and shared product cache
- Optional AI-assisted estimation when structured product data is incomplete

## Tech Stack

- Kotlin
- XML layouts with ViewBinding
- MVVM
- Hilt
- Room
- Retrofit and OkHttp
- Firebase Authentication
- Firebase Firestore
- Kotlin Coroutines and Flow
- ZXing Android Embedded
- MPAndroidChart
- Glide

## Architecture

The project uses a repository-based Android architecture:

```text
Fragments
  -> ViewModels
    -> EcoTrackerRepository
      -> Room
      -> Retrofit API services
      -> Firebase Auth / Firestore
      -> Optional Gemini estimation
```

### Local persistence model

Room persistence is split into two concerns:

- `cached_products`
  Stores the latest known product data for a barcode
- `scan_history`
  Stores user scan events separately from cached product definitions

This avoids mixing barcode cache entries with scan history records and makes history/statistics queries more reliable.

## Project Structure

```text
com.ecotracker/
├── data/
│   ├── local/          Room entities, DAO, migrations, converters
│   ├── model/          Shared models
│   ├── remote/         Retrofit services, API models, Gemini service
│   └── repository/     EcoTrackerRepository
├── di/                 Hilt module configuration
├── ui/
│   ├── achievements/
│   ├── auth/
│   ├── comparison/
│   ├── history/
│   ├── leaderboard/
│   ├── main/
│   ├── manual/
│   ├── profile/
│   ├── quests/
│   ├── scan/
│   └── statistics/
└── utils/              Shared helpers and utility classes
```

## Product Lookup Flow

When a barcode is scanned, the app resolves product data using a fallback chain:

1. Local Room cache
2. Open Food Facts
3. Open Beauty Facts
4. Shared Firestore cache
5. UPCitemdb with optional AI estimation
6. User-provided hint for AI-assisted identification

The repository prefers stronger product matches first and falls back to weaker candidates only when necessary.

## Save Flow

When a product is saved:

1. The product definition is upserted into `cached_products`
2. A scan event is inserted into `scan_history`
3. If the user is authenticated, aggregate user stats are synced to Firestore

If Firestore sync fails, the local save still succeeds.

## Setup

### Prerequisites

- Android Studio
- JDK 17
- Android SDK API 24+

### Local configuration

Copy `local.properties.example` values into your local `local.properties` if needed.

Required local items:

- Android SDK path
- `GEMINI_API_KEY` if you want AI-assisted estimation enabled

Example:

```properties
sdk.dir=C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk
GEMINI_API_KEY=your_key_here
```

### Firebase

The app expects a Firebase project configured for:

- Authentication with Email/Password
- Cloud Firestore

Place your `google-services.json` file in:

```text
app/google-services.json
```

Do not commit:

- `local.properties`
- `app/google-services.json`
- signing keys

## Build

```bash
./gradlew assembleDebug
```

## Test

```bash
./gradlew test
```

The current unit test suite covers:

- carbon calculation logic
- app configuration helpers
- gamification rules
- repository save and lookup behavior

## Notes

- AI-assisted estimation is optional. The app still works without a Gemini API key, but estimation quality and fallback coverage will be reduced.
- Firestore is used for authentication-related user data, leaderboard data, and shared product cache. Local Room storage remains the primary source for on-device history and statistics.

## Current Limitations

- Profile and achievements flows still need further architecture cleanup to fully align with the ViewModel-first structure used in other parts of the app.
- Cloud sync is partial. Local scan history is stored on-device, while Firestore currently stores aggregate user data and remote product cache data.
- AI estimation depends on external services and should be treated as an assistive fallback, not a guaranteed source of verified lifecycle data.

## Development Direction

Planned improvements include:

- stronger separation of UI and Firebase concerns in profile-related screens
- better source attribution and trust labels in the scan result flow
- richer product detail views
- more targeted persistence and migration tests
