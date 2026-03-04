# Eco-Scanner & Tracker 

A modern Android application to scan product barcodes and track your environmental impact.  
Built with **Kotlin**, **Material Design 3**, and **MVVM Architecture**.

---

## Screenshots Overview

| Scan | History | Statistics |
|------|---------|------------|
| Camera viewfinder + product result card | RecyclerView list with eco-scores | Bar chart + daily/weekly totals |

---

## Architecture

```
MVVM (Model–View–ViewModel)
├── View   → Fragments (observe LiveData, dispatch events)
├── VM     → ViewModels (business logic, LiveData)
└── Model  → Repository → [Room DB | Firestore | Retrofit API]
```

### Package Structure

```
com.ecotracker/
├── EcoTrackerApp.kt           ← Hilt application class
│
├── data/
│   ├── local/
│   │   ├── ScannedProduct.kt  ← Room @Entity
│   │   ├── ScannedProductDao.kt
│   │   └── EcoTrackerDatabase.kt
│   ├── remote/
│   │   ├── OpenFoodFactsApiService.kt   ← Retrofit interface
│   │   └── OpenFoodFactsModels.kt       ← Response DTOs
│   └── repository/
│       └── EcoTrackerRepository.kt      ← Single source of truth
│
├── di/
│   └── AppModule.kt           ← Hilt @Module (DB + Retrofit)
│
├── ui/
│   ├── auth/                  ← Firebase Login / Register
│   ├── history/
│   ├── leaderboard/           ← Gamification leaderboard
│   ├── main/                  ← MainActivity + BottomNav
│   ├── manual/                ← Manual entry
│   ├── profile/               ← User profile & Leveling System
│   ├── quests/                ← Gamification quests
│   ├── scan/
│   └── statistics/
│
└── utils/
    ├── Resource.kt            ← Sealed class for UI state
    ├── CarbonCalculator.kt    ← CO₂ estimation logic
    └── Extensions.kt          ← Kotlin extensions + mapper
```

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 1.9 |
| UI | Material Design 3, ViewBinding |
| Navigation | Navigation Component |
| DI | Hilt 2.48 |
| Database | Room 2.6, Firebase Firestore |
| Authentication | Firebase Authentication |
| Networking | Retrofit 2 + OkHttp 4 |
| Async | Kotlin Coroutines + Flow |
| Barcode | ZXing Android Embedded |
| Charts | MPAndroidChart |
| Image | Glide |

---

## Key Dependencies (`app/build.gradle`)

```groovy
// Navigation
implementation 'androidx.navigation:navigation-fragment-ktx:2.7.6'

// Room
implementation 'androidx.room:room-runtime:2.6.1'
ksp 'androidx.room:room-compiler:2.6.1'

// Retrofit
implementation 'com.squareup.retrofit2:retrofit:2.9.0'

// Hilt
implementation 'com.google.dagger:hilt-android:2.48'

// ZXing barcode scanner
implementation 'com.journeyapps:zxing-android-embedded:4.3.0'

// MPAndroidChart
implementation 'com.github.PhilJay:MPAndroidChart:v3.1.0'

// Firebase
implementation platform('com.google.firebase:firebase-bom:32.7.2')
implementation 'com.google.firebase:firebase-auth'
implementation 'com.google.firebase:firebase-firestore'
```

> **Note:** MPAndroidChart requires JitPack in `settings.gradle`:
> ```groovy
> maven { url 'https://jitpack.io' }
> ```

---

## Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 24+

### Setup Steps

1. **Clone / open** the project in Android Studio
2. **Sync** Gradle (`File → Sync Project with Gradle Files`)
3. **Add JitPack** to `settings.gradle` for MPAndroidChart:
   ```groovy
   dependencyResolutionManagement {
       repositories {
           google()
           mavenCentral()
           maven { url 'https://jitpack.io' }   // ← add this
       }
   }
   ```
4. **Add Google Services**: Ensure your Firebase `google-services.json` is placed in the `app/` directory.
5. **Run** on emulator or physical device (API 24+)

---

## API: Open Food Facts

- **Base URL:** `https://world.openfoodfacts.org/`
- **Endpoint:** `GET /api/v2/product/{barcode}.json`
- **No API key required** — free & open database
- Returns eco-score grade (A–E), CO₂ data, brand, categories

### Sample barcode to test
- `3017620422003` → Nutella
- `5449000000996` → Coca-Cola
- `0737628064502` → ChocRite

---

## Room Database Schema

### Table: `scanned_products`

| Column | Type | Notes |
|--------|------|-------|
| id | Long (PK, autoincrement) | |
| barcode | String | |
| productName | String | |
| brand | String | |
| ecoScore | String | "A"–"E" or "N/A" |
| ecoScoreValue | Int | 0–100 |
| carbonFootprint | Double | kg CO₂e (mock) |
| imageUrl | String | |
| categories | String | |
| scannedAt | Long | epoch ms |

---

## Carbon Footprint Calculation

Currently uses a **mock** value based on:
1. Real `agribalyse.co2_total` from Open Food Facts (if available)
2. `carbon-footprint-from-known-ingredients` nutriment (if available)
3. Grade-based random range fallback:
   - A → 0.2–0.8 kg CO₂e
   - B → 0.8–2.0 kg CO₂e
   - C → 2.0–4.0 kg CO₂e
   - D → 4.0–7.0 kg CO₂e
   - E → 7.0–15.0 kg CO₂e

---

## Navigation

```
MainActivity
└── NavHostFragment (nav_graph.xml)
    ├── ScanFragment      (startDestination)
    │   └── ManualEntryFragment
    ├── HistoryFragment
    ├── StatisticsFragment
    ├── LeaderboardFragment
    ├── QuestsFragment
    └── ProfileFragment
```

Bottom Navigation uses `setupWithNavController()` for automatic back-stack and icon highlighting.

---

## Features Checklist

- [x] MVVM architecture with Hilt DI
- [x] Room database with Flow reactive queries
- [x] Firebase Authentication (email/password login and registration)
- [x] Firebase Firestore (user profiles and cloud data sync)
- [x] Retrofit + Open Food Facts API integration
- [x] ZXing barcode scanning
- [x] Navigation Component + Bottom Navigation
- [x] Material Design 3 theming (green eco theme)
- [x] Product result card with eco-score badge
- [x] History list with swipe-to-delete
- [x] Statistics bar chart (MPAndroidChart)
- [x] Daily & weekly carbon totals
- [x] Kotlin Coroutines + Flow throughout
- [x] Gamification Engine (Leveling System, Quests, Leaderboard)
- [x] Manual Product Entry
- [x] User Profiles

### Future Enhancements
- [ ] CameraX real-time barcode scanning (replace ZXing)
- [ ] Product image loading with Glide
- [ ] Eco-score explanation dialog
- [ ] Export history to CSV
- [ ] Push notifications for weekly summary
- [ ] Dark mode optimisation
- [ ] Unit & integration tests

---

## License

MIT — free to use and modify.
