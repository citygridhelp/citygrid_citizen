import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Supabase credentials are read from local.properties (git-ignored) so keys are
// never committed. Add SUPABASE_URL and SUPABASE_ANON_KEY there to go live.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val supabaseUrl: String = (localProperties.getProperty("SUPABASE_URL") ?: "").trim()
val supabaseAnonKey: String = (localProperties.getProperty("SUPABASE_ANON_KEY") ?: "").trim()
// Optional MapTiler key. Without it the app uses free OpenFreeMap vector tiles.
val mapTilerKey: String = (localProperties.getProperty("MAPTILER_KEY") ?: "").trim()

android {
    namespace = "com.example.potholereport"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "in.citygrid.citizen"
        minSdk = 24
        targetSdk = 36
        versionCode = 7
        versionName = "1.0.6"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
        buildConfigField("String", "MAPTILER_KEY", "\"$mapTilerKey\"")
        // MapLibre vector basemap (OpenFreeMap free by default; MapTiler when key is set).
        buildConfigField("boolean", "USE_MAPLIBRE_BASEMAP", "true")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // supabase-kt requires API 26; desugaring keeps this app at minSdk 24.
        isCoreLibraryDesugaringEnabled = true
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    androidResources {
        noCompress += "tflite"
    }
    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation("androidx.core:core-splashscreen:1.2.0")
    implementation("com.google.android.play:app-update-ktx:2.1.0")
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.work:work-runtime-ktx:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
    implementation("org.osmdroid:osmdroid-android:6.1.20")
    // MapLibre vector basemap (OpenFreeMap free; optional MAPTILER_KEY for MapTiler Streets).
    implementation("org.maplibre.gl:android-sdk:11.13.5")
    // Trip capture preview (#32 Phase 1–2).
    implementation("androidx.camera:camera-camera2:1.4.2")
    implementation("androidx.camera:camera-lifecycle:1.4.2")
    implementation("androidx.camera:camera-view:1.4.2")
    // Host-mode UVC when the OEM does not expose the webcam through CameraX (16 KB–aligned).
    implementation("com.herohan:UVCAndroid:1.0.13")
    // LiteRT replaces legacy tensorflow-lite; required for 16 KB page size (Android 15+ / Play).
    implementation("com.google.ai.edge.litert:litert:2.1.0")
    // Supabase backend (auth + reports table + evidence storage).
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.auth)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.storage)
    implementation(libs.ktor.client.android)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
