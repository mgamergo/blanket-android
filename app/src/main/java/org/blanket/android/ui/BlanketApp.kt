package org.blanket.android.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.blanket.android.data.*

enum class Screen {
    MAIN,
    PRESETS,
    SETTINGS,
    ABOUT,
}

private data class NameDialog(
    val title: String,
    val initial: String = "",
    val save: (String) -> Unit,
)

@Composable
fun BlanketApp(
    state: BlanketState,
    onUpdate: ((BlanketState) -> BlanketState) -> Unit,
    onAddSound: () -> Unit,
    error: String? = null,
    onDismissError: () -> Unit = {},
    initialScreen: Screen = Screen.MAIN,
    onRemoveSound: (Sound) -> Unit = { sound -> onUpdate { it.removeCustomSound(sound.id) } },
) {
    var screen by remember { mutableStateOf(initialScreen) }
    var menu by remember { mutableStateOf<String?>(null) }
    var nameDialog by remember { mutableStateOf<NameDialog?>(null) }
    var customDialog by remember { mutableStateOf<Sound?>(null) }
    var deletePreset by remember { mutableStateOf<Preset?>(null) }
    var removeSound by remember { mutableStateOf<Sound?>(null) }
    BackHandler(screen != Screen.MAIN) { screen = Screen.MAIN }
    BlanketTheme(state.theme) {
        val colors = LocalBlanketColors.current
        Column(
            Modifier.fillMaxSize().background(colors.background).semantics {
                contentDescription = "${state.theme.name.lowercase()} theme"
            }
        ) {
            Row(
                Modifier.fillMaxWidth()
                    .background(if (colors.dark) colors.background else colors.background)
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (screen != Screen.MAIN) GlyphButton("‹", "Back", { screen = Screen.MAIN })
                else
                    Box(Modifier.weight(1f)) {
                        Row(
                            Modifier.clip(RoundedCornerShape(8.dp))
                                .clickable(role = Role.Button) { screen = Screen.PRESETS }
                                .padding(12.dp)
                                .testTag("preset-selector"),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Label(
                                state.activePreset.name,
                                bold = true,
                                maxLines = 1,
                                modifier = Modifier.weight(1f, false),
                            )
                            Label("⌄", Modifier.padding(start = 6.dp))
                        }
                    }
                Label(
                    when (screen) {
                        Screen.MAIN -> "Blanket"
                        Screen.PRESETS -> "Presets"
                        Screen.SETTINGS -> "Preferences"
                        Screen.ABOUT -> "About Blanket"
                    },
                    bold = true,
                    align = TextAlign.Center,
                    modifier =
                        if (screen == Screen.MAIN) Modifier.weight(1f) else Modifier.weight(1f),
                )
                if (screen == Screen.MAIN)
                    Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                        GlyphButton("☰", "Main menu", { menu = "main" })
                    }
                else Spacer(Modifier.width(48.dp))
            }
            if (error != null)
                Row(
                    Modifier.background(colors.accentBackground)
                        .padding(start = 16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Label(error, modifier = Modifier.weight(1f), size = 13)
                    GlyphButton("×", "Dismiss message", onDismissError)
                }
            when (screen) {
                Screen.MAIN -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(112.dp),
                        modifier = Modifier.weight(1f).testTag("sound-grid"),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(
                            state.sounds.filter {
                                !state.activePreset.hideInactive || state.level(it.id).enabled
                            },
                            key = { it.id },
                        ) { sound ->
                            SoundCard(
                                sound,
                                state.level(sound.id),
                                { onUpdate { it.toggleSound(sound.id) } },
                                { value -> onUpdate { it.setSoundVolume(sound.id, value) } },
                                { customDialog = sound },
                            )
                        }
                        item("add") {
                            Column(
                                Modifier.padding(12.dp)
                                    .height(160.dp)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(role = Role.Button, onClick = onAddSound)
                                    .semantics { contentDescription = "Add Sound" },
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Box(Modifier.size(88.dp), contentAlignment = Alignment.Center) {
                                    SoundIcon("add", "", colors.muted)
                                }
                                Label(
                                    "Add Sound…",
                                    Modifier.padding(top = 6.dp),
                                    align = TextAlign.Center,
                                )
                            }
                        }
                    }
                    Separator()
                    Row(
                        Modifier.fillMaxWidth().background(colors.bar).height(64.dp),
                        horizontalArrangement =
                            Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        GlyphButton("◖", "Volume menu", { menu = "volume" })
                        GlyphButton(
                            if (state.playing) "Ⅱ" else "▶",
                            if (state.playing) "Pause sounds" else "Play sounds",
                            { onUpdate { it.copy(playing = !it.playing) } },
                            blue = true,
                        )
                        GlyphButton("⋮", "Sounds menu", { menu = "sounds" })
                    }
                }
                Screen.PRESETS -> {
                    LazyColumn(
                        Modifier.weight(1f).padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.presets, key = { it.id }) { preset ->
                            Column(
                                Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.bar)
                            ) {
                                Row(
                                    Modifier.fillMaxWidth()
                                        .clickable {
                                            onUpdate { it.selectPreset(preset.id) }
                                            screen = Screen.MAIN
                                        }
                                        .padding(start = 16.dp)
                                        .heightIn(min = 56.dp)
                                        .semantics { selected = preset.id == state.activePresetId },
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Label(
                                        preset.name,
                                        Modifier.weight(1f),
                                        bold = preset.id == state.activePresetId,
                                    )
                                    if (preset.id == state.activePresetId)
                                        Label("✓", Modifier.padding(16.dp), color = colors.accent)
                                }
                                Row(
                                    Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.End,
                                ) {
                                    TextButton("Rename", "Rename ${preset.name}") {
                                        nameDialog =
                                            NameDialog("Rename Preset", preset.name) { name ->
                                                onUpdate { it.renamePreset(preset.id, name) }
                                            }
                                    }
                                    TextButton(
                                        "Delete",
                                        "Delete ${preset.name}",
                                        preset.id != DEFAULT_PRESET_ID,
                                    ) {
                                        deletePreset = preset
                                    }
                                }
                            }
                        }
                        item {
                            TextButton("Save as New Preset…", "Save as New Preset") {
                                nameDialog =
                                    NameDialog("New Preset") { name ->
                                        onUpdate { it.createPreset(name) }
                                    }
                            }
                        }
                    }
                }
                Screen.SETTINGS -> SettingsScreen(state, onUpdate)
                Screen.ABOUT -> AboutScreen()
            }
        }
        menu?.let { currentMenu ->
            Panel(onDismiss = { menu = null }) {
                when (currentMenu) {
                    "main" -> {
                        MenuItem("Preferences") {
                            menu = null
                            screen = Screen.SETTINGS
                        }
                        MenuItem("About Blanket") {
                            menu = null
                            screen = Screen.ABOUT
                        }
                    }
                    "volume" -> {
                        Label("Main volume", bold = true)
                        BlanketSlider(
                            state.mainVolume,
                            "Main volume",
                            { value -> onUpdate { it.setMainVolume(value) } },
                            Modifier.fillMaxWidth().height(48.dp).testTag("main-volume"),
                        )
                        MenuItem("Reset Sounds") {
                            menu = null
                            onUpdate { it.resetSounds() }
                        }
                    }
                    "sounds" -> {
                        MenuItem(
                            if (state.activePreset.hideInactive) "Show All Sounds"
                            else "Hide Inactive Sounds"
                        ) {
                            menu = null
                            onUpdate { it.toggleHideInactive() }
                        }
                        MenuItem("Save as New Preset…") {
                            menu = null
                            nameDialog =
                                NameDialog("New Preset") { name ->
                                    onUpdate { it.createPreset(name) }
                                }
                        }
                        MenuItem("Add Sound…") {
                            menu = null
                            onAddSound()
                        }
                    }
                }
            }
        }
        nameDialog?.let { dialog ->
            NameEntry(dialog.title, dialog.initial, { nameDialog = null }) {
                dialog.save(it)
                nameDialog = null
            }
        }
        customDialog?.let { sound ->
            Panel(onDismiss = { customDialog = null }) {
                Label(sound.name, bold = true)
                MenuItem("Rename Sound") {
                    customDialog = null
                    nameDialog =
                        NameDialog("Rename Sound", sound.name) { name ->
                            onUpdate { it.renameCustomSound(sound.id, name) }
                        }
                }
                MenuItem("Remove Sound") {
                    customDialog = null
                    removeSound = sound
                }
            }
        }
        deletePreset?.let { preset ->
            Confirmation(
                "Delete ${preset.name}?",
                "The saved mix will be removed.",
                { deletePreset = null },
            ) {
                onUpdate { it.deletePreset(preset.id) }
                deletePreset = null
            }
        }
        removeSound?.let { sound ->
            Confirmation(
                "Remove ${sound.name}?",
                "The original audio file will stay in storage.",
                { removeSound = null },
            ) {
                onRemoveSound(sound)
                removeSound = null
            }
        }
    }
}

