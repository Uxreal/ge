plugins {
    id("lumen.android.library")
    id("lumen.android.compose")
    id("lumen.hilt")
}

android {
    namespace = "dev.lumen.launcher.feature.drawer"
}

dependencies {
    implementation(project(":core:design"))
    implementation(project(":core:data"))
    implementation(libs.androidx.lifecycle.viewmodel.compose)
}
