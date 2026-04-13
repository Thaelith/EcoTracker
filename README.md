# EcoTracker

Android app for scanning product barcodes and tracking carbon footprint. Uses a multi-source lookup pipeline combined with Gemini AI for carbon estimation.

## Architecture

```
MVVM + Hilt + Room + Firebase

View (Fragments)
  └─ ViewModel (LiveData / StateFlow)
       └─ Repository (single source of truth)
            ├─ Room DAO (local persistence)
            ├─ Retrofit APIs (OFF, OBF, UPCitemdb)
            ├─ Gemini AI (carbon estimation)
            └─ Firebase Firestore (global cache, leaderboard, user sync)
```

### Package layout

```
com.ecotracker/
├── data/
│   ├── local/          Room entities, DAO, type converters
│   ├── model/          Shared data classes (LeaderboardUser)
│   ├── remote/         Retrofit services, Gemini AI, API models
│   └── repository/     EcoTrackerRepository
├── di/                 Hilt AppModule
├── ui/
│   ├── achievements/   Badge grid
│   ├── auth/           Login, Register, Username setup
│   ├── comparison/     Side-by-side carbon comparison
│   ├── history/        Scanned product list
│   ├── leaderboard/    Global rankings
│   ├── main/           MainActivity, navigation
│   ├── manual/         Manual product entry fallback
│   ├── profile/        User profile, rank, badges preview
│   ├── quests/         Gamification quests
│   ├── scan/           Camera scanner, product result card
│   └── statistics/     Charts, daily/weekly carbon totals
└── utils/
    ├── AppConfig.kt    Centralized constants
    ├── CarbonCalculator.kt
    ├── Extensions.kt   View/date/color helpers
    ├── GamificationEngine.kt
    ├── Logger.kt       Release-safe logging
    └── Resource.kt     Sealed result wrapper
```

## Data Flow

### Barcode lookup waterfall

When a barcode is scanned, the repository checks sources in this order:

1. **Local Room cache** — ScanViewModel checks DAO first (`getProductByBarcode`). If found, skips network entirely.
2. **OpenFoodFacts** — Primary API. If product found but lacks carbon data, Gemini AI estimates it.
3. **OpenBeautyFacts** — Parallel with OFF. Covers cosmetics/personal care.
4. **Global Firestore cache** — Community-shared results with a configurable TTL (default 90 days via `AppConfig.CACHE_TTL_DAYS`).
5. **UPCitemdb + Gemini** — Retrieves product metadata, then Gemini estimates carbon from the title/category.
6. **User input fallback** — If all sources fail, the user is prompted to describe the product. Gemini uses the hint to identify and estimate.

Steps 1-3 run in parallel. Steps 4-6 run sequentially as fallbacks.

### Save flow

`saveProduct` writes to Room first, then syncs to Firestore inside a transaction. The transaction uses a deterministic document ID (`barcode_timestamp`) to prevent duplicate writes on retry. If Firestore fails, the local save still succeeds.

## Setup

### Prerequisites

- Android Studio (Hedgehog 2023.1.1+)
- JDK 17
- Android SDK API 24+

### Gemini API

1. Get an API key from [Google AI Studio](https://aistudio.google.com/apikey).
2. Copy `local.properties.example` to `local.properties`.
3. Add: `GEMINI_API_KEY=your_key_here`

### Firebase

1. Create a project at [Firebase Console](https://console.firebase.google.com/).
2. Add an Android app with package name `com.ecotracker`.
3. Download `google-services.json` and place it in the `app/` directory.
4. Enable **Authentication** (Email/Password).
5. Enable **Cloud Firestore**.

> **Do not commit `google-services.json` or `local.properties` to version control.**  
> Both are listed in `.gitignore`.

### Build and run

```bash
./gradlew assembleDebug
```

Or open in Android Studio and run on a device/emulator with camera access.

## Testing

### Run unit tests

```bash
./gradlew test
```

### Test coverage

- `CarbonCalculatorTest` — All 4 estimation tiers (Agribalyse, nutriments, category map, eco-score grade), format(), hasRealCarbonData().
- `GamificationEngineTest` — Rank boundaries (Seedling through Forest Guardian), all badge unlock conditions.
- `AppConfigTest` — TTL calculation consistency, username regex validation.
- `EcoTrackerRepositoryTest` — Barcode lookup priority order, API exception handling, local DAO save, duplicate scan prevention.

### What the tests use

- JUnit 4
- MockK for mocking Retrofit services and Room DAO
- kotlinx-coroutines-test for `runTest`

## Configuration

All magic numbers live in `AppConfig.kt`:

| Constant | Default | Purpose |
|---|---|---|
| `CACHE_TTL_DAYS` | 90 | Global Firestore cache expiry |
| `NETWORK_CONNECT_TIMEOUT_SECONDS` | 60 | OkHttp connect timeout |
| `NETWORK_READ_TIMEOUT_SECONDS` | 60 | OkHttp read timeout |
| `LEADERBOARD_MAX_SIZE` | 20 | Firestore query limit |
| `USERNAME_MAX_LENGTH` | 24 | Max characters for usernames |
| `BARCODE_VISIBLE_PREFIX` | 4 | Characters shown in logs |

## Logging

`Logger.kt` wraps `android.util.Log`:

- `Logger.debug()` — suppressed in release builds
- `Logger.error()` — always active
- `Logger.maskBarcode()` — shows only first 4 characters
- HTTP body logging is disabled in release builds (OkHttp interceptor level set to `NONE`)

## Roadmap

- Offline mode with queued Firestore sync
- Product detail screen with full AI reasoning breakdown
- Category-level statistics (food vs cosmetics vs electronics)
- Export scan history as CSV
- Dark mode support
- Instrumented UI tests with Espresso
