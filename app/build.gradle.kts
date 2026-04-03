plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val debugWebAppUrl = providers.gradleProperty("debugWebAppUrl")
    .orElse("http://10.0.2.2:3000/")
val releaseWebAppUrl = providers.gradleProperty("releaseWebAppUrl")
    .orElse("https://oclock-bell.netlify.app/")

android {
    namespace   = "com.example.oclockbell"
    compileSdk  = 34

    defaultConfig {
        applicationId = "com.example.oclockbell"
        minSdk        = 26
        targetSdk     = 34
        versionCode   = 1
        versionName   = "1.0"
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            buildConfigField("String", "WEB_APP_URL", "\"${debugWebAppUrl.get()}\"")
            manifestPlaceholders["usesCleartextTraffic"] = "true"
        }

        release {
            isMinifyEnabled = false
            buildConfigField("String", "WEB_APP_URL", "\"${releaseWebAppUrl.get()}\"")
            manifestPlaceholders["usesCleartextTraffic"] = "false"
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
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
}
