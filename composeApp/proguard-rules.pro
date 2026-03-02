# ==========================================
# ProGuard/R8 Rules for DoctorChakravue App
# Optimized for minimum APK size
# ==========================================

# ==========================================
# General Android & Kotlin Rules
# ==========================================
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

# Keep Kotlin Metadata for reflection
-keepattributes RuntimeVisibleAnnotations

# Kotlin serialization
-keepattributes InnerClasses

# Keep Kotlin coroutines
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ==========================================
# Compose Rules
# ==========================================
-keep class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.** { *; }

# Keep Compose Runtime
-keep class androidx.compose.runtime.** { *; }

# ==========================================
# Firebase Rules
# ==========================================
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Keep FirebaseMessaging service
-keep class com.org.doctorchakravue.DoctorFirebaseMessagingService { *; }

# ==========================================
# Ktor & Networking Rules
# ==========================================
-keep class io.ktor.** { *; }
-keep class kotlin.reflect.jvm.internal.** { *; }

# Keep Ktor client classes
-keepclassmembers class io.ktor.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ==========================================
# Kotlinx Serialization Rules
# ==========================================
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep model classes
-keep class com.org.doctorchakravue.model.** { *; }
-keepclassmembers class com.org.doctorchakravue.model.** { *; }

# ==========================================
# Coil Image Loading Rules
# ==========================================
-keep class coil.** { *; }
-keep class coil3.** { *; }

# ==========================================
# Agora SDK Rules
# ==========================================
-keep class io.agora.** { *; }
-dontwarn io.agora.**

# ==========================================
# Multiplatform Settings Rules
# ==========================================
-keep class com.russhwolf.settings.** { *; }

# ==========================================
# Application Specific Rules
# ==========================================
# Keep all data classes and their members
-keepclassmembers class com.org.doctorchakravue.** {
    <init>(...);
    <fields>;
}

# Keep MainActivity
-keep class com.org.doctorchakravue.MainActivity { *; }

# ==========================================
# Optimization Settings
# ==========================================
# Enable aggressive optimizations
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Remove Kotlin assertions in release builds
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
    static void checkNotNullParameter(java.lang.Object, java.lang.String);
    static void checkExpressionValueIsNotNull(java.lang.Object, java.lang.String);
    static void checkNotNullExpressionValue(java.lang.Object, java.lang.String);
    static void checkReturnedValueIsNotNull(java.lang.Object, java.lang.String);
    static void checkReturnedValueIsNotNull(java.lang.Object, java.lang.String, java.lang.String);
    static void checkFieldIsNotNull(java.lang.Object, java.lang.String);
    static void checkFieldIsNotNull(java.lang.Object, java.lang.String, java.lang.String);
    static void checkNotNull(java.lang.Object);
    static void checkNotNull(java.lang.Object, java.lang.String);
}

# ==========================================
# Suppress Warnings
# ==========================================
-dontwarn org.slf4j.**
-dontwarn javax.annotation.**
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
-dontwarn java.lang.management.**
-dontwarn reactor.**
-dontwarn kotlinx.atomicfu.**
