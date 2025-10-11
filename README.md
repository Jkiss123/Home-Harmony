# 🏠 Home Harmony - Furniture E-Commerce Android App

A comprehensive furniture shopping application built with modern Android development practices. Home
Harmony provides users with an intuitive platform to browse, purchase, and manage furniture orders
with a seamless shopping experience.

## 📱 Features

### 🔐 Authentication & User Management

- **User Registration & Login** - Secure account creation and authentication
- **Google Sign-In Integration** - Quick login with Google accounts
- **Password Recovery** - Forgot password functionality with email reset
- **Profile Management** - Update personal information, profile picture, and preferences

### 🛍️ Shopping Experience

- **Product Catalog** - Browse furniture by categories (Chairs, Tables, Cupboards, Accessories,
  etc.)
- **Advanced Search** - Text-based and voice search functionality
- **Product Filtering** - Filter products by price, rating, category, and availability
- **Product Details** - Comprehensive product information with image gallery
- **Wishlist Management** - Save favorite products for later purchase
- **Shopping Cart** - Add, remove, and manage items before checkout

### 🛒 Order Management

- **Secure Checkout** - Multiple payment methods support
- **Address Management** - Add, edit, and manage delivery addresses
- **Order Tracking** - Real-time order status updates
- **Order History** - View all past and current orders
- **Order Details** - Detailed information about each order

### ⭐ Social Features

- **Product Reviews & Ratings** - Read and write product reviews
- **User Ratings** - Rate products based on purchase experience
- **Verified Purchase Reviews** - Authentic reviews from verified buyers

### 🔍 Advanced Features

- **Voice Search** - Search products using voice commands (Vietnamese supported)
- **Offline Support** - Local database caching with Room
- **Real-time Sync** - Data synchronization with Firebase
- **Bottom Navigation** - Easy navigation between Home, Search, Cart, and Profile

## 🛠️ Tech Stack

### Architecture & Design Patterns

- **MVVM Architecture** - Clean separation of concerns
- **Repository Pattern** - Data abstraction layer
- **LiveData & DataBinding** - Reactive UI components

### Core Technologies

- **Kotlin** - Primary programming language
- **Android Jetpack Components** - Modern Android development
- **Navigation Component** - Single activity architecture with fragments

### Backend & Database

- **Firebase Authentication** - User authentication and management
- **Firebase Firestore** - Cloud NoSQL database
- **Firebase Storage** - Image and file storage
- **Room Database** - Local data persistence and offline support

### Dependency Injection & Networking

- **Hilt-Dagger** - Dependency injection framework
- **Kotlin Coroutines** - Asynchronous programming
- **Coroutines with Firebase** - Reactive data operations

### UI & UX

- **Material Design 3** - Modern UI components
- **View Binding** - Type-safe view references
- **Glide** - Efficient image loading and caching
- **Loading Button** - Enhanced user interaction feedback
- **StepView** - Order progress visualization
- **CircleImageView** - Circular profile images

### Additional Libraries

- **Google Play Services** - Google Sign-In integration
- **Gson** - JSON serialization/deserialization
- **Speech Recognition** - Voice search functionality

## 📋 Requirements

- **Minimum SDK**: API 24 (Android 7.0)
- **Target SDK**: API 34 (Android 14)
- **Compile SDK**: API 34
- **Java Version**: 1.8

## 🚀 Getting Started

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/home-harmony.git
   cd home-harmony
   ```

2. **Set up Firebase**
    - Create a new Firebase project
    - Enable Authentication, Firestore, and Storage
    - Download `google-services.json` and place it in the `app/` directory

3. **Build and run**
   ```bash
   ./gradlew assembleDebug
   ```

## 📁 Project Structure

```
app/src/main/java/com/example/furniturecloudy/
├── data/                    # Data models and entities
├── database/               # Room database components
│   ├── dao/               # Data Access Objects
│   ├── entity/            # Database entities
│   └── repository/        # Repository implementations
├── di/                    # Dependency injection modules
├── model/                 # View models and adapters
├── present/               # UI layer
│   └── fragments/         # Fragment implementations
│       ├── categories/    # Category-specific fragments
│       ├── loginRegister/ # Authentication fragments
│       ├── setting/       # Settings and profile
│       └── shopping/      # Shopping-related fragments
└── util/                  # Utility classes and extensions
```

## 🔄 Data Flow

1. **UI Layer** - Fragments and Activities handle user interactions
2. **ViewModel Layer** - Manages UI-related data and business logic
3. **Repository Layer** - Provides clean API for data access
4. **Data Sources** - Firebase (remote) and Room (local) databases

## 🌟 Key Features Implementation

### Authentication Flow

- Registration with email validation
- Login with email/password or Google Sign-In
- Password reset via email
- Automatic session management

### Shopping Flow

- Category-based product browsing
- Search with filters and voice input
- Add to cart with quantity selection
- Wishlist management
- Secure checkout process
- Order tracking and history

### Offline Support

- Product data cached locally with Room
- Seamless sync when connection is restored
- Optimistic UI updates

## 🔧 Configuration

### Firebase Setup

1. Authentication providers: Email/Password, Google Sign-In
2. Firestore collections: users, products, orders, reviews, cart
3. Storage buckets: product images, user profiles


## 📸 Screenshots

![Home Harmony App Overview](https://github.com/cuocdart18/eKMA/assets/111178266/729fee2a-27a5-401a-9c0e-1499a59df79e)

![App Interface 1](https://github.com/Jkiss123/NoteApp/assets/111178266/7a7a4fb1-4082-4a52-b075-45642d2dfc18)

![App Interface 2](https://github.com/Jkiss123/NoteApp/assets/111178266/943cf4ba-daf9-4afd-99fa-5fca531342af)

![App Interface 3](https://github.com/Jkiss123/NoteApp/assets/111178266/70db23dd-5508-4c05-922c-6e2efb2a18a7)


## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👥 Authors

- **Mây** - *Initial work* - [YourGitHub](https://github.com/Jkiss123)

---

**Home Harmony** - Making furniture shopping simple and enjoyable! 🛋️✨