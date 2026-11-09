# Blanket for Android

An unofficial native Android port of [Rafael Mardojai's Blanket 0.8.0](https://github.com/rafaelmardojai/blanket/tree/0.8.0). Kotlin, Jetpack Compose, Media3 and DataStore. Android 8.0 or later, target API 37. GPL-3.0-or-later, with separately licensed audio and font assets.

The app includes all 14 original recordings and symbolic icons, independent looping sound levels, a main volume, play/pause, automatically saved presets, custom document audio, light/dark/system appearance, background media controls, optional keep-awake and battery-saver pause.

## Build

Install a JDK 17 or later, Android command-line tools, SDK Platform 37.0, and Build-Tools 36.0.0. Set `JAVA_HOME` and `ANDROID_HOME`, or point `sdk.dir` in an untracked `local.properties` at the SDK. Gradle uses the checked-in wrapper and version catalog.

```sh
./gradlew :app:spotlessCheck lint test assembleDebug
```

The debug APK is `app/build/outputs/apk/debug/app-debug.apk`. The Gradle cache needs several GB on the first build. Use `GRADLE_USER_HOME` on a disk with enough room. On the build machine the JDK is `~/.local/blanket-jdk`, SDK is `~/Android/sdk`, and the task cache is `/var/tmp/blanket-gradle/cache`.

```sh
export JAVA_HOME="$HOME/.local/blanket-jdk"
export ANDROID_HOME="$HOME/Android/sdk"
export GRADLE_USER_HOME=/var/tmp/blanket-gradle/cache
./gradlew :app:spotlessCheck lint test assembleDebug --no-daemon
scripts/verify-assets.sh app/build/outputs/apk/debug/app-debug.apk
```

## Install with wireless ADB

Enable Developer options and Wireless debugging on the phone. Pair and connect from a computer on the same network. Use the addresses and ports shown by Android.

```sh
adb pair PHONE_IP:PAIRING_PORT
adb connect PHONE_IP:DEBUG_PORT
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

You can also copy the APK to the phone and open it, allowing installation from that file app. This debug build is for sideload testing, not a signed release.

## Use

Tap a sound icon to enable it. Drag its slider to change its level. The bottom play button pauses the entire mix. The speaker opens main volume. The adjacent menu saves a preset or adds an audio document. The top selector opens presets. A custom card's menu renames or removes it. Main menu opens Preferences and About.

Edits save automatically to the selected preset. Default cannot be deleted. Notification and lockscreen next/previous switch presets without wrapping. Custom files stay in their original provider; keep them available. Removing a sound leaves the original file intact.

## Verify

Unit and Robolectric tests run without an emulator. Compose UI tests run with native Robolectric graphics. Roborazzi screenshots are in `docs/screenshots/`, next to upstream references in `docs/reference/`. To refresh screenshots deliberately:

```sh
./gradlew :app:testDebugUnitTest --tests '*BlanketUiTest*' -Proborazzi.test.record=true --rerun-tasks --no-daemon
```

`docs/acceptance.md` records the autonomously resolved questions. `docs/architecture.md` explains ownership. `logbook.md` records decisions and remaining checks. Test XML and HTML reports are generated under `app/build/test-results/` and `app/build/reports/tests/`. Android lint writes `app/build/reports/lint-results-debug.html`.

## Check on a real phone

No emulator or device was available. Robolectric cannot prove audible continuity, decoder behavior, power consumption, real notification/lockscreen layout, audio focus enforcement, or a provider's document grants.

- Listen across at least two loop boundaries for every recording, then combine all 14 and check volume/headroom.
- Lock the screen, leave the app, and use play/pause and preset next/previous on the notification, lockscreen and headset.
- Start playback, enable battery saver, and confirm pause. Disable saver and confirm it stays paused until Play.
- Interrupt with another media app or a call, and unplug headphones. Confirm the mix pauses.
- Add a local file and a document-provider file, rename them, restart the app and reboot. Check access and graceful errors if a file disappears.
- Check theme switching, system bars, landscape, large text, TalkBack slider adjustment and controls.

## Credits

Original Blanket by Rafael Mardojai CM. Sound icons by Jakub Steiner. Sound editing by Porrumentzio. All sound authors, sources and licenses are listed in [SOUNDS_LICENSING.md](SOUNDS_LICENSING.md) and in About. Cantarell by Dave Crossland and contributors, SIL OFL 1.1. Full GPL text is in [LICENSE](LICENSE).
