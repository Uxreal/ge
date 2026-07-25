import com.android.build.api.dsl.ApplicationExtension
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Compose for either an application or a library module. Applied on top of
 * `lumen.android.application` / `lumen.android.library`.
 */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        extensions.findByType(ApplicationExtension::class.java)?.apply {
            buildFeatures.compose = true
        }
        extensions.findByType(LibraryExtension::class.java)?.apply {
            buildFeatures.compose = true
        }

        dependencies {
            val bom = libs.findLibrary("compose-bom").get()
            add("implementation", platform(bom))
            add("androidTestImplementation", platform(bom))

            add("implementation", libs.findLibrary("compose-ui").get())
            add("implementation", libs.findLibrary("compose-ui-graphics").get())
            add("implementation", libs.findLibrary("compose-ui-util").get())
            add("implementation", libs.findLibrary("compose-foundation").get())
            add("implementation", libs.findLibrary("compose-material3").get())
            add("implementation", libs.findLibrary("compose-ui-tooling-preview").get())
            add("implementation", libs.findLibrary("androidx-lifecycle-runtime-compose").get())
            add("debugImplementation", libs.findLibrary("compose-ui-tooling").get())
        }
    }
}
