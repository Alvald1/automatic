plugins {
    id(Plugin.androidLibrary)
    id(Plugin.androidKotlin)
}

android {
    namespace = "com.automatic.scanner"
    compileSdk = Apps.compileSdk

    defaultConfig {
        minSdk = Apps.minSdk

        testInstrumentationRunner = TestDependencies.instrumentationRunner
        consumerProguardFiles("consumer-rules.pro")
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
    implementation("com.google.android.gms:play-services-mlkit-barcode-scanning:18.3.1")
    mlkit()
    cameraX()
    lifeCycleRuntimeKtx()
    coroutines()
}