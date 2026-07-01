# SmartSpacer World Clock

SmartSpacer World Clock is a standalone SmartSpacer plugin that provides a world clock target and a compact text-based complication showing the current time in a configured timezone. It also provides a SmartSpacer Requirement for showing any target or complication only when a selected timezone has a different current GMT offset from the device.

Each target and complication instance has independent settings for timezone, mode, time format, icon, and label.

## Features

- Multiple independent target and complication instances.
- Target support with full title/subtitle display, optional AOD subtitle hiding, and support for attached SmartSpacer complications.
- Compact complication support for showing the world clock alongside other SmartSpacer targets.
- Normal mode: always visible.
- Home mode: visible only when the device's current UTC offset differs from the configured home timezone's current UTC offset.
- DST-aware offset comparison.
- System, 12-hour, or 24-hour time format.
- Optional custom label or dynamic GMT offset label.
- Minute-level updates via SmartSpacer broadcast provider while the target or complication is visible.
- Tap action opens the system clock/alarms app.
- Timezone Offset Requirement: can be attached to SmartSpacer targets or complications to require that a selected timezone's current GMT offset differs from the device's current GMT offset.

## Requirement provider

The Timezone Offset Requirement is configured separately from World Clock targets and complications. Its setup screen contains a status preview and a timezone selector, reusing the same timezone picker as the world clock configuration.

The requirement is met when the selected timezone and the device timezone currently have different GMT offsets. To require matching offsets instead, use SmartSpacer's built-in invert option on the requirement.

## Battery behaviour

Home mode avoids registering per-minute `ACTION_TIME_TICK` updates while a target or complication is hidden because the device's current UTC offset matches the configured home timezone. It still listens for time, timezone, and date changes so SmartSpacer can re-check whether the item should become visible.

This intentionally trades a small amount of DST-transition precision for lower idle wakeups: if two zones have the same offset and later diverge only because of a daylight-saving transition, the hidden target, complication, or timezone-offset requirement may not update until the next date/time/timezone broadcast rather than exactly at the transition minute.

## Requirements

- Android minSdk 29.
- SmartSpacer installed on the device.
- JDK 17.
- Android SDK platform `android-37.0`.
- Gradle wrapper included in this repo.

The build intentionally matches SmartSpacer's current SDK toolchain:

```properties
android.builtInKotlin=false
android.newDsl=false
```

This allows the project to use Kotlin Android plugin `2.4.0`, which is required by the published SmartSpacer SDK `1.1.2`.

## Build

Debug APK:

```bash
./gradlew assembleDebug
```

Unit tests:

```bash
./gradlew testDebugUnitTest
```

Release APK:

```bash
./gradlew assembleRelease
```

Release builds require signing credentials and fail early if they are missing.

## Release Signing

Release signing can be configured with a gitignored `keystore.properties` file in the repository root:

```properties
storeFile=/absolute/path/to/release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

Alternatively, use environment variables:

```bash
export ANDROID_KEYSTORE_FILE=/absolute/path/to/release.jks
export ANDROID_KEYSTORE_PASSWORD=...
export ANDROID_KEY_ALIAS=...
export ANDROID_KEY_PASSWORD=...
./gradlew assembleRelease
```

## Localisation

User-facing strings live in `app/src/main/res/values/strings.xml`. Future translations can be added with Android resource directories such as:

```text
app/src/main/res/values-de/strings.xml
app/src/main/res/values-fr/strings.xml
```

Technical identifiers, JSON keys, DataStore keys, and time format patterns are intentionally not localised.

## License

Original project code is licensed under the Zero-Clause BSD license. See `LICENSE`.

Third-party tooling, dependencies, and icon assets keep their own licenses. See `THIRD_PARTY_NOTICES.md`.
