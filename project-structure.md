# DoctorChakraVue - Complete Folder Structure

```
D:\ChakraVue AI\Doctorchakravue\
│
├── 📁 .git/                          # Git repository
├── 📁 .gradle/                       # Gradle cache
├── 📁 .idea/                         # IDE settings
├── 📁 .kotlin/                       # Kotlin cache
├── 📁 build/                         # Build output
│   └── 📁 reports/                   # Build reports
│       ├── 📁 configuration-cache/   # Gradle config cache
│       └── 📁 problems/              # Build problem reports
│           └── 📄 problems-report.html # Build issues report
│
├── 📁 composeApp/                    # 🎯 MAIN APP MODULE
│   ├── 📁 build/                     # Module build output
│   │   ├── 📁 generated/             # Auto-generated resources
│   │   ├── 📁 intermediates/         # Intermediate build files
│   │   ├── 📁 kotlin/                # Compiled Kotlin output
│   │   ├── 📁 outputs/               # Build logs/outputs
│   │   └── 📁 tmp/                   # Temp build files
│   ├── 📄 build.gradle.kts           # Module build config
│   ├── 📄 google-services.json       # Firebase config for Android
│   │
│   └── 📁 src/
│       │
│       ├── 📁 androidMain/           # 🤖 ANDROID-SPECIFIC CODE
│       │   ├── 📄 AndroidManifest.xml         # Android app manifest
│       │   └── 📁 kotlin/com/org/doctorchakravue/
│       │       ├── 📄 MainActivity.kt         # Android entry point
│       │       ├── 📁 core/navigation/
│       │       │   └── 📄 BackHandler.android.kt # Android back button handler
│       │       ├── 📁 platform/
│       │       │   ├── 📄 Platform.android.kt     # Android platform utils
│       │       │   └── 📄 VideoCallAndroid.kt     # Android video call
│       │       └── 📁 res/                       # Android resources
│       │           ├── 📁 drawable/
│       │           ├── 📁 drawable-v24/
│       │           ├── 📁 mipmap-anydpi-v26/
│       │           ├── 📁 mipmap-hdpi/
│       │           ├── 📁 mipmap-mdpi/
│       │           ├── 📁 mipmap-xhdpi/
│       │           ├── 📁 mipmap-xxhdpi/
│       │           ├── 📁 mipmap-xxxhdpi/
│       │           └── 📁 values/
│       │
│       ├── 📁 commonMain/            # 🌐 SHARED CODE (Android + iOS)
│       │   ├── 📁 composeResources/  # Shared Compose resources
│       │   └── 📁 kotlin/com/org/doctorchakravue/
│       │       ├── 📄 App.kt                    # Main Compose app entry
│       │       ├── 📄 Platform.kt               # Platform abstraction
│       │       ├── 📁 core/
│       │       │   ├── 📁 navigation/           # Navigation logic
│       │       │   │   ├── 📄 AppNavGraph.kt
│       │       │   │   ├── 📄 NavActions.kt
│       │       │   │   ├── 📄 NavRoutes.kt
│       │       │   │   └── 📄 BackHandler.kt
│       │       │   └── 📁 ui/                   # UI components/themes
│       │       │       ├── 📁 bottomnav/
│       │       │       │   └── 📄 DoctorBottomNavBar.kt
│       │       │       ├── 📁 components/
│       │       │       │   ├── 📄 AppTopBar.kt
│       │       │       │   └── 📄 DoctorCard.kt
│       │       │       └── 📁 theme/
│       │       │           ├── 📄 ChakravueTheme.kt
│       │       │           └── 📄 Color.kt
│       │       ├── 📁 data/                     # Data layer
│       │       │   ├── 📄 DoctorApi.kt
│       │       │   ├── 📄 DoctorModels.kt
│       │       │   └── 📄 DoctorRepository.kt
│       │       ├── 📁 domain/                   # Domain models
│       │       │   └── 📄 Models.kt
│       │       ├── 📁 features/                 # Feature modules
│       │       │   ├── 📁 adherence/
│       │       │   │   ├── 📄 AdherenceScreen.kt
│       │       │   │   └── 📄 AdherenceViewModel.kt
│       │       │   ├── 📁 auth/
│       │       │   │   ├── 📄 LoginScreen.kt
│       │       │   │   └── 📄 LoginViewModel.kt
│       │       │   ├── 📁 call/
│       │       │   │   ├── 📄 DoctorCallScreen.kt
│       │       │   │   └── 📄 DoctorCallViewModel.kt
│       │       │   ├── 📁 dashboard/
│       │       │   │   ├── 📁 components/
│       │       │   │   ├── 📄 DashboardScreen.kt
│       │       │   │   ├── 📄 DashboardState.kt
│       │       │   │   └── 📄 DashboardViewModel.kt
│       │       │   ├── 📁 notifications/
│       │       │   │   ├── 📄 NotificationScreen.kt
│       │       │   │   └── 📄 NotificationViewModel.kt
│       │       │   ├── 📁 patients/
│       │       │   │   ├── 📄 PatientDetailScreen.kt
│       │       │   │   ├── 📄 PatientListScreen.kt
│       │       │   │   └── 📄 PatientViewModel.kt
│       │       │   └── 📁 submissions/
│       │       │       ├── 📄 SubmissionDetailScreen.kt
│       │       │       ├── 📄 SubmissionListScreen.kt
│       │       │       └── 📄 SubmissionViewModel.kt
│       │
│       ├── 📁 commonTest/            # Shared tests
│       │   └── 📁 kotlin/com/org/doctorchakravue/
│       │       └── 📄 ComposeAppCommonTest.kt
│       ├── 📁 iosMain/               # 🍎 iOS-SPECIFIC CODE
│       │   └── 📁 kotlin/com/org/doctorchakravue/
│       │       ├── 📄 MainViewController.kt     # iOS entry point
│       │       └── 📁 platform/
│       │           ├── 📄 Platform.ios.kt       # iOS platform utils
│       │           └── 📄 VideoCallIOS.kt       # iOS video call
│
├── 📁 gradle/                        # Gradle Wrapper and version catalog
│   ├── 📄 libs.versions.toml         # Dependency versions
│   └── 📁 wrapper/
│       ├── 📄 gradle-wrapper.jar     # Gradle wrapper binary
│       └── 📄 gradle-wrapper.properties # Gradle wrapper config
│
├── 📁 iosApp/                        # 🍎 iOS native shell project
│   ├── 📁 Configuration/
│   │   └── 📄 Config.xcconfig        # iOS build config
│   ├── 📁 iosApp/
│   │   ├── 📄 ContentView.swift      # iOS SwiftUI entry
│   │   ├── 📄 Info.plist             # iOS app info
│   │   ├── 📄 iOSApp.swift           # iOS app main
│   │   ├── 📁 Assets.xcassets/       # iOS image assets
│   │   │   ├── 📁 AccentColor.colorset/
│   │   │   ├── 📁 AppIcon.appiconset/
│   │   │   └── 📄 Contents.json
│   │   └── 📁 Preview Content/       # SwiftUI previews
│   │       └── 📁 Preview Assets.xcassets/
│   │           └── 📄 Contents.json
│   └── 📁 iosApp.xcodeproj/
│       ├── 📄 project.pbxproj        # Xcode project file
│       └── 📁 project.xcworkspace/
│           └── 📄 contents.xcworkspacedata
│
├── 📄 .gitignore                     # Git ignore rules
├── 📄 adherence_screen.dart          # Adherence tracking screen (Flutter/Dart)
├── 📄 backend_mainpy.txt             # Python backend main logic
├── 📄 backend_modelpy.txt            # Python backend model definitions
├── 📄 build.gradle.kts               # Root Gradle build config
├── 📄 call_screen.dart               # Video call screen (Flutter/Dart)
├── 📄 doc_submissions.dart           # Document submissions screen
├── 📄 firebase_options.dart          # Firebase config
├── 📄 gradle.properties              # Gradle properties
├── 📄 gradlew                        # Gradle wrapper (Unix)
├── 📄 gradlew.bat                    # Gradle wrapper (Windows)
├── 📄 local.properties               # Local SDK paths
├── 📄 main.dart                      # Main entry point (Flutter/Dart)
├── 📄 notifications_screen.dart      # Notification screen
├── 📄 patient_record_screen.dart     # Patient record screen
├── 📄 README.md                      # Project documentation
├── 📄 settings.gradle.kts            # Gradle settings
├── 📄 submission_detail_screen.dart  # Submission detail screen
├── 📄 submission_history_screen.dart # Submission history screen
```

## Key Directories Summary

| Directory | Purpose |
|-----------|---------|
| `composeApp/src/commonMain/` | Shared Kotlin code (Android + iOS) |
| `composeApp/src/androidMain/` | Android-specific implementations |
| `composeApp/src/iosMain/` | iOS-specific implementations |
| `composeApp/src/commonMain/kotlin/.../core/ui/` | UI components and themes |
| `composeApp/src/commonMain/kotlin/.../data/` | API & Data management |
| `composeApp/src/commonMain/kotlin/.../domain/` | Domain models |
| `composeApp/src/commonMain/kotlin/.../features/` | Feature modules |
| `composeApp/src/commonMain/composeResources/` | Shared resources (images, etc.) |
| `iosApp/` | iOS native shell project |
| `gradle/` | Dependency version management |
# Dart files are for reference to build this project according to features and functionality present in it