@Composable
private fun SoundCard(
    sound: Sound,
    level: SoundLevel,
    onToggle: () -> Unit,
    onVolume: (Float) -> Unit,
    onManage: () -> Unit,
) {
    val colors = LocalBlanketColors.current
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Column(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(role = Role.Checkbox, onClick = onToggle)
                    .semantics {
                        contentDescription = sound.name
                        toggleableState =
                            if (level.enabled) androidx.compose.ui.state.ToggleableState.On
                            else androidx.compose.ui.state.ToggleableState.Off
                    }
                    .testTag("sound-${sound.id}"),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.size(88.dp)
                        .clip(CircleShape)
                        .background(
                            if (level.enabled) colors.accentBackground
                            else androidx.compose.ui.graphics.Color.Transparent
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    SoundIcon(
                        if (sound.builtIn) sound.id else "sound-wave",
                        "",
                        if (level.enabled) colors.accent else colors.foreground,
                    )
                }
                Box(
                    Modifier.heightIn(min = 46.dp).padding(top = 6.dp),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Label(sound.name, align = TextAlign.Center, maxLines = 2)
                }
            }
            if (!sound.builtIn)
                GlyphButton("⋮", "Manage ${sound.name}", onManage, Modifier.align(Alignment.TopEnd))
        }
        BlanketSlider(
            level.volume,
            "${sound.name} volume",
            onVolume,
            Modifier.fillMaxWidth().height(48.dp).testTag("volume-${sound.id}"),
            level.enabled,
        )
    }
}

