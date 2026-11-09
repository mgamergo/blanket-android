package org.blanket.android

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import com.github.takahirom.roborazzi.captureRoboImage
import java.io.File
import org.blanket.android.data.*
import org.blanket.android.ui.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BlanketUiTest {
    @get:Rule val compose = createEmptyComposeRule()
    private var activity: ActivityController<ComponentActivity>? = null
    private var state by mutableStateOf(BlanketState(playing = false))
    private var importCount = 0

    private fun render(screen: Screen = Screen.MAIN) {
        activity = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        activity!!.get().setContent {
            BlanketApp(
                state,
                { state = it(state) },
                {
                    importCount++
                    state = state.addCustomSound("My Audio", "content://test/audio", "custom")
                },
                initialScreen = screen,
            )
        }
    }

    @After
    fun closeActivity() {
        activity?.pause()?.stop()?.destroy()
    }

    @Test
    fun toggleAndSlidersChangeMix() {
        render()
        compose.onNodeWithTag("sound-rain").performClick()
        compose.onNodeWithTag("sound-rain").assertIsToggleable().assertIsOn()
        compose.onNodeWithTag("volume-rain").performSemanticsAction(SemanticsActions.SetProgress) {
            it(0.8f)
        }
        compose.onNodeWithContentDescription("Volume menu").performClick()
        compose.onNodeWithTag("main-volume").performSemanticsAction(SemanticsActions.SetProgress) {
            it(0.25f)
        }
        compose.runOnIdle { assertEquals(0.2f, state.copy(playing = true).gain("rain"), 0.0001f) }
    }

    @Test
    fun sliderDragSurvivesStateUpdates() {
        render()
        compose.onNodeWithTag("sound-rain").performClick()
        compose.onNodeWithTag("volume-rain").performTouchInput {
            swipe(start = centerLeft, end = centerRight, durationMillis = 500)
        }
        compose.runOnIdle {
            assertTrue(
                "Dragging to the right must raise the volume",
                state.level("rain").volume > 0.85f,
            )
        }
        compose.onNodeWithTag("volume-rain").performTouchInput {
            swipe(start = centerRight, end = centerLeft, durationMillis = 500)
        }
        compose.runOnIdle {
            assertTrue("Dragging left must lower the volume", state.level("rain").volume < 0.15f)
        }
    }

    @Test
    fun playPauseRetainsEnabledSound() {
        render()
        compose.onNodeWithTag("sound-rain").performClick()
        compose.onNodeWithContentDescription("Play sounds").performClick()
        compose.onNodeWithContentDescription("Pause sounds").performClick()
        compose.runOnIdle {
            assertFalse(state.playing)
            assertTrue(state.level("rain").enabled)
        }
    }

    @Test
    fun createSwitchRenameAndDeletePreset() {
        render(Screen.PRESETS)
        compose.onNodeWithContentDescription("Save as New Preset").performClick()
        compose.onNodeWithTag("name-input").performTextInput("Night")
        compose.onNodeWithContentDescription("Save", useUnmergedTree = true).performClick()
        compose.runOnIdle { assertEquals("Night", state.activePreset.name) }
        compose.onNodeWithContentDescription("Rename Night").performClick()
        compose.onNodeWithTag("name-input").performTextReplacement("Sleep")
        compose.onNodeWithContentDescription("Save", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Default", useUnmergedTree = true).performClick()
        compose.runOnIdle { assertEquals(DEFAULT_PRESET_ID, state.activePresetId) }
        compose.onNodeWithTag("preset-selector").performClick()
        compose.onNodeWithContentDescription("Delete Sleep").performClick()
        compose.onNodeWithContentDescription("Delete", useUnmergedTree = true).performClick()
        compose.runOnIdle { assertEquals(1, state.presets.size) }
    }

    @Test
    fun customImportRenameAndRemove() {
        render()
        compose.onNodeWithContentDescription("Sounds menu").performClick()
        compose.onNodeWithText("Add Sound…").performClick()
        compose.runOnIdle {
            assertEquals(1, importCount)
            assertEquals("My Audio", state.customSounds.single().name)
        }
        compose.onNodeWithTag("sound-grid").performScrollToKey("custom")
        compose.onNodeWithContentDescription("Manage My Audio").performClick()
        compose.onNodeWithText("Rename Sound").performClick()
        compose.onNodeWithTag("name-input").performTextReplacement("Local Rain")
        compose.onNodeWithContentDescription("Save", useUnmergedTree = true).performClick()
        compose.onNodeWithContentDescription("Manage Local Rain").performClick()
        compose.onNodeWithText("Remove Sound").performClick()
        compose.onNodeWithContentDescription("Delete", useUnmergedTree = true).performClick()
        compose.runOnIdle { assertTrue(state.customSounds.isEmpty()) }
    }

    @Test
    fun themeSwitchAndBehaviorOptions() {
        render(Screen.SETTINGS)
        compose.onNodeWithTag("theme-DARK").performClick()
        compose.onNodeWithTag("theme-DARK").assertIsSelected()
        compose.onNodeWithContentDescription("Always Start on Pause").performClick()
        compose.onNodeWithContentDescription("Keep Awake While Playing").performClick()
        compose.runOnIdle {
            assertEquals(ThemeMode.DARK, state.theme)
            assertTrue(state.alwaysStartPaused)
            assertTrue(state.keepAwake)
        }
        compose.onNodeWithTag("theme-LIGHT").performClick()
        compose.runOnIdle { assertEquals(ThemeMode.LIGHT, state.theme) }
    }

    private fun screenshot(name: String, screen: Screen = Screen.MAIN) {
        state =
            state
                .setSoundVolume("rain", 0.3f)
                .setSoundVolume("storm", 0.55f)
                .setSoundVolume("waves", 0.55f)
        render(screen)
        compose.waitForIdle()
        val folder = File("../docs/screenshots").apply { mkdirs() }
        compose.onRoot().captureRoboImage(File(folder, "$name.png").absolutePath)
    }

    @Test
    fun mainLightScreenshot() {
        state = state.copy(theme = ThemeMode.LIGHT)
        screenshot("main-light-phone")
    }

    @Test
    fun mainDarkScreenshot() {
        state = state.copy(theme = ThemeMode.DARK)
        screenshot("main-dark-phone")
    }

    @Test
    @Config(qualifiers = "w520dp-h600dp-mdpi")
    fun upstreamSizeLightScreenshot() {
        state = state.copy(theme = ThemeMode.LIGHT)
        screenshot("main-light-520")
    }

    @Test
    @Config(qualifiers = "w520dp-h600dp-mdpi")
    fun upstreamSizeDarkScreenshot() {
        state = state.copy(theme = ThemeMode.DARK)
        screenshot("main-dark-520")
    }

    @Test
    @Config(qualifiers = "w768dp-h1024dp-mdpi")
    fun tabletScreenshot() {
        screenshot("main-tablet")
    }

    @Test
    @Config(qualifiers = "w390dp-h844dp-mdpi")
    fun largeTextScreenshot() {
        org.robolectric.RuntimeEnvironment.setFontScale(1.5f)
        screenshot("main-large-text")
    }

    @Test
    fun presetsScreenshot() {
        state = state.createPreset("Deep Focus", "focus").createPreset("Sleep", "sleep")
        screenshot("presets", Screen.PRESETS)
    }

    @Test
    fun settingsScreenshot() {
        screenshot("settings", Screen.SETTINGS)
    }
}
