# Doctor ChakraVue - Project File Structure

## Overview
This is a Kotlin Multiplatform (KMP) project for a doctor-facing healthcare application. The structure follows a clean architecture pattern with separation of concerns across app flow, data, models, platform-specific code, and UI.

## Complete Project Structure

```
Doctorchakravue/
├── README.md
├── project-structure.md
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
├── local.properties
│
├── build/                                    # Gradle build output
│   └── reports/
│       ├── configuration-cache/
│       └── problems/
│
├── composeApp/
│   ├── build.gradle.kts
│   ├── google-services.json                 # Firebase config for Android
│   │
│   ├── build/                               # Build artifacts
│   │   ├── generated/
│   │   ├── intermediates/
│   │   ├── kotlin/
│   │   ├── outputs/
│   │   └── tmp/
│   │
│   └── src/
│       ├── commonMain/                      # Shared Kotlin code
│       │   └── kotlin/com/org/doctorchakravue/
│       │       │
│       │       ├── app/                     # 🎯 App flow & navigation
│       │       │   ├── App.kt                       # Main app entry with NavHost
│       │       │   ├── AppBackHandler.kt            # expect/actual back handler
│       │       │   ├── AppScreen.kt                 # Screen route definitions
│       │       │   └── Navigator.kt                 # Navigation utilities
│       │       │
│       │       ├── data/                    # 🔌 Backend & repositories
│       │       │   ├── ApiRepository.kt             # API communication (HTTP client)
│       │       │   └── SessionManager.kt            # Session/login state management
│       │       │
│       │       ├── model/                   # 📦 Data contracts
│       │       │   └── Models.kt                    # All @Serializable data classes
│       │       │       ├── LoginResponse
│       │       │       ├── Submission
│       │       │       ├── SubmissionDetail
│       │       │       ├── PatientRecord
│       │       │       ├── PatientSimple
│       │       │       ├── AdherencePatient
│       │       │       ├── VideoCallRequest
│       │       │       ├── NotificationItem
│       │       │       └── ... (20+ more)
│       │       │
│       │       ├── platform/                # 🔧 Platform-specific code
│       │       │   ├── Platform.kt                  # expect Platform interface
│       │       │   ├── SystemTime.kt               # expect time utilities
│       │       │   └── BitmapCapture.kt            # expect screenshot/bitmap capture
│       │       │
│       │       └── ui/                      # 🎨 User Interface (all screens)
│       │           ├── components/
│       │           │   ├── BottomNavBar.kt        # Navigation bar (5 items)
│       │           │   └── AppTopBar.kt           # Top app bar component
│       │           │
│       │           ├── theme/
│       │           │   ├── Theme.kt               # MaterialTheme + gradient background
│       │           │   └── Color.kt               # Brand colors (Green, Blue)
│       │           │
│       │           ├── DashboardScreen.kt         # Home screen (includes ViewModel & State)
│       │           │   ├── DashboardState         # UI state
│       │           │   ├── DashboardViewModel     # Business logic
│       │           │   ├── UrgentReviewCard       # Horizontal scrolling cards
│       │           │   ├── QuickActionItem        # Action buttons
│       │           │   └── HistoryItem            # List item
│       │           │
│       │           ├── PatientsScreen.kt          # Patient list
│       │           │   └── PatientCard            # Card layout
│       │           │
│       │           ├── NotificationsScreen.kt     # Compose & send notifications
│       │           │   └── (includes dialog for patient selection)
│       │           │
│       │           ├── ProfileScreen.kt           # Doctor profile (name, email, logout)
│       │           │   └── ProfileInfoRow         # Info display component
│       │           │
│       │           ├── LoginScreen.kt             # Login form (includes ViewModel)
│       │           │   ├── LoginState             # State management
│       │           │   └── LoginViewModel         # Auth logic
│       │           │
│       │           ├── PainScaleHistoryScreen.kt  # History in grid layout (2 cols)
│       │           │   └── PainScaleCard          # Grid item card
│       │           │
│       │           ├── PainScaleDetailScreen.kt   # Detail view (includes ViewModel)
│       │           │   ├── PainScaleDetailState
│       │           │   ├── PainScaleDetailViewModel
│       │           │   ├── SymptomBadge           # Redness/Swelling/Discharge display
│       │           │   └── DetailRow              # Dialog content
│       │           │
│       │           ├── AdherenceScreen.kt         # Drug adherence tracking
│       │           │   ├── PatientAdherenceCard   # List item with adherence %
│       │           │   ├── PatientAdherenceDetail # Detail view with expanded days
│       │           │   ├── DayAccordion           # Expandable day view
│       │           │   └── MedicationEntryRow     # Medication taken checkbox
│       │           │
│       │           ├── VideoCallListScreen.kt     # Video call requests list
│       │           │   └── VideoCallRequestCard   # Request card item
│       │           │
│       │           ├── VideoCallDetailScreen.kt   # Single call request details
│       │           │   └── DetailRow              # Info rows
│       │           │
│       │           └── DoctorCallScreen.kt        # Active video call UI
│       │               └── CallControlButton      # Mute/Camera/End buttons
│       │
│       ├── androidMain/                    # 🤖 Android-specific code
│       │   └── kotlin/com/org/doctorchakravue/
│       │       ├── MainActivity.kt                 # App entry point
│       │       ├── app/
│       │       │   └── AppBackHandler.android.kt  # actual BackHandler using AndroidX
│       │       └── platform/
│       │           ├── Platform.android.kt        # actual getPlatform() returns AndroidPlatform
│       │           ├── SystemTime.android.kt      # actual System.currentTimeMillis()
│       │           └── BitmapCapture.android.kt   # actual bitmap capture
│       │
│       ├── commonTest/                    # Shared tests
│       │   └── kotlin/...
│       │
│       ├── iosMain/                       # 🍎 iOS-specific code
│       │   └── kotlin/com/org/doctorchakravue/
│       │       ├── MainViewController.kt          # iOS app entry
│       │       ├── app/
│       │       │   └── AppBackHandler.ios.kt      # actual no-op (uses gesture nav)
│       │       └── platform/
│       │           ├── Platform.ios.kt            # actual getPlatform() returns IOSPlatform
│       │           ├── SystemTime.ios.kt          # actual using NSDate
│       │           └── BitmapCapture.ios.kt       # actual bitmap capture placeholder
│       │
│       └── resources/                     # UI resources
│           └── (images, icons, etc.)
│
├── gradle/
│   ├── libs.versions.toml                 # Dependency versions (centralized)
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
│
└── iosApp/                                # 🍎 iOS native wrapper
    ├── Configuration/
    │   └── Config.xcconfig
    ├── iosApp/
    │   ├── ContentView.swift
    │   ├── Info.plist
    │   ├── iOSApp.swift
    │   ├── Assets.xcassets/
    │   └── Preview Content/
    └── iosApp.xcodeproj/
        ├── project.pbxproj
        └── project.xcworkspace/
```

