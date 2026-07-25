plugins {
    id("lumen.android.library")
    id("lumen.android.compose")
    id("lumen.hilt")
}

android {
    namespace = "dev.lumen.launcher.feature.settings"
}

dependencies {
    implementation(project(":core:design"))
    implementation(project(":core:data"))
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.compose.material.icons.extended)
}
