plugins {
    id("lumen.android.application")
    id("lumen.android.compose")
    id("lumen.hilt")
}

android {
    namespace = "dev.lumen.launcher"

    defaultConfig {
        applicationId = "dev.lumen.launcher"
        versionCode = 1
        versionName = "0.1.0-phase1"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        debug {
            // §5 requires zero main-thread icon decodes; the assertion is compiled into debug only.
            buildConfigField("boolean", "STRICT_MAIN_THREAD", "true")
        }
        release {
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
    implementation(project(":feature:home"))
    implementation(project(":feature:drawer"))
    implementation(project(":feature:widgets"))
    implementation(project(":feature:settings"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.profileinstaller)
}
