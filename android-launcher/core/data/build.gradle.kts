plugins {
    id("lumen.android.library")
    id("lumen.hilt")
    alias(libs.plugins.protobuf)
}

android {
    namespace = "dev.lumen.launcher.core.data"
}

protobuf {
    protoc {
        artifact = libs.protobuf.protoc.get().toString()
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                register("java") {
                    option("lite")
                }
            }
        }
    }
}

ksp {
    // Committed so schema changes are reviewable and migrations are provable.
    arg("room.schemaLocation", "${projectDir}/schemas")
}

dependencies {
    // Deliberately does NOT depend on :core:design. §3 masks adaptive icon layers at draw time, so
    // the cache stores unmasked layers and the shape lives entirely in the UI layer — which is also
    // what makes a theme change instant with no cache invalidation.
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.datastore)
    implementation(libs.protobuf.javalite)

    implementation(libs.androidx.palette)
    implementation(libs.androidx.lifecycle.process)

    testImplementation(libs.room.testing)
}
