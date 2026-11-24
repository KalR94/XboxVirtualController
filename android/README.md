# Xbox Virtual Controller (Android)

Kotlin + Jetpack Compose (Material 3) + MVVM + Coroutines. minSdk 26, targetSdk 34.
Package/namespace: `com.haikal.xboxcontroller`.

## What it is

A virtual Xbox controller app. The phone acts as an Xbox pad on a **Windows PC
via the USB-to-Windows bridge**: the app opens a TCP connection to
`127.0.0.1:8765` (made reachable with `adb reverse tcp:8765 tcp:8765`) and sends
JSON input frames.

**Honest compatibility note:** direct output to a physical Xbox console is
**NOT supported**. PC bridge is SUPPORTED; some Series consoles are marked
REQUIRES_HARDWARE. See the Console Selection screen for per-profile status.

## Wire format (one JSON object per line)

```json
{"type":"controller_input","console":"XBOX_ONE",
 "buttons":{"A":true,"B":false,"X":false,"Y":false,"LB":false,"RB":false,
            "Up":false,"Down":false,"Left":false,"Right":false,
            "View":false,"Menu":false,"Guide":false,"L3":false,"R3":false},
 "left_stick":{"x":-1.0,"y":0.5},
 "right_stick":{"x":0.0,"y":0.0},
 "triggers":{"LT":0.0,"RT":1.0}}
```

Sticks are -1.0..1.0 (y up = +1.0); triggers 0.0..1.0. Sending is change-based
and rate-limited to ~60 Hz. Auto-reconnect uses exponential backoff (0.5s→8s,
max 8 attempts).

## Setup

1. Open this `android/` folder in **Android Studio** (Ladybug or newer with
   AGP 8.4 / Gradle 8.7).
2. **The Gradle wrapper JAR is intentionally not committed.** Generate it with
   `gradle wrapper --gradle-version 8.7` or let Android Studio create it
   (File → Sync). `gradle/wrapper/gradle-wrapper.properties` already pins
   `gradle-8.7-bin.zip`.
3. On the PC running the bridge: `adb reverse tcp:8765 tcp:8765`.
4. Bridge host/port are configurable in Settings.

## Local testing mode

Toggle in Home/Settings. No connection is made; every input change is logged
to the on-screen debug panel (last 100 entries) instead of being sent.

## Structure

```
app/src/main/java/com/haikal/xboxcontroller/
├── MainActivity.kt          # NavHost: Home / ConsoleSelection / VirtualController / Settings
├── connection/              # ConnectionManager, UsbClient, InputSerializer, ConnectionStatus
├── console/                 # ConsoleProfile (9 profiles, generation + compatibility)
├── data/                    # DataStore SettingsRepository
├── viewmodel/               # ControllerViewModel, HomeViewModel
├── navigation/              # Screen routes
└── ui/                      # theme (Xbox Elite dark), home, console, controller, settings
```

JSON uses the platform-bundled `org.json` — no extra serialization dependency.
