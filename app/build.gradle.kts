import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val debugWebAppUrl = providers.gradleProperty("debugWebAppUrl")
    .orElse("http://10.0.2.2:3000/")
val releaseWebAppUrl = providers.gradleProperty("releaseWebAppUrl")
    .orElse("https://oclock-bell.netlify.app/")

// 릴리즈 서명 설정 — keystore.properties(버전관리 제외)가 있을 때만 활성화
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) load(keystorePropsFile.inputStream())
}

android {
    namespace   = "com.example.oclockbell"
    compileSdk  = 34

    defaultConfig {
        applicationId = "com.example.oclockbell"
        minSdk        = 26
        targetSdk     = 34
        versionCode   = 2
        versionName   = "2.0.0"
    }

    buildFeatures {
        buildConfig = true
    }

    signingConfigs {
        if (keystorePropsFile.exists()) {
            create("release") {
                storeFile     = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias      = keystoreProps.getProperty("keyAlias")
                keyPassword   = keystoreProps.getProperty("keyPassword")
            }
        }
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
            if (keystorePropsFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
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
