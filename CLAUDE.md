# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Home Harmony is an Android furniture e-commerce application built with Kotlin using MVVM architecture. The app features user authentication, product browsing, shopping cart, wishlist, order management, and integrated payment systems with enhanced security features.

**Package name**: `com.example.furniturecloudy`

## Build Commands

### Build the application
```bash
./gradlew assembleDebug
```

### Run tests
```bash
# Unit tests
./gradlew test

# Instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest
```

### Clean build
```bash
./gradlew clean
```

### Specific test execution
```bash
# Run a single test class
./gradlew test --tests "com.example.furniturecloudy.ExampleUnitTest"

# Run instrumented test
./gradlew connectedAndroidTest --tests "com.example.furniturecloudy.ExampleInstrumentedTest"
```

### Build release with obfuscation
```bash
# Build release APK with R8 obfuscation enabled
./gradlew assembleRelease

# Output APK: app/build/outputs/apk/release/app-release-unsigned.apk
# Mapping file: app/build/outputs/mapping/release/mapping.txt (SAVE THIS!)
```

**Note**: ProGuard/R8 obfuscation is enabled for release builds. Always save `mapping.txt` for deobfuscating crash reports. See `docs/PROGUARD_OBFUSCATION.md` for details.

## Architecture

### MVVM Pattern
The app follows strict MVVM (Model-View-ViewModel) architecture:

- **Models** (`data/`): Data classes representing entities (Product, Order, User, Address, etc.)
- **Views** (`present/`): Activities and Fragments that observe ViewModels
- **ViewModels** (`model/viewmodel/`): Business logic and state management, expose LiveData/StateFlow

### Dependency Injection
Uses **Hilt-Dagger** for dependency injection. All modules defined in `di/Module.kt`:
- Firebase Auth, Firestore, Storage instances
- Room database and DAOs
- SharedPreferences instances
- FirebaseCommon utility

### Navigation
Single-activity architecture with Navigation Component:
- `LoginRegisterActivity`: Entry point, handles authentication flow (`login_register_nav.xml`)
- `ShoppingActivity`: Main app, handles shopping features (`shopping_nav.xml`)

### Data Layer

**Remote (Firebase)**:
- **Firestore collections**: `user`, `products`, `orders`, `reviews`, `cart`, `wishlist`
- **Firebase Authentication**: Email/password and Google Sign-In
- **Firebase Storage**: Product images and user profile pictures

**Local (Room Database)**:
- `FurnitureDatabase` with entities: `SearchHistory`, `RecentlyViewed`
- DAOs provide suspend functions for coroutine-based access
- Repositories pattern wraps DAOs for clean data access

### Security Features

**Session Management** (`SessionManager`):
- Tracks user activity and enforces configurable timeout (1min - 1hr)
- Automatically locks session when timeout expires
- Integrates with `ProcessLifecycleOwner` to detect foreground/background transitions
- All activities extending `BaseActivity` inherit session management

**Authentication** (`AppAuthManager`, `BiometricHelper`, `PinCodeManager`):
- Three authentication methods: Biometric, Device Credential, App PIN
- `LockScreenActivity` handles re-authentication on session timeout
- Biometric authentication uses Android Biometric API

**Data Encryption** (`AddressEncryptionHelper`):
- Encrypts sensitive address fields (phone, addressFull) using AES-256-GCM
- Uses Android Keystore for secure key storage
- Encrypted data format: `ENC:<base64_iv>:<base64_ciphertext>`
- Encryption/decryption happens at repository/ViewModel layer before Firestore operations

### Payment Integration

**MoMo Payment SDK** (`momo_partner_sdk` module):
- Helper class: `MoMoPaymentHelper` in `utils/payment/`
- Initialize SDK with environment (development/production)
- Request payments via `requestPayment()`, handle results in `onActivityResult`
- Configuration in `MoMoConfig.kt`

## Key Components

### Application Lifecycle
`CloudyApplication` (HiltAndroidApp):
- Initializes SessionManager
- Registers ProcessLifecycleOwner observers
- Tracks current activity for session management

### Base Classes
- `BaseActivity`: Handles session timeout, authentication checks, touch event tracking
- `BaseCategoryFragment`: Reusable fragment for product categories with pagination

### Fragment Organization
```
present/fragments/
├── categories/      # Category-specific product lists (Chair, Table, Cupboard, etc.)
├── loginRegister/   # Authentication flows (Login, Register, ForgotPassword, Introduction)
├── setting/         # User settings (Profile, Account management)
└── shopping/        # Core shopping features (Home, Cart, Wishlist, ProductDetail, Billing, etc.)
```

