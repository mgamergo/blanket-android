package org.blanket.android

import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import android.media.AudioManager
import android.os.Looper
import android.os.PowerManager
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import kotlinx.coroutines.cancel
import org.blanket.android.data.*
import org.blanket.android.playback.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config

@UnstableApi
class RecordingMixer : MixerBackend {
    var state: BlanketState? = null
    var released = false

    override fun synchronize(state: BlanketState) {
        this.state = state
    }

    override fun release() {
        released = true
    }
}

@UnstableApi
class TestPlaybackService : BlanketPlaybackService() {
    val recording = RecordingMixer()

    override fun createBackend(): MixerBackend = recording
}

@UnstableApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PlaybackServiceTest {
    private lateinit var app: BlanketApplication
    private lateinit var lifecycle: ServiceController<TestPlaybackService>
    private lateinit var service: TestPlaybackService
    private var controller: MediaController? = null
    private var destroyed = false

    @Before
    fun setUp() {
        app = RuntimeEnvironment.getApplication() as BlanketApplication
        shadowOf(app.getSystemService(AudioManager::class.java))
            .setNextFocusRequestResponse(AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
        waitUntil { app.repository.state.value != null }
        app.repository.update {
            it.copy(playing = false)
                .toggleSound("rain")
                .createPreset("Sleep", "sleep")
                .selectPreset(DEFAULT_PRESET_ID)
        }
        waitUntil { app.repository.state.value?.presets?.size == 2 }
        lifecycle = Robolectric.buildService(TestPlaybackService::class.java).create()
        service = lifecycle.get()
        waitUntil { service.recording.state != null }
    }

    @After
    fun tearDown() {
        controller?.release()
        if (::lifecycle.isInitialized && !destroyed) lifecycle.destroy()
        app.applicationScope.cancel()
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun waitUntil(predicate: () -> Boolean) {
        val deadline = System.nanoTime() + 5_000_000_000
        while (!predicate() && System.nanoTime() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        assertTrue("Condition did not become true", predicate())
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun connect(): MediaController {
        val future = MediaController.Builder(app, service.session!!.token).buildAsync()
        waitUntil { future.isDone }
        return future.get().also { controller = it }
    }

    @Test
    fun mediaSessionPlayPauseNextPreviousAndMetadata() {
        val controls = connect()
        controls.play()
        waitUntil { service.recording.state?.playing == true }
        controls.seekToNextMediaItem()
        waitUntil { service.recording.state?.activePresetId == "sleep" }
        waitUntil { controls.mediaMetadata.title.toString() == "Sleep" }
        assertFalse(controls.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM))
        controls.seekToPreviousMediaItem()
        waitUntil { service.recording.state?.activePresetId == DEFAULT_PRESET_ID }
        controls.pause()
        waitUntil { service.recording.state?.playing == false }
        assertTrue(service.recording.state!!.level("rain").enabled)
        assertEquals(0f, service.recording.state!!.gain("rain"))
    }

    @Test
    fun notificationHasMediaSessionAndWorkingPauseAction() {
        val controls = connect()
        controls.play()
        waitUntil {
            service.recording.state?.playing == true &&
                shadowOf(app.getSystemService(NotificationManager::class.java))
                    .allNotifications
                    .lastOrNull()
                    ?.actions
                    ?.any { it.title.toString().contains("Pause", ignoreCase = true) } == true
        }
        val notification =
            shadowOf(app.getSystemService(NotificationManager::class.java)).allNotifications.last()
        assertEquals("blanket-playback", notification.channelId)
        assertNotNull(
            notification.extras.getParcelable(
                Notification.EXTRA_MEDIA_SESSION,
                android.media.session.MediaSession.Token::class.java,
            )
        )
        assertTrue(
            notification.actions.any { it.title.toString().contains("Pause", ignoreCase = true) }
        )
        repeat(20) { shadowOf(app).nextStartedService }
        notification.actions
            .first { it.title.toString().contains("Pause", ignoreCase = true) }
            .actionIntent
            .send()
        val delivered = shadowOf(app).nextStartedService
        assertNotNull("Notification must dispatch a service intent", delivered)
        assertEquals(Intent.ACTION_MEDIA_BUTTON, delivered.action)
        service.onStartCommand(delivered, 0, 1)
        waitUntil { service.recording.state?.playing == false }
    }

    @Test
    fun notificationNextAndPreviousSwitchPresets() {
        connect().play()
        waitUntil { service.recording.state?.playing == true }
        fun sendAction(label: String) {
            val manager = shadowOf(app.getSystemService(NotificationManager::class.java))
            waitUntil {
                manager.allNotifications.lastOrNull()?.actions?.any {
                    it.title.toString().contains(label, true)
                } == true
            }
            repeat(20) { shadowOf(app).nextStartedService }
            manager.allNotifications
                .last()
                .actions
                .first { it.title.toString().contains(label, true) }
                .actionIntent
                .send()
            val intent = shadowOf(app).nextStartedService
            assertEquals(Intent.ACTION_MEDIA_BUTTON, intent.action)
            service.onStartCommand(intent, 0, 1)
        }
        sendAction("Next")
        waitUntil { service.recording.state?.activePresetId == "sleep" }
        sendAction("Previous")
        waitUntil { service.recording.state?.activePresetId == DEFAULT_PRESET_ID }
    }

    @Test
    fun deniedFocusPausesAndRemovesForegroundNotification() {
        shadowOf(app.getSystemService(AudioManager::class.java))
            .setNextFocusRequestResponse(AudioManager.AUDIOFOCUS_REQUEST_FAILED)
        connect().play()
        waitUntil { app.repository.error.value?.contains("Audio is in use") == true }
        waitUntil { service.recording.state?.playing == false }
        assertTrue(shadowOf(service).isForegroundStopped)
        assertEquals(0f, service.recording.state!!.gain("rain"))
    }

    @Test
    fun unmuteRetainsWorkingNotificationControls() {
        val controls = connect()
        val manager = shadowOf(app.getSystemService(NotificationManager::class.java))
        controls.play()
        waitUntil {
            manager.allNotifications.lastOrNull()?.actions?.any {
                it.title.toString().contains("Pause", true)
            } == true
        }
        val originalActions = manager.allNotifications.last().actions.map { it.title.toString() }
        app.repository.update { it.setMainVolume(0f) }
        waitUntil { service.recording.state?.mainVolume == 0f }
        app.repository.update { it.setMainVolume(0.7f) }
        waitUntil { service.recording.state?.mainVolume == 0.7f }
        val notification = manager.allNotifications.last()
        assertEquals(
            originalActions,
            notification.actions?.map { it.title.toString() } ?: emptyList<String>(),
        )
        repeat(20) { shadowOf(app).nextStartedService }
        notification.actions
            .first { it.title.toString().contains("Pause", true) }
            .actionIntent
            .send()
        val intent = shadowOf(app).nextStartedService
        assertEquals(Intent.ACTION_MEDIA_BUTTON, intent.action)
        service.onStartCommand(intent, 0, 1)
        waitUntil { service.recording.state?.playing == false }
    }

    @Test
    fun batterySaverBroadcastPausesWithoutAutoResume() {
        connect().play()
        waitUntil { service.recording.state?.playing == true }
        val power = shadowOf(app.getSystemService(PowerManager::class.java))
        power.setIsPowerSaveMode(true)
        app.sendBroadcast(Intent(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED))
        waitUntil { service.recording.state?.playing == false }
        assertTrue(app.repository.error.value!!.contains("save power"))
        power.setIsPowerSaveMode(false)
        app.sendBroadcast(Intent(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED))
        shadowOf(Looper.getMainLooper()).idle()
        assertFalse(service.recording.state!!.playing)
    }

    @Test
    fun headphoneDisconnectPausesAndDestroyReleasesMixer() {
        connect().play()
        waitUntil { service.recording.state?.playing == true }
        app.sendBroadcast(Intent(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
        waitUntil { service.recording.state?.playing == false }
        lifecycle.destroy()
        assertTrue(service.recording.released)
        destroyed = true
    }

    @Test
    fun optionalWakeLockOnlyWhilePlaying() {
        app.repository.update { it.copy(playing = true, keepAwake = true) }
        waitUntil { service.recording.state?.keepAwake == true }
        val power = shadowOf(app.getSystemService(PowerManager::class.java))
        val wake = org.robolectric.shadows.ShadowPowerManager.getLatestWakeLock()
        assertNotNull(wake)
        assertTrue(wake.isHeld)
        app.repository.update { it.copy(playing = false) }
        waitUntil { service.recording.state?.playing == false }
        assertFalse(wake.isHeld)
    }
}
