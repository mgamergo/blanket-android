# Acceptance criteria and autonomous grill

The user delegated answers while asleep. These answers follow the explicit spec and upstream 0.8.0 source.

| Question | Accepted behavior | Verification |
| --- | --- | --- |
| What does same sounds mean? | All 14 upstream Ogg files, byte-identical; no transcoding. | SHA-256 manifest and packaged asset checks. |
| What does toggle retain? | Disable without losing remembered volume; first enable uses 50%. | State and Compose toggle tests. |
| How do sliders combine? | Output gain is enabled × sound volume × main volume. Global pause retains mix. | Mixer state/engine tests and Compose semantics actions. |
| What does saving a preset capture? | Current enabled flags and volumes. Every edit autosaves to active preset. Main volume remains global. | Copy, switch, edit isolation, rename, delete, boundary tests. |
| Can Default be removed? | No. Last remaining preset is always Default. | Deletion guard test and disabled UI action. |
| Should next wrap? | No, matching MainPlayer.can_next/can_prev. | Media-session and state boundary tests. |
| How does custom import survive restart? | Retain document read grant; keep stable sound ID when renaming. No audio duplication or broad storage access. | Import result, persistence, rename/remove tests; provider grant on phone. |
| What happens if a custom file disappears? | Skip only its player and show a useful error; do not crash or erase other sounds. | Engine failure test and phone checklist. |
| Which startup state? | Restore play state unless always-start-paused is enabled. Initial mix empty. | Repository initialization tests. |
| Which power behavior? | Battery saver activation pauses and informs user; no automatic resume on exit. Keep-awake off by default. | Broadcast/service tests and settings tests. |
| Which UI geometry? | Upstream icon drawings and ordering, 12dp gaps, round 88dp icon areas, blue accent, bottom controls, light/dark palette. Responsive grid. | Native screenshot tests and inspection. |
| What is proven without hardware? | Pure domain behavior, DataStore round trip, service/session/notification wiring, Compose actions and native rendering. | Robolectric and Roborazzi. Audio continuity and OEM behavior cannot be established here. |

## Upstream evidence

- `blanket/define.py`: ordered sound catalog and authors.
- `data/resources/window.blp`, `sound-item.blp`, `style.css`: top selector, bottom actions, icon geometry, accent and slider styling.
- `blanket/sound.py`, `player.py`: remembered gain, default 0.5 on enable, main multiplier and pause.
- `blanket/main_player.py`, `settings.py`, `preset.py`: boundary navigation, cloning, per-preset state, protected Default.
- `blanket/mpris.py`: play/pause, preset navigation and preset-name metadata.
- `SOUNDS_LICENSING.md`: per-recording source, author, editor and license.
