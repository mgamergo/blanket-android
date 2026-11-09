# Parity against Blanket 0.8.0

| Requested behavior | Implementation | Automated evidence and limits |
| --- | --- | --- |
| All 14 original sounds and icons | Done | Exact upstream Ogg and SVG files; hash manifest and packaged audio verifier. ExoPlayers use repeat-one. Audible seam timing needs a phone. |
| Multiple sounds, per-sound and main volume | Done | Domain multiplier, ExoPlayer gains, Compose semantics and actual touch-drag tests. |
| Global play/pause | Done | Mix retained across pause; UI, media controller and notification PendingIntent tests. |
| Create, rename, delete and switch presets | Done | Copy-on-create, autosave, protected Default, isolated edits, bounded next/previous. Domain and UI tests. |
| Custom audio add, rename, remove | Done | OpenDocument audio picker, persistable read permission, provider metadata/file checks, stable IDs and removal from every preset. UI and provider adapter tests. Real provider permission behavior needs a phone. |
| Foreground playback and media controls | Done in code; hardware verification partial | MediaSessionService and ExoPlayer backend. Controller and notification play/pause/next/previous tests. Foreground promotion before Android 15+ focus request; denied-focus cleanup. Real lockscreen, headset, interruptions and OEM lifecycle need a phone. |
| System/light/dark themes | Done | Persisted preference, Compose interaction tests and native light/dark renders. Status/navigation bars follow the chosen theme. Real system-bar rendering needs a phone. |
| Always start paused and optional keep-awake | Done | Restoration and settings tests; wake lock held only while enabled and audible, released on pause/destruction. Both options default off. |
| Battery saver pause | Done | Saver broadcast pauses without automatic resume; explicit Play can resume. |
| State survives restart | Done | DataStore serializes all mixes, names, custom URIs and preferences; close/reopen test, corrupt-file fallback. Reboot plus URI grant needs a phone. |
| GPL and credits | Done | Full GPL in repo and About, upstream sound attribution/licensing, original authors, icon and font credits. |
| libadwaita appearance | Partial, close native adaptation | Original symbolic artwork, Cantarell, 88dp circular active areas, blue accent, light/dark palettes and bottom controls. Touch targets and Android chrome differ. Presets/preferences use full pages rather than GTK popovers/dialogs. Not a pixel-equivalence claim. |

No requested feature is intentionally omitted. No emulator or device was available. Robolectric tests use API 35, not a real API 37 runtime; minSdk 26 is declared and compile/lint checked.
