plugins {
    id(Plugin.androidLibrary)
    id(Plugin.androidKotlin)
}

android {
    namespace = "com.licious.sample.design"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    android.apply {
        dataBinding.enable = true
        viewBinding.enable = true
    }
}

dependencies {
    androidX()
}