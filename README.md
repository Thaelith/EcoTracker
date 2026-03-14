# EcoTracker

EcoTracker is an Android application built to help users scan product barcodes and track their environmental impact. It utilizes a multi-layered discovery system combining crowdsourced data and generative AI to provide accurate carbon footprint estimations even for products not found in traditional databases.

## Features

- **Multi-Layered Product Discovery:**
  - **Open Food/Beauty Facts:** Primary lookup for verified nutritional and environmental data.
  - **Global Community Cache:** High-speed Firestore-based caching of previously identified products.
  - **UPCitemdb Integration:** Fallback lookup for commercial product titles and categories.
  - **AI Carbon Estimation:** Integrated Google Gemini AI to analyze product categories and estimate CO2e impact when primary data is missing.
- **User-Assisted Identification:** Interactive fallback system that allows users to provide a product description to guide the AI when a barcode is entirely unknown.
- **Environmental Impact Statistics:** Detailed tracking of daily and weekly carbon footprint totals, visualized with interactive charts.
- **Product History:** Local persistence of all scanned items with synchronized cloud backups.
- **Gamification System:** Progress tracking through experience points, levels, quests, and a global community leaderboard.
- **Secure Authentication:** Integrated Firebase Authentication for personal data persistence and cross-device synchronization.

## Technologies Used

- **Language:** Kotlin
- **Architecture:** MVVM (Model-View-ViewModel)
- **AI Integration:** Google Gemini AI (Generative AI SDK)
- **UI & Layouts:** XML Layouts, ViewBinding, Material Design 3
- **Dependency Injection:** Dagger Hilt
- **Local Database:** Room
- **Cloud Infrastructure:** Firebase Firestore, Firebase Authentication
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
│   ├── remote/         # Retrofit API services, Gemini AI integration, and models
│   └── repository/     # Centralized data access with discovery waterfall logic
├── di/                 # Dagger Hilt module configurations
├── ui/
│   ├── auth/           # Login and registration fragments
│   ├── history/        # Scanned product history list
│   ├── leaderboard/    # Global user rankings
│   ├── main/           # MainActivity and navigation setup
│   ├── manual/         # Manual product entry
│   ├── profile/        # User profile and leveling data
│   ├── quests/         # Gamification quests UI
│   ├── scan/           # Camera barcode scanner and AI feedback UI
│   └── statistics/     # Environmental impact charts
└── utils/              # Helper extensions, constants, and carbon calculations
```

## Prerequisites

- Android Studio (Hedgehog 2023.1.1 or newer)
- JDK 17
- Android SDK (API level 24 or higher)


## External Services

- **Google Gemini AI:** Provides intelligent carbon footprint estimation and product identification based on user hints or database titles.
- **Open Food Facts API:** Primary source for verified product environmental data.
- **UPCitemdb:** Supplementary database for identifying barcodes not present in Open Food Facts.
- **Firebase Firestore:** Powers the global product cache and user progress synchronization.
- **Firebase Authentication:** Secures user accounts and personalized data.

## Purpose

This project explores the intersection of mobile development, generative AI, and environmental awareness, providing a robust tool for consumers to understand and minimize their carbon footprint.
