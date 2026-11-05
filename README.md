# Fold

A home-screen launcher for the Galaxy Z Fold 8 that learns how you use your phone instead of making you arrange it.

## What it does

- **Learns your habits.** It records which app you open, at what time of day, on weekdays or weekends, and with the phone folded, open or half-open. The "Right now" section shows the apps you're most likely to want at that moment. Older habits fade, with a half-life of two weeks. Everything stays in a small file on the phone.
- **Type first.** Start typing and press Go to open the top match. Matching is forgiving: `gm` finds Google Maps, `wa` finds WhatsApp, `wtsp` also finds WhatsApp.
- **A layout for each posture:**
  - **Folded (cover screen):** a big clock, your eight likeliest apps, and search at thumb height.
  - **Open:** two panes. On the left are the time and your predictions, on the right every app with search.
  - **Half-open on a table:** the top half is a clock you can glance at, and the bottom half holds search and apps.
- **Long-press any app** to open its app info, hide it from home (it stays in search), forget its usage history, or uninstall it.
- Shows work-profile apps, and refreshes when apps are installed or removed.

## Install

1. Build it, or use a prebuilt `app-release.apk`:
   ```bash
   ./gradlew assembleRelease
   ```
   The APK is written to `app/build/outputs/apk/release/app-release.apk`.
2. Copy it to the phone and open it. Allow installing from that source when asked.
3. Go to **Settings → Apps → Choose default apps → Home app** and pick **Fold**.

To switch back, pick One UI Home in the same setting.

## Code

| File | Purpose |
|---|---|
| `MainActivity.kt` | The home activity. It watches the hinge through Jetpack WindowManager. |
| `AppRepository.kt` | Lists, launches and watches installed apps (through `LauncherApps`). |
| `UsageModel.kt` | The on-device learning model that predicts your next app. |
| `Search.kt` | Fuzzy search over app names. |
| `ui/LauncherScreen.kt` | The three posture layouts, written in Compose. |
