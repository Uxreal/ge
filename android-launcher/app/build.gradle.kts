plugins {
    id("lumen.android.application")
    id("lumen.android.compose")
    id("lumen.hilt")
}

android {
    namespace = "dev.lumen.launcher"

    defaultConfig {
        // "launcher2": a fresh install identity (D40). The old dev.lumen.launcher on the phone is
        // signed with a build key this machine no longer has, so updates to it can never install
        // again; a new id side-steps the corpse. Uninstall the old Lumen after switching.
        applicationId = "dev.lumen.launcher2"
        versionCode = 18
        versionName = "1.2.1-lifeline"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        // A keystore that lives in the repo (D40): this is a personal, never-published build, and
        // a committed key is what guarantees every future APK updates over the last one no matter
        // which machine built it. The debug-keystore era caused exactly that failure.
        create("lumen") {
            storeFile = rootProject.file("signing/lumen-release.keystore")
            storePassword = "lumen-personal"
            keyAlias = "lumen"
            keyPassword = "lumen-personal"
        }
    }

    buildTypes {
        debug {
            // §5 requires zero main-thread icon decodes; the assertion is compiled into debug only.
            buildConfigField("boolean", "STRICT_MAIN_THREAD", "true")
            signingConfig = signingConfigs.getByName("lumen")
        }
        release {
            // Back on after the 0.1.0 incident (AGP 8.9.2's R8 vs Kotlin 2.2 metadata — dead
            // screen). The gate STATUS.md set was "watch the minified output work on a screen":
            // met on an Android 15 emulator with AGP 8.13 — boot to home, Capsule pill, drawer,
            // app launch, capsule push all verified on this exact configuration (D29).
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Sideload distribution (DECISIONS D2), now on the committed permanent key (D40).
            signingConfig = signingConfigs.getByName("lumen")
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
