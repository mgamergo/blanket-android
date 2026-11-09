package org.blanket.android.playback

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import org.blanket.android.data.BlanketState

interface MixerBackend {
    fun synchronize(state: BlanketState)

    fun release()
}

@UnstableApi
class ExoMixerBackend(private val context: Context, private val onError: (String) -> Unit) :
    MixerBackend {
    internal val players = mutableMapOf<String, ExoPlayer>()

    override fun synchronize(state: BlanketState) {
        val enabled =
            state.sounds.filter { state.level(it.id).enabled && state.level(it.id).volume > 0f }
        val enabledIds = enabled.map { it.id }.toSet()
        players.keys
            .toList()
            .filterNot { it in enabledIds }
            .forEach { players.remove(it)?.release() }
        enabled.forEach { sound ->
            val player =
                players.getOrPut(sound.id) {
                    ExoPlayer.Builder(context)
                        .setLoadControl(
                            DefaultLoadControl.Builder()
                                .setBufferDurationsMs(1000, 5000, 500, 500)
                                .build()
                        )
                        .build()
                        .apply {
                            setAudioAttributes(
                                AudioAttributes.Builder()
                                    .setUsage(C.USAGE_MEDIA)
                                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                                    .build(),
                                false,
                            )
                            repeatMode = Player.REPEAT_MODE_ONE
                            addListener(
                                object : Player.Listener {
                                    override fun onPlayerError(error: PlaybackException) {
                                        onError(
                                            "Could not play ${sound.name}. Check that the audio file is still available."
                                        )
                                    }
                                }
                            )
                            setMediaItem(MediaItem.fromUri(sound.uri))
                            prepare()
                        }
                }
            player.volume = state.gain(sound.id)
            player.playWhenReady = state.playing && state.mainVolume > 0f
        }
    }

    override fun release() {
        players.values.forEach { it.release() }
        players.clear()
    }
}
