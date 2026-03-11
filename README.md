# EcoTracker

EcoTracker is an Android application built to help users scan product barcodes and track their environmental impact. It allows users to view the eco-score and carbon footprint of scanned products, maintain a scan history, and engage with gamification features like leveling, quests, and leaderboards.

## Features

- **Barcode Scanning:** Scan products using the device camera to retrieve environmental data via the Open Food Facts API.
- **Manual Entry:** Manually search and input products if a barcode fails to scan.
- **Product History:** View previously scanned products in a persistent local list.
- **Environmental Impact Statistics:** View daily and weekly carbon footprint totals visualized with bar charts.
- **Gamification:** Participate in quests, earn experience points to level up, and view a global leaderboard.
- **User Accounts:** Authenticate using email and password, with user data and scores backed by the cloud.

## Technologies Used

- **Language:** Kotlin
- **Architecture:** MVVM (Model-View-ViewModel)
- **UI & Layouts:** XML Layouts, ViewBinding, Material Design 3
- **Dependency Injection:** Dagger Hilt
- **Local Database:** Room
- **Cloud Database:** Firebase Firestore
- **Authentication:** Firebase Authentication
- **Networking:** Retrofit, OkHttp
- **Asynchrony:** Kotlin Coroutines, Flow
- **Barcode Scanning:** ZXing Android Embedded
- **Charts:** MPAndroidChart
- **Image Loading:** Glide
- **Navigation:** Jetpack Navigation Component

## Project Structure

```
com.ecotracker/
├── data/
│   ├── local/          # Room database entities and DAOs
│   ├── remote/         # Retrofit API services and models
│   └── repository/     # Centralized data access (EcoTrackerRepository)
├── di/                 # Dagger Hilt module configurations
├── ui/
│   ├── auth/           # Login and registration fragments
│   ├── history/        # Scanned product history list
│   ├── leaderboard/    # Global user rankings
│   ├── main/           # MainActivity and bottom navigation setup
│   ├── manual/         # Manual product lookup
│   ├── profile/        # User profile and leveling data
│   ├── quests/         # Gamification quests UI
│   ├── scan/           # Camera barcode scanner
│   └── statistics/     # Environmental impact charts
└── utils/              # Helper extensions, constants, and carbon calculations
```

## Setup and Installation

### Prerequisites
- Android Studio (Hedgehog 2023.1.1 or newer recommended)
- JDK 17
- Android SDK (API level 24 or higher)

### Steps to Run
1. Clone the repository.
2. Open the project in Android Studio.
3. Sync the project with Gradle files.
4. Obtain a `google-services.json` file for your Firebase project and place it in the `app/` directory (required for Authentication and Firestore).
5. Build and run the application on an Android emulator or a physical device.

## External Services

- **Open Food Facts API:** Used to retrieve product data (eco-score, carbon footprint, brand) using a barcode. It is free and requires no API key.
- **Firebase Authentication:** Manages user registration and login.
- **Firebase Firestore:** Syncs user profiles, experience points, and history to the cloud.
