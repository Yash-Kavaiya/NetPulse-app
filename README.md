# NetPulse

Android data-usage monitor: per-app mobile and Wi-Fi usage, billing-cycle plan tracking with alerts,
history charts, an in-app speed test, a live-speed notification with a Quick Settings tile, a home-screen
widget, CSV export and backup, and Gemini-powered usage insights.

Kotlin · Jetpack Compose (Material 3) · Hilt · Room · DataStore · WorkManager · Glance · OkHttp · Firebase AI Logic

## Features

| Area | What it does |
| --- | --- |
| **Home** | Today vs yesterday, billing-cycle usage against your plan, end-of-cycle forecast, top apps, live speed |
| **Apps** | Per-app usage with range, network and sort filters and search; detail sheet with foreground/background split and a 14-day trend chart |
| **History** | 7, 30 and 90-day stacked mobile/Wi-Fi charts, tap a day for hourly usage, comparison with the previous period |
| **Speed** | Ping, jitter, download and upload test (Cloudflare endpoints) with saved result history |
| **Insights** | Gemini summary of your usage with data-saving tips (sends only app names and byte counts) |
| **Alerts** | Hourly background check that sends one warning and one limit notification per billing cycle |
| **Live monitor** | Ongoing ↓/↑ speed notification, toggled in Settings or from the Quick Settings tile |
| **Widget** | Today's usage and cycle progress on the home screen |
| **Export** | CSV export of daily usage, app usage and speed tests; JSON backup and restore of plan and settings |

## Project layout

```
app/src/main/java/com/example/
  NetPulseApp.kt          Hilt application, WorkManager config, notification channels, Firebase/App Check
  MainActivity.kt         Splash, theme, onboarding vs main navigation
  di/                     Hilt modules (Room, DataStore, OkHttp, stats binding)
  data/stats/             NetworkStatsManager access (NetworkStatsHelper), TimeRanges, StatsRepository
  data/room/              Room database v2 (plan, daily snapshots, speed tests, AI insight cache) + migration
  data/plan/              Billing-cycle usage and alert rules
  data/prefs/             DataStore settings
  data/speedtest/         OkHttp speed-test engine
  data/ai/                Gemini prompt + Firebase AI client
  data/export/            CSV and JSON backup
  work/                   UsageSnapshotWorker, DataLimitCheckWorker, scheduling
  live/                   Live-speed foreground service and Quick Settings tile
  widget/                 Glance app widget
  ui/                     Compose screens (dashboard, apps, history, speedtest, insights, settings, about, onboarding)
app/src/debug|release/    App Check provider per build type
app/schemas/              Exported Room schemas (used by the migration test)
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

### Enabling AI insights (Firebase)

1. Create a Firebase project, add an Android app with package `com.aistudio.netpulse.kpzvrq`, and
   download `google-services.json` into `app/`. The file is gitignored.
2. Enable **Firebase AI Logic** with the Gemini Developer API.
3. Enable **App Check**: use Play Integrity for release builds, and register the debug token from
   Logcat (`DebugAppCheckProvider`) for debug builds.
4. Optional: choose the model with `-Pnetpulse.geminiModel=<model-name>` (default `gemini-2.5-flash`).

Without `google-services.json` the app still builds and runs. The Insights tab then shows a
"not configured" message.

## Permissions

| Permission | Why |
| --- | --- |
| `PACKAGE_USAGE_STATS` (Usage access, granted by the user in Settings) | Read per-app network statistics |
| `POST_NOTIFICATIONS` | Plan alerts and the live-speed notification |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_SPECIAL_USE` | Live-speed notification while the user keeps it on |
| `INTERNET`, `ACCESS_NETWORK_STATE` | Speed test, AI insights, network type |

`QUERY_ALL_PACKAGES` is **not** requested. A `<queries>` launcher filter resolves the names and icons of
installed launcher apps. Packages without a launcher activity appear as "System Service (UID n)".

## Play Store notes

- **Data safety:** usage statistics stay on the device. Only when the user taps Generate are app names
  and byte totals sent to Google (Gemini) to produce insights. Speed tests exchange test data with
  Cloudflare. There are no analytics, ads or accounts.
- **Special-use foreground service:** declare it in Play Console as a "user-initiated, ongoing network
  throughput indicator".
- **Privacy policy:** host a policy based on the About screen's Privacy section and link it in the listing.
