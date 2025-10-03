plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
    id("kotlin-parcelize")
}

android {
    namespace = "com.ridwanelly.ingatin"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ridwanelly.ingatin"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.database)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // Firebase BoM (Bill of Materials) - Mengelola versi library Firebase secara otomatis
    implementation(platform("com.google.firebase:firebase-bom:33.1.1"))

// Firebase Authentication
    implementation("com.google.firebase:firebase-auth-ktx")

// Cloud Firestore
    implementation("com.google.firebase:firebase-firestore-ktx")

// Android KTX (ekstensi Kotlin untuk Android)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")

// Komponen UI Material Design
    implementation("com.google.android.material:material:1.12.0")

// ConstraintLayout untuk desain layout
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

// ViewModel & LiveData (untuk arsitektur yang baik)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.2")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.2")

// WorkManager untuk notifikasi background
    implementation("androidx.work:work-runtime-ktx:2.9.0")

// Google Play Services
    implementation("com.google.android.gms:play-services-auth:20.5.0")
    implementation("com.google.android.gms:play-services-base:18.2.0")
}