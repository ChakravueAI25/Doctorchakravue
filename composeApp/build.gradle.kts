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
            implementation("io.agora.rtc:full-sdk:4.6.1")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime-jvm:0.6.0")
            implementation("androidx.core:core-splashscreen:1.0.1") // Splash Screen API

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
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
            implementation(libs.ktor.client.logging)
            implementation(libs.kotlinx.datetime)


            // 1. Networking (Replaces 'http')
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.peekaboo.image.picker)


            // 2. Storage (Replaces 'shared_preferences')
            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.no.arg)

            // 3. Images (Replaces 'cached_network_image')
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)

            // 4. Date Formatting (Replaces 'intl')
            implementation(libs.kotlinx.datetime)

            // 5. Navigation for KMP
            implementation(libs.androidx.navigation.compose)

            // 6. Extended Material Icons
            implementation(libs.compose.icons.extended)


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
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
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

