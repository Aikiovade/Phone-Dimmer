# Night Dimmer

An Android app that dims the screen **below the system minimum brightness** and
adds a warm blue-light filter, so reading in the dark does not hurt your eyes.

[![CI](https://github.com/Aikiovade/Phone-Dimmer/actions/workflows/ci.yml/badge.svg)](https://github.com/Aikiovade/Phone-Dimmer/actions/workflows/ci.yml)

## Features

| Feature | What it does |
|---|---|
| Extreme dimming | Draws a black overlay on top of everything, down to almost black. |
| Blue-light filter | Amber overlay whose strength is configurable, independent of the dim level. |
| Auto-dimming | Follows the ambient light sensor with hysteresis and smooth fades, so it does not flicker. |
| Schedule | Turns the dimmer on and off at fixed times; windows may cross midnight (22:00 – 07:00). |
| Quick settings tile | Toggle the dimmer from the notification shade. |
| Deep black theme | Optional pure-black UI to avoid glare when the phone is used in the dark. |
| Boot restore | After a reboot the dimmer and the next schedule alarm come back automatically. |

The UI is English by default and Russian on Russian devices (`values-ru`).

## Install

Download the APK from the [Releases](https://github.com/Aikiovade/Phone-Dimmer/releases)
page and open it on the device. Android will ask for the "Display over other
apps" permission the first time the dimmer is switched on.

Requirements: Android 8.0 (API 26) or newer.

## How it works

* **Dimming** – a `TYPE_APPLICATION_OVERLAY` window is drawn over the screen. A
  system window can be darker than the brightness slider allows, which is what
  makes "below minimum brightness" possible. Because it is an overlay, it does
  not save battery on OLED panels; it reduces emitted light, not power draw.
* **Auto-dimming** – lux values are mapped to five levels with a hysteresis band
  around every threshold and exponential smoothing on the way out. See
  `domain/AutoBrightnessPolicy.kt`.
* **Schedule** – only one alarm is armed at a time: the next transition of the
  window. When it fires, the receiver applies the change and arms the following
  transition. See `domain/ScheduleCalculator.kt`.

### Permissions

| Permission | Why |
|---|---|
| `SYSTEM_ALERT_WINDOW` | Draw the dimming overlay. Also exempts the app from Android 12+ background foreground-service restrictions. |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE` | Keep the overlay alive with a visible notification. |
| `POST_NOTIFICATIONS` | Show that notification on Android 13+. Requested the first time the dimmer is enabled. |
| `SCHEDULE_EXACT_ALARM` | Fire the schedule on time. The app falls back to an inexact alarm when the user denies it and shows a hint. |
| `RECEIVE_BOOT_COMPLETED` | Restore the dimmer and re-arm the schedule after a reboot. |

The app has no `INTERNET` permission: nothing leaves the device and there is no
telemetry. All settings live in a private `SharedPreferences` file.

## Build from source

```bash
# requirements: JDK 17+ (JDK 21 recommended) and an Android SDK with API 36.1
echo "sdk.dir=/path/to/Android/Sdk" > local.properties
./gradlew assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest      # 29 unit tests
./gradlew lintDebug              # Android lint
```

The project uses Gradle 9.3.1 (via the wrapper), AGP 9.1.1, Kotlin 2.2.10 and
the Compose BOM 2024.09.00. Those versions are pinned deliberately; the app is
verified against them on CI.

### Signed release builds

`assembleRelease` produces a minified APK. It is signed only when a keystore is
available, otherwise it stays unsigned:

```bash
export STORE_PASSWORD=...
export KEY_ALIAS=upload
export KEY_PASSWORD=...
./gradlew assembleRelease        # uses ./release-keystore.jks
```

For CI, add these repository secrets:

| Secret | Content |
|---|---|
| `RELEASE_KEYSTORE_BASE64` | `base64 -w0 release-keystore.jks` |
| `STORE_PASSWORD` | keystore password |
| `KEY_ALIAS` | key alias (`upload`) |
| `KEY_PASSWORD` | key password |

Pushing a `v*` tag then publishes a GitHub release with the APK attached
(`.github/workflows/release.yml`). Without the secrets the workflow attaches the
installable debug APK instead.

Forgot to bump the version? Update `versionCode` / `versionName` in
`app/build.gradle.kts` before tagging.

## Project layout

```
app/src/main/java/io/github/aikiovade/nightdimmer/
├── MainActivity.kt              # Compose host, permission flows
├── NightDimmerApp.kt            # process-scoped object graph
├── data/                        # SettingsRepository + key/value persistence
├── domain/                      # pure Kotlin: schedule, auto-brightness, overlay math
├── service/                     # overlay service, tile, alarm scheduler, receiver
└── ui/                          # ViewModel, Compose screen, theme
```

The `domain` package has no Android imports, which is why the schedule and
auto-dimming logic can be unit tested without an emulator.

## Known limitations

* The overlay dims the rendered image; it cannot lower the hardware backlight.
* Dimming is applied to the primary display only.
* Very dark levels make system dialogs hard to read; keep the level at a value
  that still lets you hit "OK".
* `SCHEDULE_EXACT_ALARM` is denied by default on Android 14+ for newly installed
  apps unless the user allows it in the app settings; the UI warns about it.

## License

[MIT](LICENSE)
