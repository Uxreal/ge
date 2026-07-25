import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

/**
 * Shared constants for every module. §3 of the spec makes design tokens the single source of truth;
 * the same discipline applies to the build, so no module states its own SDK or Java level.
 */
internal object LumenBuild {
    const val COMPILE_SDK = 36
    const val TARGET_SDK = 36

    /** §2 pins minSdk 30, which removes most of the dual-path branching a launcher accumulates. */
    const val MIN_SDK = 30
}

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")
