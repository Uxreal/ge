plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.kotlin.android)
}

/**
 * Macrobenchmark module. §11's budgets are only allowed into `STATUS.md` with output from these
 * tests behind them, which is why they are committed alongside the budgets rather than after.
 *
 * These must run on a physical device against a non-debuggable build:
 *   ./gradlew :benchmark:connectedBenchmarkAndroidTest
 */
android {
    namespace = "dev.lumen.launcher.benchmark"
    compileSdk = 36

    defaultConfig {
        minSdk = 30
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildTypes {
        create("benchmark") {
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }

    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

dependencies {
    implementation(libs.androidx.test.runner)
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.uiautomator)
    implementation(libs.benchmark.macro.junit4)
}
