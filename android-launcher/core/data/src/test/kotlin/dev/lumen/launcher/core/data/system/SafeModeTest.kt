package dev.lumen.launcher.core.data.system

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** D44's protocol: two full-mode boots may die; the third boot must come up safe. */
class SafeModeTest {

    @Test
    fun `first and second boots run full`() {
        assertFalse(SafeMode.safeFor(1))
        assertFalse(SafeMode.safeFor(2))
    }

    @Test
    fun `the third boot and beyond are safe`() {
        assertTrue(SafeMode.safeFor(3))
        assertTrue(SafeMode.safeFor(7))
    }
}
