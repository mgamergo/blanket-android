# Logbook

## 2026-11-09 · Completed
- Native port built. Formatting, lint, 40 debug + 40 release tests, screenshot verification and assembleDebug pass. No failing or skipped tests. A missing-snapshot negative control failed as expected and was restored.
- Sol-loop converged in two rounds with GPT-6.1 Sol at high effort. Fixed notification controls lost on unmute and DataStore writes on the UI thread. Both regressions fail before and pass after; round 2 approved with no new findings.
- Debug APK: app/build/outputs/apk/debug/app-debug.apk. Signature, 16 KiB alignment, and all 14 original packaged recordings verified. SHA-256 and detailed evidence are in docs/verification.md.
- Local commits: e8de0dd adopted AGENTS; 958804a imported original assets/licenses; f8d8d73 implemented the app and tests. Final documentation is committed separately. No remote or push. Task-owned Gradle processes are stopped; no preview server was started.
- Next for the user: sideload and run the README phone checklist. No emulator/device was available; acoustic seams, decoder capacity, OEM background limits, real lockscreen/focus and document grants remain phone checks.

## Decisions
- Project AGENTS.md comes from mgamergo/skills revision 4850ac433888dcd9b0e0526c6c0a7fd61f7e60f5. User exceptions govern native stack, own dirty tree, no fetch/rebase without a remote, autonomous grill answers and test-only verification.
- Upstream reference is tag 0.8.0, commit a961c1faeab69a61605b8733692b709ab5d2f09f. Scratch clones remain unmodified. Original Ogg/SVG files and licensing are retained; launcher/notification artwork is derived from the original app icon.
- Preserve the top preset selector, bottom playback bar, original icon drawings, Cantarell and libadwaita colors. Responsive touch targets and full preset/preferences pages are Android adaptations. See docs/parity.md and the screenshot comparison.
- DataStore is the single state writer. The service owns independent ExoPlayer loops and a Media3 session whose playlist represents presets. Default cannot be deleted; new presets clone the mix; next/previous stop at ends. Commands stay ordered on Main; storage runs on IO with the application Job.
- System theme is the default. Always-start-paused and keep-awake default off. Battery saver pauses without automatic resume; explicit Play can resume.
- Custom sounds use OpenDocument with a retained read grant and stable IDs. Provider operations run on IO. Removing a sound leaves its original file intact.
- Autonomous grill answers are in docs/acceptance.md, design tradeoffs in docs/architecture.md, review details in docs/sol-loop.md, and the decision trail in decisions.tsv.

## Toolchain and limits
- Installed Temurin 21 in ~/.local/blanket-jdk and Android command-line tools/platform 37.0/build-tools 36.0.0 in ~/Android/sdk, never system-wide. minSdk 26, target/compile 37. SDK PreviewSdkInt=0.
- Disk space was limited. Task Gradle and Robolectric caches use /var/tmp/blanket-gradle; the home Robolectric cache points there. No unrelated files were removed.
- AGP 8.13.2 emits an SDK 37 support advisory because its tested ceiling is 36.1. All checks pass. Dependency-update lint hints are excluded for pinned versions; correctness warnings remain fatal. Public media access and the optional playback-lifetime wake lock have narrow documented suppressions.
- Robolectric tests use API 35 and native graphics. Expected platform diagnostics and all remaining hardware limits are recorded in docs/verification.md.
