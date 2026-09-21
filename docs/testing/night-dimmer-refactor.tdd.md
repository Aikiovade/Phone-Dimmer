# Night Dimmer refactor — verification evidence

Run during the same session that rewrote the app. Every result below comes from
the commands shown, executed in the repository checkout.

## Environment

| Item | Value |
|---|---|
| Gradle | 9.3.1 (`./gradlew`, wrapper committed) |
| JDK | 21.0.10 (Oracle) |
| Android SDK | platforms 33–36.1, build-tools 36.0.0 |
| AGP / Kotlin | 9.1.1 / 2.2.10 |

## Commands and results

| Command | Result |
|---|---|
| `clean :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease :app:lintDebug` | `BUILD SUCCESSFUL in 7m 3s` |
| `:app:testDebugUnitTest` | 29 tests, 0 failures, 0 errors |
| `:app:assembleDebug` | `app-debug.apk`, 16.4 MB |
| `:app:assembleRelease` | `app-release-unsigned.apk`, 1.14 MB (R8 + resource shrinking) |
| `:app:lintDebug` | `0 errors, 12 warnings` (all "newer dependency version available") |

Test breakdown, from `app/build/test-results/testDebugUnitTest/*.xml`:

```
io.github.aikiovade.nightdimmer.data.SettingsRepositoryTest      5 tests, 0 failures
io.github.aikiovade.nightdimmer.domain.AutoBrightnessPolicyTest  7 tests, 0 failures
io.github.aikiovade.nightdimmer.domain.model.ScheduleWindowTest  4 tests, 0 failures
io.github.aikiovade.nightdimmer.domain.OverlaySpecTest           5 tests, 0 failures
io.github.aikiovade.nightdimmer.domain.ScheduleCalculatorTest    8 tests, 0 failures
```

## What the tests guarantee

| # | Guarantee | Test |
|---|---|---|
| 1 | A reboot inside a 22:00–07:00 window schedules *off* at 07:00 instead of leaving the screen dark until the next evening | `ScheduleCalculatorTest.a reboot inside the window must not wait for the next start` |
| 2 | The start minute is inside the window, the end minute is outside, so transitions hit exactly once | `ScheduleCalculatorTest.exactly at the start minute…`, `…exactly at the end minute…` |
| 3 | A window may cross midnight and the same rules hold on both sides of it | `ScheduleWindowTest.a window crossing midnight covers both sides of the day` |
| 4 | Start == end disables the schedule instead of dimming for 24 h | `ScheduleWindowTest.equal start and end disable the window`, `ScheduleCalculatorTest.a disabled window never schedules anything` |
| 5 | Auto-dimming does not oscillate at a threshold (hysteresis) | `AutoBrightnessPolicyTest.hysteresis keeps the level stable around a boundary` |
| 6 | Auto-dimming fades instead of jumping, and the first sample is applied instantly | `AutoBrightnessPolicyTest.a real change fades in instead of jumping`, `…the first measurement is applied without fading` |
| 7 | Taking over from a manual level fades from that level | `AutoBrightnessPolicyTest.seeding fades from the previous manual level` |
| 8 | Dim and blue-light layers are computed independently and clamped to 0..1 | `OverlaySpecTest.*` |
| 9 | Slider changes are kept by a separate explicit persist step and survive a process restart | `SettingsRepositoryTest.persisted updates survive a restart`, `…in-memory updates are only written when asked` |
| 10 | Corrupted preferences cannot push values out of their valid range | `SettingsRepositoryTest.corrupted stored values are clamped on read`, `…out of range values are clamped on write` |

## Manual checks performed

* Resource move (`mipmap-anydpi-v26` → `mipmap-anydpi`, raster mipmaps removed)
  verified by a `clean` build with the Gradle build cache disabled, after a warm
  cache restored stale packaged resources once.
* `lint` error `StartActivityAndCollapseDeprecated` fixed and re-verified; the
  remaining warnings are dependency-update suggestions only.

## Not verified automatically

* Behaviour on a physical device (sensor curve, overlay across a real reboot,
  tile behaviour, `setExactAndAllowWhileIdle` timing). Instrumented tests were
  removed because the repository cannot run them; the service layer stays thin
  and the logic it delegates to is covered by the tests above.
