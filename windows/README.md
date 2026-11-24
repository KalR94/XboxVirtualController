# Xbox Virtual Controller Bridge (Windows) — V3 Research

**Honest scope:** this bridge only *receives, validates and displays* controller input
sent from an Android app over TCP. It does **NOT** forward input to a physical Xbox
console. No ViGEm/ViGEmBus, no XInput injection, no console integration.

## Files

- `server.py` — TCP socket server on port 8765 (thread per client), newline-delimited JSON, sends ack per message.
- `input_parser.py` — validates `controller_input` payloads; clamps sticks to -1..1 and triggers to 0..1; rejects invalid payloads with a clear error message.
- `controller_state.py` — `ControllerState` dataclass (buttons, sticks, triggers, last_updated) with diff helpers.
- `connection_manager.py` — client connect/disconnect, ack `{"type":"ack","received":true,"seq":N}`, auto cleanup of dead clients, status.
- `monitor.py` — real-time console monitor (uses `rich` if installed, plain ANSI print otherwise).
- `tests/` — pytest suite including real loopback-socket integration tests.

## Protocol (from Android)

```json
{"type":"controller_input","console":"XBOX_ONE",
 "buttons":{"A":true,"B":false,"X":false,"Y":false,"LB":false,"RB":false,
            "View":false,"Menu":false,"Guide":false,"L3":false,"R3":false},
 "left_stick":{"x":0.0,"y":0.0},
 "right_stick":{"x":0.0,"y":0.0},
 "triggers":{"LT":0.0,"RT":0.0},
 "seq":1}
```

One JSON object per line, newline-terminated. Server replies:
`{"type":"ack","received":true,"seq":<your seq>}` or
`{"type":"error","message":"<why it was rejected>"}`.

## Run on Windows

```
cd windows
python -m venv .venv
.venv\Scripts\pip install -r requirements.txt
.venv\Scripts\python server.py
```

The server binds to `127.0.0.1:8765` by default. With `adb reverse`, the Android
device reaches it as `localhost:8765` on the phone itself.

## adb setup (Android device → bridge)

```
adb reverse tcp:8765 tcp:8765
```

Then the Android app connects to `127.0.0.1:8765` on the device, and traffic is
forwarded to the Windows bridge.

## Tests

```
.venv\Scripts\python -m pytest -v
```
