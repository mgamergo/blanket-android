package org.blanket.android.data

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

object BlanketSerializer : Serializer<BlanketState> {
    override val defaultValue = BlanketState()

    override suspend fun readFrom(input: InputStream): BlanketState =
        try {
            decode(input.readBytes().toString(Charsets.UTF_8))
        } catch (error: Exception) {
            if (
                error is JSONException ||
                    error is IllegalArgumentException ||
                    error is NoSuchElementException
            ) {
                throw CorruptionException("Blanket settings are invalid", error)
            }
            throw error
        }

    override suspend fun writeTo(t: BlanketState, output: OutputStream) {
        output.write(encode(t).toByteArray(Charsets.UTF_8))
    }

    fun encode(state: BlanketState): String =
        JSONObject()
            .apply {
                put("version", 1)
                put("active", state.activePresetId)
                put("mainVolume", state.mainVolume)
                put("playing", state.playing)
                put("theme", state.theme.name)
                put("alwaysStartPaused", state.alwaysStartPaused)
                put("keepAwake", state.keepAwake)
                put(
                    "customSounds",
                    JSONArray().apply {
                        state.customSounds.forEach { sound ->
                            put(
                                JSONObject()
                                    .put("id", sound.id)
                                    .put("name", sound.name)
                                    .put("uri", sound.uri)
                            )
                        }
                    },
                )
                put(
                    "presets",
                    JSONArray().apply {
                        state.presets.forEach { preset ->
                            put(
                                JSONObject().apply {
                                    put("id", preset.id)
                                    put("name", preset.name)
                                    put("hideInactive", preset.hideInactive)
                                    put(
                                        "levels",
                                        JSONObject().apply {
                                            preset.levels.forEach { (id, level) ->
                                                put(
                                                    id,
                                                    JSONObject()
                                                        .put("enabled", level.enabled)
                                                        .put("volume", level.volume),
                                                )
                                            }
                                        },
                                    )
                                }
                            )
                        }
                    },
                )
            }
            .toString()

    fun decode(text: String): BlanketState {
        val root = JSONObject(text)
        require(root.getInt("version") == 1)
        val customSounds =
            root.getJSONArray("customSounds").objects().map {
                Sound(it.getString("id"), it.getString("name"), it.getString("uri"))
            }
        require(
            customSounds.all {
                it.id.isNotBlank() && it.name.isNotBlank() && it.uri.startsWith("content://")
            }
        )
        require(customSounds.map { it.id }.distinct().size == customSounds.size)
        require(customSounds.none { custom -> SoundCatalog.builtIn.any { it.id == custom.id } })
        val knownSounds = (SoundCatalog.builtIn + customSounds).map { it.id }.toSet()
        val presets =
            root.getJSONArray("presets").objects().map { preset ->
                val levels = preset.getJSONObject("levels")
                Preset(
                    preset.getString("id"),
                    preset.getString("name"),
                    levels
                        .keys()
                        .asSequence()
                        .filter { it in knownSounds }
                        .associateWith {
                            val level = levels.getJSONObject(it)
                            SoundLevel(
                                level.getBoolean("enabled"),
                                level
                                    .getDouble("volume")
                                    .toFloat()
                                    .also { gain -> require(gain.isFinite()) }
                                    .coerceIn(0f, 1f),
                            )
                        },
                    preset.optBoolean("hideInactive"),
                )
            }
        require(presets.isNotEmpty() && presets.any { it.id == DEFAULT_PRESET_ID })
        require(
            presets.map { it.id }.distinct().size == presets.size &&
                presets.all { it.id.isNotBlank() && it.name.isNotBlank() }
        )
        val active =
            root.getString("active").takeIf { id -> presets.any { it.id == id } }
                ?: DEFAULT_PRESET_ID
        return BlanketState(
            presets,
            active,
            customSounds,
            root.getDouble("mainVolume").toFloat().also { require(it.isFinite()) }.coerceIn(0f, 1f),
            root.getBoolean("playing"),
            ThemeMode.valueOf(root.getString("theme")),
            root.getBoolean("alwaysStartPaused"),
            root.getBoolean("keepAwake"),
        )
    }

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map(::getJSONObject)
}

class BlanketRepository(
    private val store: DataStore<BlanketState>,
    private val scope: CoroutineScope,
) {
    private val mutableState = MutableStateFlow<BlanketState?>(null)
    val state: StateFlow<BlanketState?> = mutableState
    private val mutableError = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = mutableError

    init {
        scope.launch {
            try {
                store.updateData { it.restored() }
                store.data.collect { mutableState.value = it }
            } catch (error: java.io.IOException) {
                mutableError.value = "Could not load settings: ${error.message}"
            }
        }
    }

    fun update(edit: (BlanketState) -> BlanketState) {
        scope.launch {
            try {
                store.updateData(edit)
            } catch (error: java.io.IOException) {
                mutableError.value = "Could not save settings: ${error.message}"
            }
        }
    }

    fun reportError(message: String?) {
        mutableError.value = message
    }

    companion object {
        fun create(
            file: File,
            scope: CoroutineScope,
            serializer: Serializer<BlanketState> = BlanketSerializer,
        ): BlanketRepository =
            BlanketRepository(
                DataStoreFactory.create(
                    serializer,
                    ReplaceFileCorruptionHandler { BlanketState() },
                    scope = CoroutineScope(scope.coroutineContext + Dispatchers.IO),
                    produceFile = { file },
                ),
                scope,
            )
    }
}
