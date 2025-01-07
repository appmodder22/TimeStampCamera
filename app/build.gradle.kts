plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.tis.timestampcamerafree"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.tis.timestampcamerafree"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.2"

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
        viewBinding = true
    }
    dataBinding {
        enable = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // CameraX core library
    implementation (libs.androidx.camera.core.v110beta01)
    // CameraX Camera2 extensions
    implementation (libs.androidx.camera.camera2)
    // CameraX Lifecycle library
    implementation (libs.androidx.camera.lifecycle.v110beta01)
    implementation (libs.androidx.camera.video)
    // CameraX View class
    implementation (libs.androidx.camera.view.v110beta01)
    implementation (libs.androidx.camera.extensions.v110beta01)
    //Dexter
    implementation (libs.dexter)

    implementation (libs.locus.android.v412)

    // CameraX Extensions library
    // implementation "androidx.camera:camera-extensions:$camerax_version"
    implementation (libs.play.services.location) // For Location
    implementation (libs.androidx.lifecycle.livedata.ktx)
}