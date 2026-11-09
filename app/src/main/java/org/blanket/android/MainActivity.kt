package org.blanket.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.blanket.android.data.CustomSoundImporter
import org.blanket.android.data.ThemeMode
import org.blanket.android.playback.BlanketPlaybackService
import org.blanket.android.ui.BlanketApp
import org.blanket.android.ui.BlanketColors
import org.blanket.android.ui.Label

@UnstableApi
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = (application as BlanketApplication).repository
        setContent {
            val state by repository.state.collectAsStateWithLifecycle()
            val error by repository.error.collectAsStateWithLifecycle()
            val importScope = rememberCoroutineScope()
            val picker =
                rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                    if (uri != null) {
                        importScope.launch {
                            try {
                                val sound =
                                    withContext(Dispatchers.IO) {
                                        CustomSoundImporter(contentResolver).read(uri)
                                    }
                                repository.update {
                                    it.addCustomSound(sound.name, sound.uri, sound.id)
                                }
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (error: Exception) {
                                repository.reportError(
                                    "Could not add sound: ${error.message ?: "the file is unavailable"}"
                                )
                            }
                        }
                    }
                }
            val current = state
            val systemDark = isSystemInDarkTheme()
            val dark =
                when (current?.theme) {
                    ThemeMode.DARK -> true
                    ThemeMode.LIGHT -> false
                    else -> systemDark
                }
            LaunchedEffect(dark) {
                val bars =
                    if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    else
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT,
                        )
                enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
            }
            LaunchedEffect(current?.playing, current?.hasEnabledSounds) {
                if (current != null) {
                    val intent = Intent(this@MainActivity, BlanketPlaybackService::class.java)
                    if (current.playing && current.hasEnabledSounds) startForegroundService(intent)
                    else startService(intent)
                }
            }
            Box(
                Modifier.fillMaxSize()
                    .background(BlanketColors(dark).background)
                    .safeDrawingPadding()
            ) {
                if (current == null) Label(error ?: "Loading Blanket…")
                else
                    BlanketApp(
                        current,
                        repository::update,
                        { picker.launch(arrayOf("audio/*")) },
                        error,
                        { repository.reportError(null) },
                        onRemoveSound = { sound ->
                            repository.update { it.removeCustomSound(sound.id) }
                        },
                    )
            }
        }
    }
}
