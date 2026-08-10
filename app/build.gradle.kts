plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.insightface.recognizer"
    compileSdk = 35
    ndkVersion = "28.1.13356709"

    defaultConfig {
        applicationId = "com.insightface.recognizer"
        minSdk = 24
        targetSdk = 35
        // versionCode/versionName are bumped by CI on release; see .github/workflows/android.yml
        versionCode = 1
        versionName = "1.0.0"

        ndk {
            // InspireFace 1.2.0 ships arm64-v8a / armeabi-v7a only (per official example).
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Fall back to the debug signing key when no keystore.properties is present, so CI
            // can produce an installable release APK out of the box. Provide a keystore.properties
            // (see README) for production signing.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    // Inject the GitHub owner/repo used by the in-app update checker. Override these in
    // your own fork's gradle.properties (GITHUB_REPO_OWNER / GITHUB_REPO_NAME).
    buildTypes.onEach { bt ->
        bt.buildConfigField(
            "String",
            "GITHUB_REPO_OWNER",
            "\"${project.findProperty("GITHUB_REPO_OWNER") ?: "nextlevelbuilder"}\""
        )
        bt.buildConfigField(
            "String",
            "GITHUB_REPO_NAME",
            "\"${project.findProperty("GITHUB_REPO_NAME") ?: "InsightFaceRecognizer"}\""
        )
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Official InspireFace Android SDK (deepinsight/insightface). Model packs (Pikachu /
    // Megatron) are bundled inside the AAR and unpacked by GlobalLaunch on first run.
    implementation(libs.inspireface.android.sdk)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.exifinterface)
    implementation(libs.coil.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    debugImplementation(libs.androidx.ui.tooling)
}
