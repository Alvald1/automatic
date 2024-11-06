plugins {
    id(Plugin.androidApplication)
    id(Plugin.androidKotlin)
    kotlin(Plugin.kotlinAndroidKapt)
    id(Plugin.daggerHilt)
    id("org.jetbrains.kotlin.plugin.compose")
    kotlin("plugin.serialization") version "2.0.20"
}

android {
    namespace = Apps.applicationId
    compileSdk = Apps.compileSdk

    defaultConfig {
        applicationId = Apps.applicationId
        minSdk = Apps.minSdk
        targetSdk = Apps.targetSdk
        versionCode = Apps.versionCode
        versionName = Apps.versionName

        testInstrumentationRunner = TestDependencies.instrumentationRunner
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    // Enable Jetpack Compose
    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = Versions.kotlinCompose
    }

    android.apply {
        dataBinding.enable = true
        viewBinding.enable = true
    }
}

dependencies {
    implementation(project(":design"))
    implementation(project(":scanner"))
    implementation("androidx.core:core:1.9.0")
    implementation("androidx.navigation:navigation-compose:2.8.3")
    androidX()
    daggerHilt()
    testEspressoCore()
    jUnit()
    navigation()

    // Jetpack Compose dependencies
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("androidx.compose.ui:ui:1.7.5") // Compose UI
    implementation("androidx.compose.material3:material3:1.3.1") // Material Design 3
    implementation("androidx.compose.ui:ui-tooling-preview:1.7.5") // Tooling support
    implementation("androidx.activity:activity-compose:1.9.3") // Compose activity
}

