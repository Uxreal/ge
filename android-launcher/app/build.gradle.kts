plugins {
    id("lumen.android.application")
    id("lumen.android.compose")
    id("lumen.hilt")
}

android {
    namespace = "dev.lumen.launcher"

    defaultConfig {
        applicationId = "dev.lumen.launcher"
        versionCode = 11
        versionName = "0.8.1-unblock"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        debug {
            // §5 requires zero main-thread icon decodes; the assertion is compiled into debug only.
            buildConfigField("boolean", "STRICT_MAIN_THREAD", "true")
        }
        release {
            // Back on after the 0.1.0 incident (AGP 8.9.2's R8 vs Kotlin 2.2 metadata — dead
            // screen). The gate STATUS.md set was "watch the minified output work on a screen":
            // met on an Android 15 emulator with AGP 8.13 — boot to home, Capsule pill, drawer,
            // app launch, capsule push all verified on this exact configuration (D29).
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Sideload distribution (DECISIONS D2): debug-signed until a release keystore exists.
            signingConfig = signingConfigs.getByName("debug")
            buildConfigField("boolean", "STRICT_MAIN_THREAD", "false")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}")
    }
}

dependencies {
    implementation(project(":core:design"))
    implementation(project(":core:data"))
    implementation(project(":feature:capsule"))
    implementation(project(":feature:home"))
    implementation(project(":feature:drawer"))
    implementation(project(":feature:widgets"))
    implementation(project(":feature:settings"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.profileinstaller)
}
