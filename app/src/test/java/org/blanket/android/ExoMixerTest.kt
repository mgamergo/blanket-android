package org.blanket.android

import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import org.blanket.android.data.BlanketState
import org.blanket.android.playback.ExoMixerBackend
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@UnstableApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExoMixerTest {
    @Test
    fun exoPlayersLoopMixPauseAndRelease() {
        val backend = ExoMixerBackend(RuntimeEnvironment.getApplication()) {}
        try {
            val mix =
                BlanketState()
                    .setSoundVolume("rain", 0.8f)
                    .setSoundVolume("birds", 0.4f)
                    .setMainVolume(0.5f)
            backend.synchronize(mix)
            assertEquals(2, backend.players.size)
            assertEquals(0.4f, backend.players.getValue("rain").volume)
            assertEquals(0.2f, backend.players.getValue("birds").volume)
            assertTrue(
                backend.players.values.all {
                    it.repeatMode == Player.REPEAT_MODE_ONE && it.playWhenReady
                }
            )
            backend.synchronize(mix.copy(playing = false))
            assertTrue(backend.players.values.none { it.playWhenReady })
            backend.synchronize(mix.toggleSound("rain"))
            assertEquals(setOf("birds"), backend.players.keys)
        } finally {
            backend.release()
        }
        assertTrue(backend.players.isEmpty())
    }
}
