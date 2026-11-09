package org.blanket.android

import org.blanket.android.data.*
import org.junit.Assert.*
import org.junit.Test

class BlanketStateTest {
    @Test
    fun catalogContainsExactlyUpstreamSounds() {
        assertEquals(14, SoundCatalog.builtIn.size)
        assertEquals(14, SoundCatalog.builtIn.map { it.id }.toSet().size)
        assertEquals(
            listOf(
                "rain",
                "storm",
                "wind",
                "waves",
                "stream",
                "birds",
                "summer-night",
                "train",
                "boat",
                "city",
                "coffee-shop",
                "fireplace",
                "pink-noise",
                "white-noise",
            ),
            SoundCatalog.builtIn.map { it.id },
        )
    }

    @Test
    fun toggleRemembersVolumeAndGlobalPauseRetainsMix() {
        var state = BlanketState().toggleSound("rain").setSoundVolume("rain", 0.72f)
        state = state.toggleSound("rain")
        assertFalse(state.level("rain").enabled)
        assertEquals(0.72f, state.level("rain").volume)
        state = state.toggleSound("rain").copy(playing = false)
        assertTrue(state.level("rain").enabled)
        assertEquals(0f, state.gain("rain"))
        assertEquals(0.72f, state.copy(playing = true).gain("rain"))
    }

    @Test
    fun mixerMultipliesGainsAndClampsBoundaries() {
        val state =
            BlanketState().toggleSound("rain").setSoundVolume("rain", 0.8f).setMainVolume(0.25f)
        assertEquals(0.2f, state.gain("rain"), 0.0001f)
        assertEquals(0f, state.gain("birds"))
        assertEquals(1f, state.setSoundVolume("rain", 9f).level("rain").volume)
        assertEquals(0f, state.setMainVolume(-1f).mainVolume)
        assertEquals(state, state.setMainVolume(Float.NaN))
        assertEquals(state, state.setSoundVolume("rain", Float.POSITIVE_INFINITY))
    }

    @Test
    fun zeroVolumeDisablesAndNextToggleRestoresHalf() {
        val zero = BlanketState().toggleSound("rain").setSoundVolume("rain", 0f)
        assertFalse(zero.level("rain").enabled)
        assertEquals(0.5f, zero.toggleSound("rain").gain("rain"))
    }

    @Test
    fun presetsCopyMixAndEditsStayIsolated() {
        val original = BlanketState().setSoundVolume("rain", 0.3f)
        val second =
            original
                .createPreset("Sleep", "sleep")
                .setSoundVolume("rain", 0.8f)
                .toggleSound("birds")
        assertEquals(0.3f, second.selectPreset(DEFAULT_PRESET_ID).gain("rain"))
        assertEquals(0.8f, second.gain("rain"))
        assertFalse(second.selectPreset(DEFAULT_PRESET_ID).level("birds").enabled)
        assertEquals("Rest", second.renamePreset("sleep", " Rest ").activePreset.name)
    }

    @Test
    fun presetNavigationStopsAtEndsAndDefaultCannotBeDeleted() {
        val first = BlanketState()
        val third = first.createPreset("A", "a").createPreset("B", "b")
        assertEquals("b", third.nextPreset().activePresetId)
        assertEquals("a", third.previousPreset().activePresetId)
        assertEquals(
            DEFAULT_PRESET_ID,
            third.previousPreset().previousPreset().previousPreset().activePresetId,
        )
        assertEquals(third, third.deletePreset(DEFAULT_PRESET_ID))
        assertEquals(DEFAULT_PRESET_ID, third.deletePreset("b").activePresetId)
        assertEquals(2, third.deletePreset("b").presets.size)
    }

    @Test
    fun customRenameRetainsIdentityAndRemovalCleansEveryPreset() {
        val state =
            BlanketState()
                .addCustomSound("Ocean", "content://audio/one", "custom")
                .setSoundVolume("custom", 0.6f)
                .createPreset("Night", "night")
        val renamed = state.renameCustomSound("custom", "Sea")
        assertEquals("Sea", renamed.customSounds.single().name)
        assertEquals(0.6f, renamed.gain("custom"))
        val removed = renamed.removeCustomSound("custom")
        assertTrue(removed.customSounds.isEmpty())
        assertTrue(removed.presets.all { "custom" !in it.levels })
    }

    @Test
    fun startupOptionAndInvalidNames() {
        assertTrue(BlanketState().restored().playing)
        assertFalse(BlanketState(alwaysStartPaused = true).restored().playing)
        assertFalse(BlanketState().keepAwake)
        assertEquals(
            BlanketState(),
            BlanketState()
                .createPreset(" ")
                .renamePreset(DEFAULT_PRESET_ID, " ")
                .toggleSound("missing"),
        )
    }
}
