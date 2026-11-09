package org.blanket.android

import android.app.Activity
import android.content.Intent
import android.content.pm.ProviderInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Looper
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.cancel
import org.blanket.android.playback.BlanketPlaybackService
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowContentResolver

@UnstableApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MainActivityIntegrationTest {
    @get:Rule val compose = createEmptyComposeRule()
    private var activity: ActivityController<MainActivity>? = null
    private var service: ServiceController<BlanketPlaybackService>? = null
    private val app
        get() = RuntimeEnvironment.getApplication() as BlanketApplication

    private fun await(predicate: () -> Boolean) {
        compose.waitUntil(5000) {
            shadowOf(Looper.getMainLooper()).idle()
            predicate()
        }
    }

    @After
    fun close() {
        activity?.pause()?.stop()?.destroy()
        service?.destroy()
        app.applicationScope.cancel()
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun realActivityMixAndDocumentPickerAreWiredToPersistentState() {
        shadowOf(app.getSystemService(AudioManager::class.java))
            .setNextFocusRequestResponse(AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
        activity = Robolectric.buildActivity(MainActivity::class.java).setup()
        await { app.repository.state.value != null }
        compose.onNodeWithTag("sound-rain").performClick()
        await { app.repository.state.value?.level("rain")?.enabled == true }
        service = Robolectric.buildService(BlanketPlaybackService::class.java).create()
        await { service!!.get().session!!.player.playWhenReady }
        compose.onNodeWithContentDescription("Pause sounds").performClick()
        await { app.repository.state.value?.playing == false }
        await { !service!!.get().session!!.player.playWhenReady }

        val provider = AudioDocumentProvider()
        provider.attachInfo(app, ProviderInfo().apply { authority = "blanket.test.documents" })
        ShadowContentResolver.registerProviderInternal("blanket.test.documents", provider)
        compose.onNodeWithContentDescription("Sounds menu").performClick()
        compose.onNodeWithText("Add Sound…").performClick()
        val request = shadowOf(activity!!.get()).nextStartedActivityForResult
        assertNotNull(request)
        assertEquals(Intent.ACTION_OPEN_DOCUMENT, request.intent.action)
        assertTrue(
            request.intent.getStringArrayExtra(Intent.EXTRA_MIME_TYPES)!!.contains("audio/*")
        )
        val uri = Uri.parse("content://blanket.test.documents/audio")
        activity!!
            .get()
            .activityResultRegistry
            .dispatchResult(
                request.requestCode,
                Activity.RESULT_OK,
                Intent()
                    .setData(uri)
                    .addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                    ),
            )
        await { app.repository.state.value?.customSounds?.size == 1 }
        val sound = app.repository.state.value!!.customSounds.single()
        assertEquals("Ocean", sound.name)
        assertEquals(uri.toString(), sound.uri)
        assertNull(app.repository.error.value)
        compose.onNodeWithTag("sound-grid").performScrollToKey(sound.id)
        compose.onNodeWithTag("sound-${sound.id}").assertExists()
        compose.onNodeWithContentDescription("Manage Ocean").assertExists()
    }
}
