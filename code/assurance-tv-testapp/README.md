# Adobe Experience Platform Assurance TV Test App

A sample Android TV application demonstrating Adobe Experience Platform Assurance SDK integration.

## What This App Does
- **Adobe Assurance**: Real-time debugging and validation of Adobe SDK events
- **Video Player**: ExoPlayer-based video streaming with TV remote control support
- **Media Analytics**: Chapter and ad tracking with Adobe Edge Media SDK
- **TV Optimized**: Android TV Material Design with focus animations

## Quick Start

### 1. Configure Your App ID
Replace `YOUR_APP_ID` with your actual Adobe configuration ID in the app initialization:

```kotlin
MobileCore.initialize(
    this.application,
    "YOUR_APP_ID",  // ← Replace this with your actual App ID
)
```

### 2. Build and Install
```bash
cd aepsdk-assurance-android/code
./gradlew :assurance-tv-testapp:assembleDebug
./gradlew :assurance-tv-testapp:installDebug
```

### 3. Run on Android TV emulator
- **Option A**: Double-click `settings.gradle.kts` in Android Studio to open the project and run directly
- **Option B**: Launch the app on your Android TV emulator
- Navigate to the Connect to Assurance button using arrow keys and press Enter to use Quick Connect

## Requirements
- Android TV emulator
- Android SDK API 21+
- Valid Adobe Experience Platform configuration

