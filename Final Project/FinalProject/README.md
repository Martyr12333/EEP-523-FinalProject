# Smart Bike - Cycling Companion App

## Overview
Smart Bike is a comprehensive cycling companion application that helps cyclists track their rides, monitor performance metrics, and manage their cycling activities. The app provides real-time tracking, statistics, and a user-friendly interface for both casual and serious cyclists.

## Features
- **Real-time Ride Tracking**: Track your cycling routes with GPS
- **Performance Metrics**: Monitor distance, speed, duration, and calories burned
- **Ride History**: View and analyze past rides
- **Statistics Dashboard**: Get insights into your cycling performance
- **User-friendly Interface**: Clean and intuitive design for easy navigation

## Technical Implementation
The app is built using:
- Kotlin as the primary programming language
- Android Jetpack components (ViewModel, LiveData, Navigation)
- Material Design components for UI
- Room database for local storage
- OpenStreetMap for map visualization
- Location services for GPS tracking

## Development Time
The development of this application took approximately 10 hours, including:
- Initial setup and architecture design: 3 hours
- Core functionality implementation: 3 hours
- UI/UX design and implementation: 3 hours
- Testing and debugging: 1 hours

## Challenging Aspects
1. **Real-time Location Tracking**: Implementing accurate GPS tracking while managing battery consumption was challenging. The solution involved optimizing location update intervals and implementing efficient background services.

2. **Data Persistence**: Managing ride data storage and retrieval required careful consideration of database schema and query optimization.

3. **UI State Management**: Handling complex UI states during ride tracking (start, pause, resume, stop) while maintaining a smooth user experience was particularly challenging.

4. **Permission Handling**: Managing various Android permissions (location, Bluetooth, notifications) across different Android versions required careful implementation.

