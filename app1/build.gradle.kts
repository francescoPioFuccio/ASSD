plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.app1"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.app1"
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
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    dependencies {

        // ----- CORE ANDROIDX & UI -----
        implementation(libs.androidx.core.ktx)
        implementation(libs.androidx.appcompat)
        implementation(libs.material)
        implementation(libs.androidx.activity)
        implementation("androidx.constraintlayout:constraintlayout:2.1.4")
        implementation(libs.androidx.annotation)
        implementation(libs.androidx.lifecycle.livedata.ktx)
        implementation(libs.androidx.lifecycle.viewmodel.ktx)
        implementation("androidx.lifecycle:lifecycle-viewmodel:2.7.0")
        implementation("androidx.preference:preference:1.2.0")

        // ----- NAVIGATION -----
        implementation("androidx.navigation:navigation-fragment:2.7.7")
        implementation("androidx.navigation:navigation-ui:2.7.7")

        // ----- GOOGLE SERVICES -----
        implementation("com.google.android.gms:play-services-maps:18.2.0")
        implementation("com.google.android.gms:play-services-location:21.0.1")

        // ----- NETWORK -----
        // Client HTTP principale. Usato da ApiService e da Retrofit.
        implementation("com.squareup.okhttp3:okhttp:4.12.0")
        // Retrofit (layer di astrazione sopra OkHttp, non è in conflitto)
        implementation("com.squareup.retrofit2:retrofit:2.9.0")
        implementation("com.squareup.retrofit2:converter-gson:2.9.0")

        // ----- TESTING -----
        implementation(libs.mediation.test.suite)
        testImplementation(libs.junit)
        androidTestImplementation(libs.androidx.junit)
        androidTestImplementation(libs.androidx.espresso.core)
    }
}