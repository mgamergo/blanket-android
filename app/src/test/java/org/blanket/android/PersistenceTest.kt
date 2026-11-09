package org.blanket.android

import androidx.datastore.core.CorruptionException
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import org.blanket.android.data.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PersistenceTest {
    @Test
    fun serializerRoundTripRetainsEveryField() = runBlocking {
        val state =
            BlanketState()
                .toggleSound("rain")
                .setMainVolume(0.35f)
                .createPreset("Focus", "focus")
                .addCustomSound("Audio", "content://documents/audio", "audio")
                .setSoundVolume("audio", 0.7f)
                .toggleHideInactive()
                .copy(
                    theme = ThemeMode.DARK,
                    alwaysStartPaused = true,
                    keepAwake = true,
                    playing = false,
                )
        val output = ByteArrayOutputStream()
        BlanketSerializer.writeTo(state, output)
        assertEquals(state, BlanketSerializer.readFrom(ByteArrayInputStream(output.toByteArray())))
    }

    @Test
    fun applicationPersistenceWritesOffTheMainThread() {
        val app = org.robolectric.RuntimeEnvironment.getApplication() as BlanketApplication
        val writeOnMain = java.util.concurrent.CopyOnWriteArrayList<Boolean>()
        val serializer =
            object : androidx.datastore.core.Serializer<BlanketState> {
                override val defaultValue = BlanketState()

                override suspend fun readFrom(input: java.io.InputStream) =
                    BlanketSerializer.readFrom(input)

                override suspend fun writeTo(t: BlanketState, output: java.io.OutputStream) {
                    writeOnMain +=
                        (android.os.Looper.myLooper() == android.os.Looper.getMainLooper())
                    BlanketSerializer.writeTo(t, output)
                }
            }
        val repository =
            BlanketRepository.create(
                File(app.cacheDir, "thread-settings.json"),
                app.applicationScope,
                serializer,
            )
        fun await(predicate: () -> Boolean) {
            val deadline = System.nanoTime() + 5_000_000_000
            while (!predicate() && System.nanoTime() < deadline) {
                org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                Thread.sleep(10)
            }
            assertTrue("Settings write did not finish", predicate())
        }
        try {
            await { repository.state.value != null }
            repository.update { it.setSoundVolume("rain", 0.6f) }
            await { repository.state.value?.level("rain")?.volume == 0.6f }
            assertTrue("Must observe a real serializer write", writeOnMain.isNotEmpty())
            assertFalse("Persistence must not write/fsync on the UI thread", writeOnMain.any { it })
        } finally {
            app.applicationScope.cancel()
        }
    }

    @Test
    fun corruptDataIsReportedAsCorruption() = runBlocking {
        try {
            BlanketSerializer.readFrom(ByteArrayInputStream("broken".toByteArray()))
            fail("Expected CorruptionException")
        } catch (_: CorruptionException) {}
    }

    @Test
    fun dataStoreSurvivesReopenAndAlwaysStartPaused() = runBlocking {
        val folder = kotlin.io.path.createTempDirectory("blanket-state").toFile()
        val file = File(folder, "settings.json")
        val firstJob = SupervisorJob()
        val secondJob = SupervisorJob()
        try {
            val repository =
                BlanketRepository.create(file, CoroutineScope(firstJob + Dispatchers.IO))
            withTimeout(5000) { repository.state.filterNotNull().first() }
            repository.update {
                it.toggleSound("rain")
                    .createPreset("Bedtime", "bedtime")
                    .copy(alwaysStartPaused = true, theme = ThemeMode.DARK)
            }
            val saved =
                withTimeout(5000) {
                    repository.state.filterNotNull().first { it.activePresetId == "bedtime" }
                }
            assertTrue(saved.playing)
            firstJob.cancelAndJoin()
            val reopened =
                BlanketRepository.create(file, CoroutineScope(secondJob + Dispatchers.IO))
            val restored = withTimeout(5000) { reopened.state.filterNotNull().first() }
            assertEquals(saved.copy(playing = false), restored)
            assertTrue(restored.level("rain").enabled)
        } finally {
            firstJob.cancelAndJoin()
            secondJob.cancelAndJoin()
            folder.deleteRecursively()
        }
    }

    @Test
    fun corruptFileRecoversToPermanentDefault() = runBlocking {
        val folder = kotlin.io.path.createTempDirectory("blanket-corrupt").toFile()
        val file = File(folder, "settings.json").apply { writeText("invalid") }
        val job = SupervisorJob()
        try {
            val repository = BlanketRepository.create(file, CoroutineScope(job + Dispatchers.IO))
            val restored = withTimeout(5000) { repository.state.filterNotNull().first() }
            assertEquals(DEFAULT_PRESET_ID, restored.activePreset.id)
        } finally {
            job.cancelAndJoin()
            folder.deleteRecursively()
        }
    }
}
