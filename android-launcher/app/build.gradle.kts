plugins {
    id("lumen.android.application")
    id("lumen.android.compose")
    id("lumen.hilt")
}

android {
    namespace = "dev.lumen.launcher"

    defaultConfig {
        applicationId = "dev.lumen.launcher"
        versionCode = 5
        versionName = "0.3.1-capsule"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        debug {
            // §5 requires zero main-thread icon decodes; the assertion is compiled into debug only.
            buildConfigField("boolean", "STRICT_MAIN_THREAD", "true")
        }
        release {
            // Minification stays off until R8 output is verified on a device: the first field
            // install of the minified 0.1.0 produced a dead screen, and an unshrunk launcher
            // that works beats a small one that does not (STATUS.md tracks re-enabling).
            isMinifyEnabled = false
            isShrinkResources = false
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
