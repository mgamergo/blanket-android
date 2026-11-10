# Verification

## Final verified result

- `./gradlew :app:spotlessApply lint test assembleDebug --no-daemon -Proborazzi.test.record=true` passed. It recorded native screenshot baselines and produced the initial APK. After the Sol fixes, `./gradlew :app:spotlessApply :app:spotlessCheck lint test assembleDebug --no-daemon` passed 40 debug and 40 release tests and rebuilt the APK.
- `./gradlew :app:spotlessCheck test --no-daemon` passed with screenshot verification enabled by default. The final suite has 80 tests, zero failures, errors or skips.
- Android lint reported `No issues found.` Correctness warnings are fatal. Only dependency-update hints are excluded because the dependency set is pinned. Public media-service access and the optional playback-lifetime wake lock have narrow documented lint suppressions.
- `scripts/verify-assets.sh app/build/outputs/apk/debug/app-debug.apk` passed. Every original recording packaged in the APK matches the upstream SHA-256 value. Original copied symbolic SVG hashes also match their manifest.
- Direct comparison against the untouched upstream checkout also confirms that the source files and packaged APK contain identical audio, symbolic icons, GPL text and sound licensing.
- APK manifest inspection confirms package `org.blanket.android`, version `0.8.0-android.1`, min SDK 26 and target SDK 37.
- A negative control temporarily removed one screenshot baseline. The default verification test failed specifically because the original image was missing. The baseline was restored and the complete checks passed again.
- Native screenshots were inspected at 390×844, 520×600, 768×1024 and 150% text size. The inline comparison was previewed at 728px light and 390px dark with no console errors.

Generated evidence is available in `app/build/reports/tests/testDebugUnitTest/index.html`, `app/build/reports/tests/testReleaseUnitTest/index.html` and `app/build/reports/lint-results-debug.html`. The committed screenshot baselines are in `docs/screenshots/`; the self-contained comparison is `docs/screenshot-comparison.html`. Rebuild the comparison after a deliberate snapshot update with `bun scripts/build-comparison.mjs`.

## Issues found and fixed during verification

- SVGs rendered too small because changing document dimensions did not scale drawings without viewBox. Scale the drawing canvas instead.
- Two-line titles needed explicit line height and flexible title height. Slider gesture callbacks needed stable pointer-input lifetime during state updates.
- Robolectric width-qualified dialogs containing text fields had a known sizing oscillation. Use explicit responsive dialog widths rather than the platform default-width heuristic.
- A debug-only test-host manifest prevented UI tests under release. Own and destroy the host activity explicitly in the Robolectric UI test.
- Notification PendingIntent tests must deliver the recorded service intent through `onStartCommand`; Robolectric does not run an actual service process for PendingIntent.send.
- Android 15+ background audio-focus requests require foreground status first. Promote before requesting focus and remove the bootstrap notification if focus is denied.
- Custom document queries and file opening now run on an IO dispatcher, and cancellation is preserved.
- Explicit backup policy resources are supplied for both legacy and Android 12+ backup formats. Persistable document grants cannot transfer to another installation.

The real-activity integration check exercises the actual MainActivity, persistent repository, playback service and an OpenDocument result backed by an audio provider. It asserts the imported sound reaches both persisted state and the rendered grid. The APK passes 16 KiB ZIP alignment; all eight bundled native libraries also have ELF LOAD alignment of at least 16 KiB.

The real-service test logs that Robolectric has no Android `media_metrics` service. Media3's optional metrics listener explicitly returns null when that service is absent, so playback proceeds. A native `Invalid ID 0x00000000` startup diagnostic is also retained in the report. These diagnostics were not filtered; the test asserts the app has no repository/playback error and verifies its controls and import result.

## Known verification limits

There was no emulator or device. Tests run under Robolectric API 35, with native graphics, while the app compiles and targets stable API 37. Acoustic loop boundaries, simultaneous decoder capacity, OEM power management, actual notification/lockscreen layout, audio focus enforcement and real document-provider grants need the phone checks below.

The pinned AGP 8.13.2 emits an advisory because it was tested through compile SDK 36.1. Compilation, lint, both test variants and assembly pass against SDK 37.0. SDK command-line tool metadata also produces a tooling XML-version advisory. Native dependency symbols were retained when the local strip tool could not strip them; APK assembly succeeded.

### Phone checklist

- Listen across at least two loop boundaries for every recording, then combine all 14 and check volume/headroom.
- Lock the screen, leave the app, and use play/pause and preset next/previous on the notification, lockscreen and headset.
- Start playback, enable battery saver, and confirm pause. Disable saver and confirm it stays paused until Play.
- Interrupt with another media app or a call, and unplug headphones. Confirm the mix pauses.
- Add a local file and a document-provider file, rename them, restart the app and reboot. Check access and graceful errors if a file disappears.
- Check theme switching, system bars, landscape, large text, TalkBack slider adjustment and controls.

## Final Sol review

Round 1 found and reproduced two issues: notification controls lost on unmute, and main-thread DataStore writes. Both are fixed with focused regression coverage. The new checks failed before the fixes and pass afterward. Full checks pass after the fixes. Round 2 approved both fixes with no new findings; the loop converged in two rounds with GPT-6.1 Sol at high reasoning effort. The ledger is in `docs/sol-loop.md`.

The final debug APK SHA-256 is `a2407c46ace33391e1b815744bcc992d4d55cc078d0709fb91805ef49dc42361`. Its v2 signature verifies. The APK is `app/build/outputs/apk/debug/app-debug.apk`.

## Local delivery and cleanup

The implementation is committed as `f8d8d73`, original assets/licenses as `958804a`, and requested agent instructions as `e8de0dd`. Verification artifacts and the review ledger are committed separately. No remote was added and nothing was pushed. `./gradlew --stop` reported no running Gradle daemons. No test-owned browser or preview server remains.
