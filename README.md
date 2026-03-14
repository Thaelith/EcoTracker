# EcoTracker

EcoTracker is an Android application built to help users scan product barcodes and track their environmental impact. It utilizes a multi-layered discovery system combining crowdsourced data and generative AI to provide accurate carbon footprint estimations even for products not found in traditional databases.

## Features

- **Multi-Layered Product Discovery:** A robust, automated pipeline that integrates verified environmental databases, community caching, and generative AI to identify products and calculate carbon impact.
- **User-Assisted Identification:** An interactive fallback mechanism allowing users to provide product descriptions to guide the AI when a barcode is not found in structured databases.
- **Environmental Impact Statistics:** Comprehensive tracking of daily and weekly carbon footprint totals, visualized through interactive analytical charts.
- **Product History:** Local persistence of scanned items with cross-device cloud synchronization.
- **Gamification System:** Engagement features including experience points (XP), leveling, specialized quests, and a global community leaderboard.
- **Secure Authentication:** Firebase-powered authentication for data persistence and secure user profiles.

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

## Product Discovery & CO₂e Estimation Pipeline

EcoTracker employs a structured, multi-step pipeline to ensure high coverage and accuracy in product identification and carbon footprint estimation. The system prioritizes verified data before utilizing AI-based heuristics.

1.  **Verified Database Lookup:** The application first queries Open Food Facts and Open Beauty Facts. These are the primary sources for verified product information, ecological scores, and carbon footprint data.
2.  **Global Community Cache:** If the product is not in the primary databases, the system checks a global Firestore-based cache. This cache stores results from previous successful discoveries and AI estimations, enabling high-speed retrieval for common items.
3.  **Supplementary Metadata Retrieval:** If the barcode is still unrecognized, EcoTracker uses UPCitemdb to retrieve technical product titles and categories. This metadata provides the necessary context for subsequent AI analysis.
4.  **Categorical AI Estimation:** Google Gemini AI analyzes the retrieved product title and category. It maps the product to known environmental impact patterns and scientific data to generate a structured CO₂e estimate.
5.  **Heuristic AI Fallback:** In cases where no structured data or metadata is available, the AI generates a best-effort estimate based on similar product types and known environmental impact factors, often guided by user-provided hints.

## Prerequisites

- Android Studio (Hedgehog 2023.1.1 or newer)
- JDK 17
- Android SDK (API level 24 or higher)


## External Services

- **Google Gemini AI:** Performs categorical analysis and best-effort carbon footprint estimation when structured data is unavailable.
- **Open Food Facts / Open Beauty Facts:** Primary APIs for retrieving verified environmental and nutritional information.
- **UPCitemdb:** Provides supplementary product metadata (titles and categories) for unrecognized barcodes.
- **Firebase Firestore:** Facilitates the global product cache and synchronizes user progress across devices.
- **Firebase Authentication:** Handles secure user identity management and data protection.

## Purpose

This project explores the intersection of mobile development, generative AI, and environmental awareness, providing a robust tool for consumers to understand and minimize their carbon footprint.
