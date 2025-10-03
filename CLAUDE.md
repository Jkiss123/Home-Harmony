# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Home Harmony is an Android e-commerce application for furniture shopping, built in Kotlin. The app allows users to browse furniture by category, manage a shopping cart, track orders, and manage their profile.

**Package**: `com.example.furniturecloudy`

## Technology Stack

- **Language**: Kotlin
- **Architecture**: MVVM with LiveData
- **DI**: Hilt-Dagger
- **Navigation**: Navigation Component with Safe Args
- **Backend**: Firebase (Auth, Firestore, Storage)
- **Image Loading**: Glide
- **View Binding**: DataBinding + ViewBinding enabled
- **Async**: Coroutines with Firebase Play Services integration

## Build Commands

```bash
# Build the project
./gradlew build

# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Clean build
./gradlew clean

# Install debug APK
./gradlew installDebug
```

## Architecture

### Package Structure

```
com.example.furniturecloudy/
├── data/              # Data models (Product, User, Order, Address, CartProducts, etc.)
├── model/
│   ├── viewmodel/     # ViewModels for each screen
│   ├── adapter/       # RecyclerView adapters
│   └── firebase/      # Firebase helper classes (FirebaseCommon)
├── present/
│   ├── fragments/
│   │   ├── shopping/       # Main shopping flow fragments
│   │   ├── loginRegister/  # Auth flow fragments
│   │   ├── categories/     # Category-specific fragments (extends BaseCategoryFragment)
│   │   └── setting/        # User settings fragments
│   ├── LoginRegisterActivity.kt  # Entry point, launcher activity
│   └── ShoppingActivity.kt       # Main shopping flow container
├── di/                # Hilt dependency injection modules
├── util/              # Utilities and extensions
└── CloudyApplication.kt  # Application class
```

### Navigation

The app uses two separate navigation graphs:
- `login_register_nav.xml` - Auth flow (login, register, account options)
- `shopping_nav.xml` - Main shopping flow (home, cart, profile, categories, orders)

Activities act as navigation containers for their respective graphs.

### Category Fragments Pattern

Category fragments (Chair, Table, Cupboard, Accessory) extend `BaseCategoryFragment`, which provides common functionality for displaying products filtered by category.

### Firebase Integration

- **FirebaseCommon** (`model/firebase/FirebaseCommon.kt`): Centralized helper for cart operations (add, increase/decrease quantity)
- Uses Firestore transactions for cart quantity updates to prevent race conditions
- Collection structure: `user/{userId}/cart/{productId}`
- Firebase services injected via Hilt in `di/Module.kt`

### ViewModel Pattern

Each major screen has a dedicated ViewModel:
- `LoginViewmodel`, `LoginRegisterViewmodel` - Auth flows
- `MainCategoryViewmodel`, `BaseCategoryViewmodel` - Product browsing
- `DetailViewmodel` - Product details
- `OrderViewmodel`, `AllOrdersViewmodel` - Order management
- `AddressViewmodel` - Address management
- `UserAccountViewmodel` - Profile management
- `SearchViewmodel` - Product search

### State Management

The app uses LiveData for reactive state updates. Check for `Resource` sealed class usage in `util/Resource.kt` for standard success/error/loading state patterns.

## Key Configuration

- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)
- **Compile SDK**: 34
- **JVM Target**: 1.8
- **Application ID**: `com.example.furniturecloudy`

## Firebase Collections

- `user` - User profiles
- `user/{userId}/cart` - User shopping cart
- Products and orders stored in Firestore (specific collection names in `util/Constants.kt`)

## Development Notes

- The app uses ViewBinding and DataBinding - ensure both are enabled when adding new layouts
- Navigation uses Safe Args plugin - rebuild after modifying navigation graphs
- Hilt requires KAPT with `correctErrorTypes = true` configuration
- When working with Firebase, use `FirebaseCommon` for cart operations to maintain consistency
- SharedPreferences key for intro screen: `INTRODUCTION_SP` (see `util/Constants.kt`)
- Price formatting: Format to two decimal places (recent commit: "Format price to two decimal places")

## Testing

- Unit tests: `app/src/test/java/com/example/furniturecloudy/`
- Instrumented tests: `app/src/androidTest/java/com/example/furniturecloudy/`