## Resources Used
### Documentation
- [Android Developer Documentation](https://developer.android.com/docs)
- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)
- [Material Design Guidelines](https://material.io/design)
- [OpenStreetMap Documentation](https://wiki.openstreetmap.org/wiki/Main_Page)

### Libraries and Tools
- [Android Jetpack](https://developer.android.com/jetpack)
- [Room Database](https://developer.android.com/training/data-storage/room)
- [OSMDroid](https://github.com/osmdroid/osmdroid)
- [Material Components for Android](https://github.com/material-components/material-components-android)

### Learning Resources
- [Android Developers YouTube Channel](https://www.youtube.com/user/androiddevelopers)
- [Google Codelabs](https://codelabs.developers.google.com/)
- [Stack Overflow](https://stackoverflow.com/)

## Future Improvements
1. Social features for sharing rides
2. Integration with fitness platforms
3. Advanced analytics and performance insights
4. Custom route planning
5. Weather integration for ride planning

## Installation
1. Clone the repository
2. Open the project in Android Studio
3. Build and run the application

## Requirements
- Android 6.0 (API level 23) or higher
- Google Play Services
- Location services enabled
- Internet connection for map data

# SmartBike Android App

A modern cycling tracking app built with Android's latest architecture components, featuring real-time GPS tracking, Bluetooth LE connectivity, and crash detection.

## 🏗️ Architecture

The app has been completely redesigned with proper Android architecture patterns:

### **Data Layer**
- **Room Database**: Persistent storage for ride history
- **Repository Pattern**: Clean data access abstraction
- **Type Converters**: JSON serialization for complex data types

### **Business Logic Layer**  
- **ViewModel**: UI state management with StateFlow
- **Foreground Service**: Background ride tracking
- **Use Cases**: Clean separation of business logic

### **Presentation Layer**
- **MVVM Pattern**: Reactive UI with data binding
- **Navigation Component**: Type-safe navigation
- **Material Design 3**: Modern, accessible UI

## 📱 Features

### **Core Functionality**
- ✅ **Real-time GPS Tracking**: Accurate location and route recording
- ✅ **Live Ride Dashboard**: Speed, distance, time, heart rate, calories
- ✅ **Pause/Resume/Stop**: Full ride control with state persistence
- ✅ **Ride History**: Persistent storage with statistics
- ✅ **Background Tracking**: Continues recording when app is minimized

### **Advanced Features**
- ✅ **Bluetooth LE Integration**: Heart rate monitors and cycling sensors
- ✅ **Crash Detection**: Accelerometer-based safety monitoring
- ✅ **Statistics Dashboard**: Overall performance metrics
- ✅ **English UI**: Complete internationalization support

## 🛠️ Technical Implementation

### **Database Design**
```kotlin
@Entity(tableName = "rides")
data class RideEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long?,
    val duration: Long,
    val distance: Double,
    val averageSpeed: Double,
    val maxSpeed: Double,
    val calories: Int,
    val averageHeartRate: Int,
    val maxHeartRate: Int,
    val routePoints: List<RoutePoint>,
    val status: RideStatus,
    val pausedDuration: Long = 0
)
```

### **Service Architecture**
- **RideTrackingService**: Foreground service for continuous tracking
- **LocationTracker**: GPS and route management
- **BleManager**: Bluetooth device connectivity
- **CrashDetection**: Accelerometer monitoring

### **State Management**
```kotlin
data class RideUiState(
    val isRiding: Boolean = false,
    val isPaused: Boolean = false,
    val isStarting: Boolean = false,
    val isStopping: Boolean = false,
    val error: String? = null
)
```

## 🚀 Getting Started

### **Prerequisites**
- Android Studio Hedgehog (2023.1.1) or later
- Android SDK API 24+ (Android 7.0)
- Kotlin 1.9+

### **Dependencies**
```kotlin
// Core
implementation("androidx.core:core-ktx:1.12.0")
implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
implementation("androidx.navigation:navigation-fragment-ktx:2.7.6")

// Database
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
kapt("androidx.room:room-compiler:2.6.1")

// Location & Bluetooth
implementation("com.google.android.gms:play-services-location:21.0.1")
implementation("androidx.bluetooth:bluetooth:1.0.0-alpha02")
```

### **Permissions**
The app requires several permissions for full functionality:
- `ACCESS_FINE_LOCATION` - GPS tracking
- `ACCESS_BACKGROUND_LOCATION` - Background tracking
- `BLUETOOTH_SCAN` / `BLUETOOTH_CONNECT` - Device connectivity
- `POST_NOTIFICATIONS` - Foreground service notifications

## 📊 App Flow

### **Home Screen** (`HomeFragment`)
- Overview statistics (total distance, time, rides, calories)
- Recent rides list
- Start ride button with permission handling

### **Live Tracking** (`DashboardFragment`) 
- Real-time metrics display
- GPS coordinate tracking with route points
- Pause/Resume/Stop controls
- Ride summary dialog

### **History** (`HistoryFragment`)
- Complete ride history from database
- Individual ride details
- Delete functionality

### **Settings** (`SettingsFragment`)
- Bluetooth device management
- Crash detection configuration
- Emergency contact setup

## 🎨 UI Design

### **Design System**
- **Primary**: Blue (#1976D2) - Trust and reliability
- **Secondary**: Orange (#FF9800) - Energy and activity  
- **Cards**: Material Design 3 with 12dp radius
- **Typography**: Roboto with clear hierarchy

### **Responsive Layout**
- **Grid System**: Consistent spacing and alignment
- **Adaptive Cards**: Flexible content containers
- **Bottom Navigation**: Primary app navigation
- **Floating Actions**: Context-aware controls

## 🔧 Key Improvements

### **From Previous Version**
1. **Complete Architecture Redesign**: MVVM + Repository pattern
2. **Database Integration**: Room for persistent storage
3. **Background Service**: Proper foreground service implementation
4. **State Management**: Reactive UI with StateFlow
5. **English UI**: Complete localization
6. **Error Handling**: Comprehensive error states
7. **Memory Management**: Proper lifecycle handling

### **Performance Optimizations**
- **Lazy Loading**: Efficient data loading patterns
- **Memory Leaks**: Proper binding cleanup
- **Background Processing**: Optimized service operations

## 📁 Project Structure

```
app/src/main/java/com/example/finalproject/
├── data/
│   ├── RideEntity.kt          # Database entities
│   ├── RideDao.kt             # Data access objects
│   ├── RideDatabase.kt        # Room database
│   └── RideRepository.kt      # Data repository
├── service/
│   └── RideTrackingService.kt # Background tracking service
├── ui/
│   ├── home/HomeFragment.kt   # Main dashboard
│   ├── dashboard/DashboardFragment.kt # Live tracking
│   ├── history/HistoryFragment.kt     # Ride history
│   └── settings/SettingsFragment.kt   # App settings
├── utils/
│   ├── LocationTracker.kt     # GPS management
│   ├── BleManager.kt          # Bluetooth connectivity
│   └── CrashDetection.kt      # Safety monitoring
├── viewmodel/
│   └── RideViewModel.kt       # UI state management
└── adapter/
    └── RecentRidesAdapter.kt  # RecyclerView adapter
```

## 🧪 Testing

The app includes comprehensive testing coverage:
- **Unit Tests**: ViewModel and Repository logic
- **Integration Tests**: Database operations
- **UI Tests**: Navigation and user interactions

## 📱 Minimum Requirements

- **Android Version**: 7.0 (API 24+)
- **RAM**: 2GB minimum
- **Storage**: 100MB
- **Hardware**: GPS, Bluetooth LE, Accelerometer

## 🎯 Future Enhancements

- [ ] Google Maps integration for route visualization
- [ ] Social features and ride sharing
- [ ] Advanced analytics and performance insights
- [ ] Wear OS companion app
- [ ] Cloud sync and backup