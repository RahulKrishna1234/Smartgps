# Smart GPS Tracker 🛰️

A native Android application built with **Kotlin**, **Jetpack Compose**, **Material 3**, and **MVVM architecture** for real-time location tracking, route recording, and local trip analytics.

The application operates entirely using the Android device's built-in **Fused Location Provider** and local **Room SQLite** storage. No external GPS hardware, Arduino, ESP32, remote backend, or paid APIs are required or used. All location data is strictly processed and persisted on the local device.

---

## 🌟 Features

1. **Real-Time GPS Dashboard**
   - Live telemetry: actual Latitude, Longitude, Altitude (meters / feet), and Location Accuracy (±m / ±ft).
   - Real-time speedometer displaying speed in km/h (or mph) with calculated Trip Average Speed and Trip Maximum Speed.
   - Live trip distance and elapsed time (HH:MM:SS) counters.
   - Clear indicators for GPS fix status ("GPS Fix Acquired" vs "Searching GPS..."), permission state, and device location state with zero hardcoded sample data.

2. **Live OpenStreetMap View**
   - Embedded interactive map powered by **OpenStreetMap** (`osmdroid-android`).
   - Real-time position marker following the user's location.
   - Live route polyline rendering with high-visibility trail.
   - Floating Heads-Up Display (HUD) showing speed, distance, and elapsed time directly on the map.
   - Interactive map controls: Zoom In, Zoom Out, and Center-on-Location.

3. **Trip Recording Engine**
   - **Start**, **Pause**, **Resume**, and **Stop** trip recording.
   - Stationary jitter filter (configurable 1m, 3m, 5m, 10m threshold) to eliminate GPS drift when paused or stationary.
   - Save trip with customizable title or discard recording on demand.

4. **Background Location Service & Persistent Notification**
   - Robust Android foreground service (`LocationTrackingService`) with `foregroundServiceType="location"`.
   - Ongoing persistent notification keeping GPS recording active even when the screen is turned off or the app is minimized.
   - In-notification controls: Quick Pause, Resume, and Stop action buttons.

5. **Local Trip History with Room Database**
   - Complete trip history stored locally in an embedded SQLite database using **Android Room 2.6.1**.
   - Records trip title, date/time, total distance, elapsed duration, average speed, max speed, and full coordinate route points list.
   - Detailed trip review modal with an interactive route preview map.
   - Delete individual trips or clear all history.

6. **Settings & Customization**
   - Measurement units: Metric (km, km/h, m) vs Imperial (mi, mph, ft).
   - Configurable GPS update polling rates: High Accuracy (1s), Balanced (2s), Eco (5s).
   - Jitter threshold selection.
   - Direct shortcut to Android System Location Settings.
   - Privacy guarantee declaration.

---

## 🏗️ Architecture & Technology Stack

| Layer | Component |
|---|---|
| **Language** | Kotlin 2.0.21 |
| **UI Framework** | Jetpack Compose + Material 3 (Material Design 3) |
| **Architecture** | Model-View-ViewModel (MVVM) + Kotlin Coroutines & StateFlow |
| **Location Engine** | Google Play Services Fused Location Provider (with platform LocationManager fallback) |
| **Map Rendering** | OpenStreetMap (`org.osmdroid:osmdroid-android:6.1.20`) |
| **Local Database** | Android Room 2.6.1 (with Kotlin Symbol Processing `KSP`) |
| **Build System** | Gradle 8.12 + Gradle Kotlin DSL (`build.gradle.kts`) |
| **Min SDK / Target SDK** | Min SDK 26 (Android 8.0 Oreo), Target SDK 35 (Android 15) |

---

## 🔒 Privacy & Permissions

- `android.permission.ACCESS_FINE_LOCATION`: Required for accurate GPS fixes.
- `android.permission.ACCESS_COARSE_LOCATION`: Fallback network-based location.
- `android.permission.FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_LOCATION`: Required to keep recording trips while the screen is off.
- `android.permission.POST_NOTIFICATIONS`: Android 13+ requirement for the persistent status notification.
- `android.permission.INTERNET`: Required solely for fetching OpenStreetMap map tiles via HTTPS.

> **Zero Cloud Transmission**: All GPS coordinates, route points, and statistics are stored exclusively in the app's local SQLite database.

---

## 🧪 Testing

The project includes JVM unit tests:
- `DistanceCalculatorTest`: Validates the mathematical Haversine great-circle calculation, multi-segment route cumulative distances, speed unit conversions (m/s to km/h, km/h to mph), and average speed formulas.
- `TripStateManagerTest`: Validates trip state machine transitions (`IDLE -> RECORDING -> PAUSED -> RECORDING -> STOPPED -> IDLE`), idempotent calls, and metric resets.

Run tests via Gradle:
```bash
./gradlew test
```

---

## 🚀 Building the Project

### Prerequisites
- Android SDK 35 (`compileSdk = 35`, `minSdk = 26`)
- JDK 17 or JDK 21 (Temurin JDK 21 recommended)

### Build Debug APK
```bash
./gradlew assembleDebug
```
The output APK is generated at:
`app/build/outputs/apk/debug/app-debug.apk`
