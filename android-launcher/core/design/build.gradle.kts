plugins {
    id("lumen.android.library")
    id("lumen.android.compose")
}

android {
    namespace = "dev.lumen.launcher.core.design"
}

dependencies {
    // §3 tokens, shapes, motion, haptics and the frosted material. No data dependencies: this
    // module must stay renderable in isolation so previews and screenshot tests never need a graph.
    implementation(libs.compose.material.icons.extended)
}
