import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.googleGmsGoogleServices) // Firebase
}

kotlin {
    @Suppress("DEPRECATION")
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp) // Android Engine
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.firebase.messaging.ktx) // Firebase Cloud Messaging
            // Full SDK for video calling - if only voice is needed, use "voice-sdk" instead (~50% smaller)
            implementation("io.agora.rtc:full-sdk:4.6.1")

            // CameraX for local camera preview
            implementation("androidx.camera:camera-core:1.3.1")
            implementation("androidx.camera:camera-camera2:1.3.1")
            implementation("androidx.camera:camera-lifecycle:1.3.1")
            implementation("androidx.camera:camera-view:1.3.1")

        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin) // iOS Engine
        }

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            // Serialization
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

            // Networking (Ktor)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)

            // Storage
            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.no.arg)

            // Images (Coil)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)

            // Date/Time
            implementation(libs.kotlinx.datetime)

            // Navigation
            implementation(libs.androidx.navigation.compose)

            // Extended Material Icons
            implementation(libs.compose.icons.extended)

            // Image Picker
            implementation(libs.peekaboo.image.picker)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.org.doctorchakravue"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.org.doctorchakravue"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        // Enable vector drawable support for smaller APK
        vectorDrawables.useSupportLibrary = true
    }

    // Resource configurations - only include necessary languages
    androidResources {
        localeFilters += listOf("en")
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/NOTICE.md"
            excludes += "/META-INF/LICENSE.md"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/*.kotlin_module"
            excludes += "**/*.properties"
            excludes += "DebugProbesKt.bin"
        }
    }

    buildTypes {
        getByName("release") {
            // Enable R8 code shrinking, obfuscation, and optimization
            isMinifyEnabled = true
            // Enable resource shrinking to remove unused resources
            isShrinkResources = true
            // Use R8 full mode for more aggressive optimizations
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // Single universal APK with all ABIs (no splits)
    // Note: For Play Store distribution, consider using App Bundle (.aab) instead
    // which automatically optimizes delivery per device

    // Optimize PNG images
    buildFeatures {
        buildConfig = true
    }

    // Lint options to catch unused resources
    lint {
        checkReleaseBuilds = true
        abortOnError = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // implementation(libs.firebase.messaging) // Disabled - not needed for now
    debugImplementation(compose.uiTooling)
}

