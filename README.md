# Fold

A home-screen launcher for the Galaxy Z Fold 8, built for running a one-person studio from a phone. It also learns how you use the phone, so you don't have to arrange it.

## The studio part

- **The Garage.** Every project gets a parking bay, like a car-game garage: a light strip in its colour, a painted bay number, and a green dot if it has a live site. Colours: gold for client work, teal for your own products, pink for games, peach for personal projects. Tap a bay to open the live site, the repo or the host dashboard (Netlify, Vercel, Render), or to hand the project to the Claude app. There's also a **"Lives under"** field for which account a project is deployed from, so you don't have to remember. Never put passwords in it. The garage starts with the projects from your Claude Code sessions; add, edit or delete bays from there.
- **Night shift.** From 6 pm to 4 am the smoke background thickens, its gold glints brighten, and a NIGHT SHIFT badge appears.
- **Dial clock.** The clock is tilted back for depth. Its gold ticks fill as the hour passes and a bead marks the hour. Tap the line under it to cycle a second city: Dhaka, London, Singapore, LA, or off.
- **Studio row.** Claude, GitHub, Gemini, WhatsApp (including Business), Telegram, Stripe, Chrome and Termux, showing whichever of them are installed.
- **Search finds projects too.** Type `near` to get the Near Coffee bay as well as any apps that match.

## The garage, in motion (0.3)

- **Roll-up door.** Coming back to the phone after a couple of minutes, or unfolding it, rolls a garage door up with gold light spilling out under it. While you unfold, the door follows the hinge sensor, so it opens as fast as you open the phone.
- **Bay lights.** As the door goes up, each bay's tube light flickers on, one bay after another.
- **Status as car lights.** Live is a breathing green light, Waiting is blinking amber hazards (with who you're waiting on), Building is a moving hazard-stripe scanner, and Parked has its lights off. Change a bay's status with one tap in its sheet. The open screen shows how many bays are waiting; tap that number to see them.
- **Dust and odometer.** Bays you don't open collect dust over three weeks, and opening one wipes it clean. The odometer counts your visits.
- **Rev to launch.** Hold a bay: a rev gauge sweeps up with haptic ticks, and at full revs it opens the live site (or the repo, or Claude).
- **Garage backdrop.** A shader draws haze, three ceiling lamps that occasionally stutter, and reflections on a wet floor.
- **Seconds comet** on the dial clock.
- **Tilt depth.** Tilting the phone shifts the backdrop, the dial and the bays by different amounts.
- **Your own art.** The Workshop sets a background per screen; tap a bay to give it its own art. Prompts for Nano Banana are in [`docs/nano-banana-prompts.md`](docs/nano-banana-prompts.md).

## The phone part

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
| `Projects.kt` | The Garage's projects, saved as JSON on the phone. |
| `ui/Garage.kt` | The bays, the project sheet and the edit dialog. |
| `ui/Look.kt` | Colours, the smoke shader, the dial clock and the night shift badge. |
