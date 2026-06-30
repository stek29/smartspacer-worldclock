# SmartSpacer World Clock

SmartSpacer World Clock is a standalone SmartSpacer plugin that provides a text-based complication showing the current time in a configured timezone.

Each complication instance has independent settings for timezone, mode, time format, and label.

## Features

- Multiple independent complication instances.
- Normal mode: always visible.
- Home mode: visible only when the device's current UTC offset differs from the configured home timezone's current UTC offset.
- DST-aware offset comparison.
- System, 12-hour, or 24-hour time format.
- Optional custom label or dynamic GMT offset label.
- Minute-level updates via SmartSpacer broadcast provider.
- Tap action opens the system clock/alarms app.

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

## Manual Testing

Install the debug APK on a device with SmartSpacer:

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

Then add the complication from SmartSpacer and verify:

- Default setup uses Home mode and the device timezone.
- Home mode hides when device and configured timezone offsets match.
- Home mode shows when offsets differ.
- Normal mode always shows.
- Multiple instances keep separate settings.
- Time format and label settings update the complication.
- Tap opens the clock/alarms app.

## Localisation

User-facing strings live in `app/src/main/res/values/strings.xml`. Future translations can be added with Android resource directories such as:

```text
app/src/main/res/values-de/strings.xml
app/src/main/res/values-fr/strings.xml
```

Technical identifiers, JSON keys, DataStore keys, and time format patterns are intentionally not localised.
