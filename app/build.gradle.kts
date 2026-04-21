plugins {
    id("com.android.application")
    alias(libs.plugins.jetbrains.kotlin.android)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.hk.vcab"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.hk.vcab"
        minSdk = 24
        targetSdk = 35
        versionCode = 13
        versionName = "1.1.2"

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


    implementation("androidx.core:core-ktx:1.12.0") // downgraded for SDK 34 compatibility

    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(platform("com.google.firebase:firebase-bom:33.13.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation ("com.google.android.gms:play-services-auth:20.7.0")

    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.android.material:material:1.9.0")

    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    implementation ("com.google.android.material:material:1.12.0")
    implementation ("androidx.cardview:cardview:1.0.0")
    implementation ("com.airbnb.android:lottie:6.1.0")
    implementation ("com.google.firebase:firebase-messaging:25.0.0")

    implementation ("androidx.navigation:navigation-fragment-ktx:2.9.3")
    implementation ("androidx.navigation:navigation-ui-ktx:2.9.3")





}