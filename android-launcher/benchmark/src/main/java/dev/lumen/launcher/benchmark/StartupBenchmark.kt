package dev.lumen.launcher.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * §11's budgets, as runnable measurements. These require a physical device and a non-debuggable
 * build; results — not assertions in prose — are what STATUS.md quotes:
 *
 *   ./gradlew :benchmark:connectedBenchmarkAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    /** Budget: cold start → first drawn frame < 350ms. */
    @Test
    fun coldStartup() = benchmarkRule.measureRepeated(
        packageName = PACKAGE,
        metrics = listOf(StartupTimingMetric()),
        iterations = 10,
        startupMode = StartupMode.COLD,
        compilationMode = CompilationMode.DEFAULT,
    ) {
        pressHome()
        startActivityAndWait()
    }

    /** Budget: frame time p99 during a grid fling < 8ms, zero dropped frames. */
    @Test
    fun gridFling() = benchmarkRule.measureRepeated(
        packageName = PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        iterations = 8,
        startupMode = StartupMode.WARM,
    ) {
        startActivityAndWait()
        val surface = device.findObject(By.pkg(PACKAGE).depth(0))
        repeat(4) {
            surface?.swipe(Direction.LEFT, 0.8f, 3000)
            device.waitForIdle()
            surface?.swipe(Direction.RIGHT, 0.8f, 3000)
            device.waitForIdle()
        }
    }

    private companion object {
        const val PACKAGE = "dev.lumen.launcher"
    }
}