@Composable
private fun SettingsScreen(
    state: BlanketState,
    onUpdate: ((BlanketState) -> BlanketState) -> Unit,
) {
    val colors = LocalBlanketColors.current
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Label("Appearance", bold = true, size = 14)
        Column(Modifier.clip(RoundedCornerShape(12.dp)).background(colors.bar)) {
            ThemeMode.entries.forEach { theme ->
                Row(
                    Modifier.fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .clickable(role = Role.RadioButton) { onUpdate { it.copy(theme = theme) } }
                        .semantics { selected = theme == state.theme }
                        .padding(16.dp)
                        .testTag("theme-${theme.name}"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Label(
                        when (theme) {
                            ThemeMode.SYSTEM -> "Follow System"
                            ThemeMode.LIGHT -> "Light"
                            ThemeMode.DARK -> "Dark"
                        },
                        Modifier.weight(1f),
                    )
                    if (state.theme == theme) Label("✓", color = colors.accent)
                }
            }
        }
        Label("Behavior", bold = true, size = 14, modifier = Modifier.padding(top = 16.dp))
        Column(Modifier.clip(RoundedCornerShape(12.dp)).background(colors.bar)) {
            ToggleRow(
                "Always Start on Pause",
                "Restore the mix without starting playback",
                state.alwaysStartPaused,
            ) {
                onUpdate { it.copy(alwaysStartPaused = !it.alwaysStartPaused) }
            }
            Separator()
            ToggleRow(
                "Keep Awake While Playing",
                "Prevent CPU suspension; uses more battery",
                state.keepAwake,
            ) {
                onUpdate { it.copy(keepAwake = !it.keepAwake) }
            }
        }
        Label(
            "Playback pauses when battery saver turns on. You can resume it with Play.",
            size = 13,
            color = colors.muted,
        )
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onClick: () -> Unit) {
    val colors = LocalBlanketColors.current
    Row(
        Modifier.fillMaxWidth()
            .clickable(role = Role.Switch, onClick = onClick)
            .semantics {
                contentDescription = title
                toggleableState =
                    if (checked) androidx.compose.ui.state.ToggleableState.On
                    else androidx.compose.ui.state.ToggleableState.Off
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Label(title)
            Label(subtitle, size = 12, color = colors.muted)
        }
        Box(
            Modifier.size(44.dp, 26.dp)
                .clip(CircleShape)
                .background(if (checked) colors.blue else colors.disabledFill)
                .padding(3.dp),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(
                Modifier.size(20.dp)
                    .clip(CircleShape)
                    .background(androidx.compose.ui.graphics.Color.White)
            )
        }
    }
}

@Composable
private fun AboutScreen() {
    val context = LocalContext.current
    val licensing = remember {
        context.assets.open("licenses/SOUNDS_LICENSING.md").bufferedReader().use { it.readText() }
    }
    val license = remember {
        context.assets.open("licenses/LICENSE").bufferedReader().use { it.readText() }
    }
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Label("Blanket", size = 28, bold = true)
        Label("Listen to different sounds")
        Label(
            "0.8.0 · Android port\nOriginal app © Rafael Mardojai CM\nSound icons by Jakub Steiner\nSound editing by Porrumentzio"
        )
        Label(
            "An unofficial native Android derivative of Blanket.\nhttps://github.com/rafaelmardojai/blanket\nGPL-3.0-or-later. No warranty."
        )
        Label("Sound credits and licenses", bold = true)
        Label(licensing, size = 13)
        Label(
            "Cantarell font by Dave Crossland and contributors, SIL Open Font License 1.1.",
            size = 13,
        )
        Label("GNU General Public License", bold = true)
        Label(license, size = 12)
    }
}

