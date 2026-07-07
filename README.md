# Fylgja

FOSS Android habit-tracker that uses an Emacs org-mode file as its database.
Named after the Norse following/companion spirit — it follows your habits.

> Fallback names (in case of rebrand): Notch, Talloho

## Features (Phase 1)

- Pick a `habits.org` file via SAF (Storage Access Framework)
- Combined counter widget (1×1): shows today's count + icon, tap to increment
- Graph widget: bar chart of last N days
- Background org file reader/writer
- Local cache via DataStore
- No Google Play Services — designed for GrapheneOS

## Build

1. Open this directory in Android Studio (Iguana or later)
2. Sync Gradle
3. Run on device/emulator

Build from command line:
```bash
./gradlew assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Setup

1. Install the app
2. Open it → pick your `habits.org` file (anywhere SAF can reach: Syncthing folder, Documents, etc.)
3. Add widgets to your home screen:
   - CountWidget (counter tap)
   - GraphWidget (bar chart)
4. Sync the org file via Syncthing to your desktop Emacs

## Org file format

```org
* Habits
:PROPERTIES:
:VERSION: 1
:END:
** Smoke
:PROPERTIES:
:ICON: 🚬
:COLOR: #e74c3c
:TYPE: counter
:END:
:LOGBOOK:
CLOCK: [2026-06-28 Sun 14:32]
:END:
```

## Companion Emacs Lisp

See `companion/counter-org-logbook.el` — computes habit stats on desktop Emacs by reading LOGBOOK CLOCK entries.
