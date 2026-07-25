plugins {
    `kotlin-dsl`
}

group = "dev.lumen.buildlogic"

// No jvmToolchain here on purpose: the convention plugins are compiled by whichever JDK runs
// Gradle. Pinning 17 would demand a second JDK install for no benefit, since these classes only
// ever run inside the Gradle daemon. Android modules still target Java 17 (see LumenBuild).

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.kotlin.composePlugin)
    compileOnly(libs.ksp.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "lumen.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "lumen.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "lumen.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("hilt") {
            id = "lumen.hilt"
            implementationClass = "HiltConventionPlugin"
        }
    }
}
