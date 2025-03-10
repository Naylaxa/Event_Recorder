plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.recorder" // Pastikan namespace sesuai dengan package aplikasi
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.recorder"
        minSdk = 24
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation("com.github.bumptech.glide:glide:4.12.0")
    implementation(libs.firebase.database)
    annotationProcessor("com.github.bumptech.glide:compiler:4.12.0")
    implementation("androidx.cardview:cardview:1.0.0")

    // Gunakan Firebase BOM
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))// Versi terbaru BOM
    implementation("com.google.firebase:firebase-auth-ktx") // Firebase Auth
    implementation("com.google.firebase:firebase-firestore-ktx") // Firestore
    implementation("com.google.firebase:firebase-database-ktx") // Realtime Database
    implementation("com.google.firebase:firebase-storage-ktx") // Firebase Storage
    implementation("com.google.firebase:firebase-analytics")
//    implementation ("com.google.android.gms:play-services-auth:20.6.0'")

    // Testing dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