## Key Features by Layer

### App Layer (`app/`)
- **App.kt**: NavHost with 11 composable routes
- **Navigator.kt**: Utils for bottom nav visibility
- **AppScreen.kt**: Sealed class for type-safe routing
- **AppBackHandler.kt**: Platform-specific back button handling

### Data Layer (`data/`)
- **ApiRepository.kt**: 
  - Ktor HTTP client for API calls
  - Methods: login, getSubmissions, getPatients, getNotifications, etc.
  - Base URL: https://doctor.chakravue.co.in
- **SessionManager.kt**: 
  - Stores doctor ID, name, email in local settings
  - Handles login/logout state

### Model Layer (`model/`)
- **20+ data classes** (all @Serializable for JSON)
- Key models:
  - `Submission` (pain scale submission with image)
  - `PatientRecord` (full medical history)
  - `AdherencePatient` (medication tracking)
  - `VideoCallRequest` (call metadata)

### Platform Layer (`platform/`)
- **Platform.kt**: Android/iOS device info
- **SystemTime.kt**: Current time in milliseconds
- **BitmapCapture.kt**: Screenshot/image capture placeholder
- Expect/actual pattern for multiplatform support

### UI Layer (`ui/`)
**Main Screens (12 total):**
1. `LoginScreen` - Email + password form
2. `DashboardScreen` - Home with urgent reviews, calls, quick actions
3. `PatientsScreen` - Patient list
4. `PainScaleHistoryScreen` - History grid (2 cols)
5. `PainScaleDetailScreen` - Detail with symptoms, history, video call
6. `AdherenceScreen` - Medication tracking timeline
7. `NotificationsScreen` - Send broadcasts to patients
8. `ProfileScreen` - Doctor info + logout
9. `VideoCallListScreen` - Incoming call requests
10. `VideoCallDetailScreen` - Single call details
11. `DoctorCallScreen` - Active call UI
12. (Component files for reusable UI parts)

**Navigation Routes:**
```
login → dashboard ─┬─ patients
                  ├─ pain_scale_history → submission/{data} → call/{appId}/{token}/{channelName}
                  ├─ notifications
                  ├─ profile
                  ├─ adherence
                  └─ video_call_list → video_call_detail/{callId} → call/...
```

## Build & Dependencies

### Gradle Build System
- **composeApp/build.gradle.kts**: Main module configuration
- **settings.gradle.kts**: Project settings
- **gradle/libs.versions.toml**: Centralized dependency versions

### Key Dependencies
- **Compose**: Latest Material3
- **Ktor**: HTTP client
- **kotlinx.serialization**: JSON serialization
- **Coil3**: Image loading
- **Russhwolf.settings**: Multiplatform preferences
- **Firebase**: Android only

### Build Variants
- **Android**: Debug + Release (with Gradle variants)
- **iOS**: Simulator + Device (via Xcode)

## Architecture Decisions

1. **Flat UI Structure**: All screens in `ui/` folder (not feature-based nested)
2. **Consolidated Models**: All data classes in single `Models.kt`
3. **ViewModel in Screens**: Small ViewModels merged into their Screen files
4. **Expect/Actual Pattern**: Platform code in `platform/` folder
5. **Session Management**: Extracted to dedicated `SessionManager.kt`
6. **Component Reusability**: Common UI in `components/` subfolder

## File Counts
- **Kotlin source files**: ~30 (commonMain)
- **Android-specific files**: ~4
- **iOS-specific files**: ~4
- **Total Composables**: 50+
- **Data classes**: 20+

## Build Status
✅ **Successful** - Android debug compilation passes
⚠️ **Warnings**: 
- Deprecated icon usage (use AutoMirrored versions)
- Expect/actual classes in Beta
- KMP/AGP compatibility warnings (expected for current AGP version)

---

*Last Updated: January 20, 2026*
*Project Type: Kotlin Multiplatform (KMP) for Android & iOS*