@Composable
private fun Separator() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(LocalBlanketColors.current.separator))
}

@Composable
private fun TextButton(
    text: String,
    description: String = text,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        Modifier.heightIn(min = 48.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Label(
            text,
            color = LocalBlanketColors.current.accent.copy(alpha = if (enabled) 1f else 0.35f),
            bold = true,
        )
    }
}

@Composable
private fun MenuItem(text: String, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(12.dp)
    ) {
        Label(text)
    }
}

@Composable
private fun Panel(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            Modifier.widthIn(max = 400.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(LocalBlanketColors.current.bar)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

@Composable
private fun NameEntry(
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by remember { mutableStateOf(initial) }
    Panel(onDismiss) {
        Label(title, bold = true, size = 18)
        BasicTextField(
            name,
            { name = it.take(80) },
            Modifier.fillMaxWidth()
                .padding(vertical = 12.dp)
                .border(1.dp, LocalBlanketColors.current.separator, RoundedCornerShape(8.dp))
                .padding(14.dp)
                .testTag("name-input")
                .semantics { contentDescription = "Name" },
            textStyle =
                TextStyle(
                    color = LocalBlanketColors.current.foreground,
                    fontFamily = BlanketFont,
                    fontSize = 16.sp,
                ),
            singleLine = true,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton("Cancel", onClick = onDismiss)
            TextButton("Save", enabled = name.isNotBlank()) { onSave(name) }
        }
    }
}

@Composable
private fun Confirmation(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Panel(onDismiss) {
        Label(title, bold = true, size = 18)
        Label(message)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton("Cancel", onClick = onDismiss)
            TextButton("Delete", onClick = onConfirm)
        }
    }
}