### ViewModels
All ViewModels follow this pattern:
- Inject dependencies via constructor (Hilt)
- Expose UI state via LiveData or StateFlow
- Use `Resource<T>` sealed class for state (Loading, Success, Error, Idle)
- Coroutines with Firebase Play Services for async operations

### Utilities
- `FirebaseCommon`: Cart operations (add, increase/decrease quantity)
- `VoiceSearchManager`: Speech recognition for product search
- `ValidationCheck`, `RegisterValidation`: Input validation helpers
- `ShowHideBottomNavigation`: Extension for managing bottom navigation visibility

## Firebase Setup

Required Firebase configuration:
1. Download `google-services.json` from Firebase Console
2. Place in `app/` directory
3. Enable Authentication providers: Email/Password, Google Sign-In
4. Create Firestore collections: `user`, `products`, `orders`, `reviews`, `cart`, `wishlist`
5. Set up Storage buckets for images

## Dependencies

Key libraries:
- **Hilt-Dagger 2.51**: Dependency injection
- **Navigation 2.7.5**: Single-activity architecture
- **Room 2.6.1**: Local database with KSP annotation processing
- **Firebase**: Auth, Firestore, Storage
- **Glide 4.14.2**: Image loading
- **Kotlin Coroutines**: Asynchronous operations
- **Material Design 3**: UI components
- **Biometric 1.1.0**: Biometric authentication
- **Lifecycle 2.7.0**: ProcessLifecycleOwner for session management

## Common Development Patterns

### Adding a New ViewModel
1. Create ViewModel class in `model/viewmodel/`
2. Use `@HiltViewModel` annotation and inject dependencies
3. Expose state via `MutableStateFlow<Resource<T>>`
4. Use coroutines for async operations: `viewModelScope.launch { }`

### Adding a New Fragment
1. Create Fragment in appropriate `present/fragments/` subdirectory
2. Use ViewBinding: `private var _binding: FragmentBinding? = null`
3. Inject ViewModel: `private val viewModel by viewModels<YourViewModel>()`
4. Observe ViewModel state in `onViewCreated()`
5. Clean up binding in `onDestroyView()`

### Firestore Operations
Always use `FirebaseCommon` for cart operations. For other collections:
1. Inject `FirebaseFirestore` and `FirebaseAuth` via Hilt
2. Use `.await()` extension from kotlinx-coroutines-play-services
3. Wrap in try-catch and emit Resource states

### Working with Encrypted Data
When handling Address objects:
1. Encrypt before saving: `addressEncryptionHelper.encryptAddress(address)`
2. Decrypt after reading: `addressEncryptionHelper.decryptAddress(address)`
3. Check encryption status: `addressEncryptionHelper.isEncrypted(field)`

### Session Timeout
To check/modify session behavior:
- Configuration: `SessionManager.getInstance(context)`
- Timeout durations: `TIMEOUT_1_MINUTE` to `TIMEOUT_1_HOUR`
- Enable/disable: `sessionManager.setSessionTimeoutEnabled(boolean)`
- Lock/unlock: `sessionManager.lockSession()` / `unlockSession()`

## Testing Notes

- Unit tests: `app/src/test/java/`
- Instrumented tests: `app/src/androidTest/java/`
- Mock Firebase with `MockK` or use Firebase Test Lab
- For local database testing, use in-memory Room database

## SDK Versions

- Min SDK: 24 (Android 7.0)
- Target SDK: 34 (Android 14)
- Compile SDK: 34
- Java: 1.8

## Code Obfuscation & Security

**ProGuard/R8 Obfuscation** is enabled for release builds:
- All class/method/field names are obfuscated to prevent reverse engineering
- Unused code and resources are automatically removed
- Debug logs (Log.d/v/i/w/e) are stripped in release builds
- Comprehensive rules in `proguard-rules.pro` cover all libraries (Firebase, Hilt, Room, Glide, etc.)

**Critical:**
- Always save `app/build/outputs/mapping/release/mapping.txt` after each release build
- Use mapping.txt to deobfuscate crash reports via `retrace.sh`
- See `docs/PROGUARD_OBFUSCATION.md` for complete documentation

**Security layers:**
1. Code obfuscation (R8)
2. Data encryption (AES-256-GCM)
3. Session timeout
4. Biometric authentication
5. Secure key storage (Android Keystore)

## Important Notes

- All sensitive data (addresses) must be encrypted before Firestore operations
- Session management is tightly coupled with BaseActivity - don't bypass it
- MoMo SDK requires merchant credentials from https://business.momo.vn
- Voice search requires RECORD_AUDIO permission at runtime
- Biometric authentication requires USE_BIOMETRIC permission
- ProGuard mapping files must be version-controlled or uploaded to Play Console