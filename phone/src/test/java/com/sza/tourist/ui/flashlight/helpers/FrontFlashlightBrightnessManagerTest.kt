package com.sza.tourist.ui.flashlight.helpers

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The brightness step manager had no test in the source. The port is pinned here: five steps, a full
 * start, clamped moves, and the window-brightness value each step carries.
 */
class FrontFlashlightBrightnessManagerTest {

    @Test
    fun `the lamp comes back at full brightness on every run`() {
        val manager = FrontFlashlightBrightnessManager()

        assertEquals(4, manager.currentLevel)
        assertEquals(1.0f, manager.currentBrightness(), 0f)
    }

    @Test
    fun `the strip offers five steps`() {
        assertEquals(5, FrontFlashlightBrightnessManager().levelCount)
    }

    @Test
    fun `a tap picks the step and its brightness follows`() {
        val manager = FrontFlashlightBrightnessManager()

        manager.setLevel(0)
        assertEquals(0.05f, manager.currentBrightness(), 0f)
        manager.setLevel(2)
        assertEquals(0.5f, manager.currentBrightness(), 0f)
    }

    @Test
    fun `a step index beyond the strip clamps to its ends`() {
        val manager = FrontFlashlightBrightnessManager()

        manager.setLevel(-3)
        assertEquals(0, manager.currentLevel)
        manager.setLevel(99)
        assertEquals(4, manager.currentLevel)
    }

    @Test
    fun `a swipe moves whole steps and stops at the ends`() {
        val manager = FrontFlashlightBrightnessManager()

        manager.shiftLevels(-2)
        assertEquals(2, manager.currentLevel)
        manager.shiftLevels(1)
        assertEquals(3, manager.currentLevel)
        manager.shiftLevels(-10)
        assertEquals(0, manager.currentLevel)
        manager.shiftLevels(10)
        assertEquals(4, manager.currentLevel)
    }

    @Test
    fun `the dimmest step is deliberately not black`() {
        val manager = FrontFlashlightBrightnessManager()

        manager.setLevel(0)

        assertEquals(0.05f, manager.currentBrightness(), 0f)
    }
}
