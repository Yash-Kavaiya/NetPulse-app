# NetPulse

Android data-usage monitor: per-app mobile and Wi-Fi usage, billing-cycle plan tracking with alerts,
history charts, an in-app speed test, a home-screen widget, CSV export and backup, and on-device smart tips.

Kotlin · Jetpack Compose (Material 3) · Hilt · Room · DataStore · WorkManager · Glance · OkHttp

## Features

| Area | What it does |
| --- | --- |
| **Home** | Today's usage with a 7-day sparkline, plan progress ring with forecast, on-device smart tips, top apps, live speed |
| **Apps** | Per-app usage with range, network and sort filters and search; detail sheet with foreground/background split and a 14-day trend chart |
| **History** | 7, 30 and 90-day stacked mobile/Wi-Fi charts, tap a day for hourly usage, comparison with the previous period |
| **Speed** | Ping, jitter, download and upload test (Cloudflare endpoints) with saved result history |
| **Alerts** | Hourly background check that sends one warning and one limit notification per billing cycle |
| **Widget** | Today's usage and cycle progress on the home screen |
| **Export** | CSV export of daily usage, app usage and speed tests; JSON backup and restore of plan and settings |

## Project layout

```
app/src/main/java/com/example/
  NetPulseApp.kt          Hilt application, WorkManager config, notification channels
  MainActivity.kt         Splash, theme, onboarding vs main navigation
  di/                     Hilt modules (Room, DataStore, OkHttp, stats binding)
  data/stats/             NetworkStatsManager access (NetworkStatsHelper), TimeRanges, StatsRepository
  data/room/              Room database v3 (plan, daily snapshots, speed tests) + migrations
  data/plan/              Billing-cycle usage and alert rules
  data/prefs/             DataStore settings
  data/speedtest/         OkHttp speed-test engine
  data/export/            CSV and JSON backup
  work/                   UsageSnapshotWorker, DataLimitCheckWorker, scheduling
  widget/                 Glance app widget
  ui/                     Compose screens (dashboard, apps, history, speedtest, settings, about, onboarding)
app/schemas/              Exported Room schemas
```

## Building

Requirements: JDK 17+ (21 recommended) and the Android SDK with platform 36.

```bash
./gradlew assembleDebug          # debug APK
./gradlew testDebugUnitTest      # JVM + Robolectric unit tests
./gradlew lintDebug
./gradlew bundleRelease          # release AAB (R8 shrinking enabled)
```

### Release signing

Release builds are signed only when a keystore exists. Provide it through environment variables:

| Variable | Meaning |
| --- | --- |
| `KEYSTORE_PATH` | Path to the upload keystore (default `my-upload-key.jks` in the project root) |
| `STORE_PASSWORD` / `KEY_PASSWORD` | Keystore and key passwords |
| `KEY_ALIAS` | Key alias (default `upload`) |

Set the version with `-Pnetpulse.versionCode=3 -Pnetpulse.versionName=2.1.0`.

### Theming

Light and dark themes use separate palettes (`ui/theme/Color.kt`). Dark mode uses stepped surface
tones instead of shadows and desaturated accent colors. Components read colors through
`LocalGoogleColors`, so they follow the active theme and the optional Material You dynamic color.

## Permissions

| Permission | Why |
| --- | --- |
| `PACKAGE_USAGE_STATS` (Usage access, granted by the user in Settings) | Read per-app network statistics |
| `POST_NOTIFICATIONS` | Plan alerts |
| `INTERNET`, `ACCESS_NETWORK_STATE` | Speed test, network type |

`QUERY_ALL_PACKAGES` is **not** requested. A `<queries>` launcher filter resolves the names and icons of
installed launcher apps. Packages without a launcher activity appear as "System Service (UID n)".

## Play Store notes

- **Data safety:** usage statistics stay on the device. Speed tests exchange test data with
  Cloudflare. There are no analytics, ads or accounts.
- **Privacy policy:** host a policy based on the About screen's Privacy section and link it in the listing.
