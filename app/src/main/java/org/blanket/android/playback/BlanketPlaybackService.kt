package org.blanket.android.playback

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.PowerManager
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import org.blanket.android.BlanketApplication
import org.blanket.android.MainActivity
import org.blanket.android.R

@UnstableApi
open class BlanketPlaybackService : MediaSessionService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val repository
        get() = (application as BlanketApplication).repository

    private lateinit var backend: MixerBackend
    internal var session: MediaSession? = null
        private set

    private lateinit var sessionPlayer: MixSessionPlayer
    private lateinit var audioManager: AudioManager
    private var hasFocus = false
    private var wakeLock: PowerManager.WakeLock? = null
    private var registered = false
    private val focusRequest by lazy {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setOnAudioFocusChangeListener { change ->
                if (
                    change == AudioManager.AUDIOFOCUS_LOSS ||
                        change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ||
                        change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK
                )
                    pause()
            }
            .build()
    }
    private val powerReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (
                    intent.action == PowerManager.ACTION_POWER_SAVE_MODE_CHANGED &&
                        getSystemService(PowerManager::class.java).isPowerSaveMode
                ) {
                    pause()
                    repository.reportError("Paused to save power. Press Play to resume.")
                } else if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) pause()
            }
        }

    protected open fun createBackend(): MixerBackend =
        ExoMixerBackend(this, repository::reportError)

    @SuppressLint(
        "WakelockTimeout"
    ) // Optional lock lasts for playback and is released on pause or destruction.
    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(AudioManager::class.java)
        backend = createBackend()
        sessionPlayer = MixSessionPlayer(repository)
        val activity =
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        session = MediaSession.Builder(this, sessionPlayer).setSessionActivity(activity).build()
        addSession(session!!)
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setNotificationId(1)
                .setChannelId("blanket-playback")
                .build()
                .apply { setSmallIcon(R.drawable.ic_blanket) }
        )
        val filter =
            IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED).apply {
                addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            }
        if (Build.VERSION.SDK_INT >= 33)
            registerReceiver(powerReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        else registerReceiver(powerReceiver, filter)
        registered = true
        if (getSystemService(PowerManager::class.java).isPowerSaveMode) pause()
        scope.launch {
            repository.state.filterNotNull().collect { state ->
                val audible = state.playing && state.hasEnabledSounds && state.mainVolume > 0f
                if (audible && !hasFocus) {
                    promoteToForeground(state.activePreset.name)
                    hasFocus =
                        audioManager.requestAudioFocus(focusRequest) ==
                            AudioManager.AUDIOFOCUS_REQUEST_GRANTED
                    if (!hasFocus) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        pause()
                        repository.reportError("Audio is in use by another app.")
                        return@collect
                    }
                } else if (!audible && hasFocus) {
                    audioManager.abandonAudioFocusRequest(focusRequest)
                    hasFocus = false
                }
                backend.synchronize(state)
                sessionPlayer.synchronize(state)
                if (audible && state.keepAwake) {
                    if (wakeLock == null)
                        wakeLock =
                            getSystemService(PowerManager::class.java)
                                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Blanket:Playback")
                                .apply { setReferenceCounted(false) }
                    if (wakeLock?.isHeld == false) wakeLock?.acquire()
                } else releaseWakeLock()
            }
        }
    }

    private fun promoteToForeground(presetName: String) {
        // Android 15 requires foreground status before a background app requests audio focus.
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                "blanket-playback",
                "Blanket playback",
                NotificationManager.IMPORTANCE_LOW,
            )
        )
        val notification =
            manager.activeNotifications.firstOrNull { it.id == 1 }?.notification
                ?: Notification.Builder(this, "blanket-playback")
                    .setSmallIcon(R.drawable.ic_blanket)
                    .setContentTitle(presetName)
                    .setContentText("Blanket")
                    .setContentIntent(
                        PendingIntent.getActivity(
                            this,
                            0,
                            Intent(this, MainActivity::class.java),
                            PendingIntent.FLAG_IMMUTABLE,
                        )
                    )
                    .setStyle(Notification.MediaStyle().setMediaSession(session!!.platformToken))
                    .setCategory(Notification.CATEGORY_TRANSPORT)
                    .setOngoing(true)
                    .build()
        startForeground(1, notification)
    }

    private fun pause() {
        repository.update { it.copy(playing = false) }
    }

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) wakeLock?.release()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (
            repository.state.value?.playing != true ||
                repository.state.value?.hasEnabledSounds != true
        )
            stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        if (registered) unregisterReceiver(powerReceiver)
        releaseWakeLock()
        if (hasFocus) audioManager.abandonAudioFocusRequest(focusRequest)
        if (::backend.isInitialized) backend.release()
        session?.release()
        session = null
        if (::sessionPlayer.isInitialized) sessionPlayer.release()
        super.onDestroy()
    }
}
