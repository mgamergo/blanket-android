package org.blanket.android.data

import java.util.UUID

const val DEFAULT_PRESET_ID = "default"

data class Sound(val id: String, val name: String, val uri: String, val builtIn: Boolean = false)

object SoundCatalog {
    val builtIn: List<Sound> =
        listOf(
                "rain" to "Rain",
                "storm" to "Storm",
                "wind" to "Wind",
                "waves" to "Waves",
                "stream" to "Stream",
                "birds" to "Birds",
                "summer-night" to "Summer Night",
                "train" to "Train",
                "boat" to "Boat",
                "city" to "City",
                "coffee-shop" to "Coffee Shop",
                "fireplace" to "Fireplace",
                "pink-noise" to "Pink Noise",
                "white-noise" to "White Noise",
            )
            .map { (id, name) -> Sound(id, name, "asset:///sounds/$id.ogg", true) }
}

data class SoundLevel(val enabled: Boolean = false, val volume: Float = 0.5f)

data class Preset(
    val id: String,
    val name: String,
    val levels: Map<String, SoundLevel> = emptyMap(),
    val hideInactive: Boolean = false,
)

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

data class BlanketState(
    val presets: List<Preset> = listOf(Preset(DEFAULT_PRESET_ID, "Default")),
    val activePresetId: String = DEFAULT_PRESET_ID,
    val customSounds: List<Sound> = emptyList(),
    val mainVolume: Float = 1f,
    val playing: Boolean = true,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val alwaysStartPaused: Boolean = false,
    val keepAwake: Boolean = false,
) {
    val activePreset: Preset
        get() = presets.first { it.id == activePresetId }

    val sounds: List<Sound>
        get() = SoundCatalog.builtIn + customSounds

    val presetIndex: Int
        get() = presets.indexOfFirst { it.id == activePresetId }

    val canNext: Boolean
        get() = presetIndex < presets.lastIndex

    val canPrevious: Boolean
        get() = presetIndex > 0

    val hasEnabledSounds: Boolean
        get() = sounds.any { level(it.id).enabled && level(it.id).volume > 0f }

    fun level(id: String): SoundLevel = activePreset.levels[id] ?: SoundLevel()

    fun gain(id: String): Float =
        if (playing && level(id).enabled) level(id).volume * mainVolume else 0f

    private fun editPreset(edit: (Preset) -> Preset): BlanketState =
        copy(presets = presets.map { if (it.id == activePresetId) edit(it) else it })

    fun toggleSound(id: String): BlanketState {
        if (sounds.none { it.id == id }) return this
        val level = level(id)
        return editPreset {
            it.copy(
                levels =
                    it.levels +
                        (id to
                            level.copy(
                                enabled = !level.enabled,
                                volume = level.volume.takeIf { v -> v > 0f } ?: 0.5f,
                            ))
            )
        }
    }

    fun setSoundVolume(id: String, value: Float): BlanketState {
        if (sounds.none { it.id == id } || !value.isFinite()) return this
        val volume = value.coerceIn(0f, 1f)
        return editPreset { it.copy(levels = it.levels + (id to SoundLevel(volume > 0f, volume))) }
    }

    fun setMainVolume(value: Float): BlanketState =
        if (value.isFinite()) copy(mainVolume = value.coerceIn(0f, 1f)) else this

    fun selectPreset(id: String): BlanketState =
        if (presets.any { it.id == id }) copy(activePresetId = id) else this

    fun nextPreset(): BlanketState =
        if (canNext) selectPreset(presets[presetIndex + 1].id) else this

    fun previousPreset(): BlanketState =
        if (canPrevious) selectPreset(presets[presetIndex - 1].id) else this

    fun createPreset(name: String, id: String = UUID.randomUUID().toString()): BlanketState {
        val cleanName = name.trim().take(80)
        if (cleanName.isEmpty() || presets.any { it.id == id }) return this
        return copy(
            presets = presets + activePreset.copy(id = id, name = cleanName),
            activePresetId = id,
        )
    }

    fun renamePreset(id: String, name: String): BlanketState {
        val cleanName = name.trim().take(80)
        if (cleanName.isEmpty()) return this
        return copy(presets = presets.map { if (it.id == id) it.copy(name = cleanName) else it })
    }

    fun deletePreset(id: String): BlanketState {
        if (id == DEFAULT_PRESET_ID) return this
        return copy(
            presets = presets.filterNot { it.id == id },
            activePresetId = if (activePresetId == id) DEFAULT_PRESET_ID else activePresetId,
        )
    }

    fun addCustomSound(
        name: String,
        uri: String,
        id: String = UUID.randomUUID().toString(),
    ): BlanketState {
        if (uri.isBlank() || name.isBlank() || sounds.any { it.id == id }) return this
        return copy(customSounds = customSounds + Sound(id, name.trim().take(80), uri))
    }

    fun renameCustomSound(id: String, name: String): BlanketState =
        if (name.isBlank()) this
        else
            copy(
                customSounds =
                    customSounds.map {
                        if (it.id == id) it.copy(name = name.trim().take(80)) else it
                    }
            )

    fun removeCustomSound(id: String): BlanketState =
        copy(
            customSounds = customSounds.filterNot { it.id == id },
            presets = presets.map { it.copy(levels = it.levels - id) },
        )

    fun resetSounds(): BlanketState = editPreset { it.copy(levels = emptyMap()) }

    fun toggleHideInactive(): BlanketState = editPreset { it.copy(hideInactive = !it.hideInactive) }

    fun restored(): BlanketState = if (alwaysStartPaused) copy(playing = false) else this
}
