plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.google.firebase.crashlytics)
}

android {
    namespace = "com.example.ventaplus"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.ventaplus"
        minSdk = 31
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
    buildFeatures {
        compose = true
    }
}

dependencies {
// CameraX core y view (usar solo la versión 1.3.0 para evitar conflictos)
implementation("androidx.camera:camera-core:1.3.0")
implementation("androidx.camera:camera-camera2:1.3.0")
implementation("androidx.camera:camera-lifecycle:1.3.0")
implementation("androidx.camera:camera-view:1.3.0")
implementation("androidx.camera:camera-extensions:1.3.0")

    implementation ("androidx.compose.material3:material3:1.1.0")


    implementation ("androidx.compose.material3:material3:1.2.1")
    implementation ("androidx.compose.material:material-icons-extended:1.6.0")

// ML Kit Barcode Scanner (usar solo la versión 17.2.0 más reciente)
implementation("com.google.mlkit:barcode-scanning:17.2.0")

// Guava para ListenableFuture necesario para CameraX
implementation("com.google.guava:guava:31.1-android")

// Compose y otras dependencias (usa solo una vez cada librería)
implementation("androidx.compose.material:material-icons-extended:1.5.1")
implementation("androidx.compose.animation:animation:1.5.1")
implementation("androidx.compose.material3:material3:1.2.0")
implementation("androidx.core:core-ktx:1.12.0")
implementation("androidx.navigation:navigation-compose:2.7.7")

// Dependencias usando libs (asegúrate que libs estén bien configuradas)
implementation(libs.androidx.core.ktx)
implementation(libs.androidx.lifecycle.runtime.ktx)
implementation(libs.androidx.activity.compose)
implementation(platform(libs.androidx.compose.bom))
implementation(libs.androidx.ui)
implementation(libs.androidx.ui.graphics)
implementation(libs.androidx.ui.tooling.preview)
implementation(libs.androidx.material3)
implementation(libs.firebase.auth)
implementation(libs.androidx.credentials)
implementation(libs.androidx.credentials.play.services.auth)
implementation(libs.googleid)
implementation(libs.firebase.database)
implementation(libs.firebase.firestore)
implementation(libs.firebase.storage)
implementation(libs.firebase.crashlytics)
implementation(libs.firebase.messaging)

testImplementation(libs.junit)
androidTestImplementation(libs.androidx.junit)
androidTestImplementation(libs.androidx.espresso.core)
androidTestImplementation(platform(libs.androidx.compose.bom))
androidTestImplementation(libs.androidx.ui.test.junit4)
debugImplementation(libs.androidx.ui.tooling)
debugImplementation(libs.androidx.ui.test.manifest)
}
