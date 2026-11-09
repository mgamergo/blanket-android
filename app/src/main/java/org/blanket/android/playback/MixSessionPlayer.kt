package org.blanket.android.playback

import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import org.blanket.android.data.BlanketRepository
import org.blanket.android.data.BlanketState

@UnstableApi
class MixSessionPlayer(private val repository: BlanketRepository) :
    SimpleBasePlayer(Looper.getMainLooper()) {
    private var mix = BlanketState(playing = false)

    fun synchronize(state: BlanketState) {
        mix = state
        invalidateState()
    }

    override fun getState(): State {
        val commands =
            Player.Commands.Builder()
                .addAll(
                    Player.COMMAND_PLAY_PAUSE,
                    Player.COMMAND_PREPARE,
                    Player.COMMAND_STOP,
                    Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
                    Player.COMMAND_GET_TIMELINE,
                    Player.COMMAND_GET_METADATA,
                    Player.COMMAND_GET_VOLUME,
                    Player.COMMAND_SET_VOLUME,
                )
        if (mix.canNext)
            commands.addAll(Player.COMMAND_SEEK_TO_NEXT, Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
        if (mix.canPrevious)
            commands.addAll(
                Player.COMMAND_SEEK_TO_PREVIOUS,
                Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
            )
        return State.Builder()
            .setAvailableCommands(commands.build())
            .setPlaylist(
                mix.presets.map { preset ->
                    val metadata =
                        MediaMetadata.Builder()
                            .setTitle(preset.name)
                            .setArtist("Blanket")
                            .setAlbumTitle("Listen to different sounds")
                            .build()
                    MediaItemData.Builder(preset.id)
                        .setMediaItem(
                            MediaItem.Builder()
                                .setMediaId(preset.id)
                                .setMediaMetadata(metadata)
                                .build()
                        )
                        .setMediaMetadata(metadata)
                        .build()
                }
            )
            .setCurrentMediaItemIndex(mix.presetIndex)
            .setPlayWhenReady(
                mix.playing && mix.hasEnabledSounds,
                Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST,
            )
            .setPlaybackState(Player.STATE_READY)
            .setVolume(mix.mainVolume)
            .build()
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        repository.update { it.copy(playing = playWhenReady) }
        return Futures.immediateVoidFuture()
    }

    override fun handlePrepare(): ListenableFuture<*> = Futures.immediateVoidFuture()

    override fun handleStop(): ListenableFuture<*> {
        repository.update { it.copy(playing = false) }
        return Futures.immediateVoidFuture()
    }

    override fun handleSetVolume(volume: Float): ListenableFuture<*> {
        repository.update { it.setMainVolume(volume) }
        return Futures.immediateVoidFuture()
    }

    override fun handleSeek(
        mediaItemIndex: Int,
        positionMs: Long,
        seekCommand: Int,
    ): ListenableFuture<*> {
        repository.update { state ->
            when (seekCommand) {
                Player.COMMAND_SEEK_TO_NEXT,
                Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> state.nextPreset()
                Player.COMMAND_SEEK_TO_PREVIOUS,
                Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> state.previousPreset()
                else -> state
            }
        }
        return Futures.immediateVoidFuture()
    }
}
