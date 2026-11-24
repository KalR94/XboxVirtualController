"""Real-time console monitor for the bridge. Uses rich if available, plain print otherwise."""
from __future__ import annotations

import sys
import threading
import time
from typing import Optional

try:
    from rich.console import Console
    from rich.live import Live
    _RICH = True
except ImportError:
    _RICH = False

from controller_state import BUTTON_NAMES, ControllerState


class Monitor:
    def __init__(self, server):
        self.server = server
        self._lock = threading.Lock()
        self._log_lines: list = []
        self._last_display_state: Optional[ControllerState] = None
        self._input_count = 0
        self._error_count = 0
        self._console = Console() if _RICH else None

    # ----- callbacks (called from server threads) -----
    def on_input(self, client_id, state: ControllerState, diff: dict):
        with self._lock:
            self._input_count += 1
            if diff:
                self._log(f"client {client_id} input: {state.summary()}")
                self._last_display_state = state
            self._render()

    def on_error(self, client_id, message: str):
        with self._lock:
            self._error_count += 1
            self._log(f"client {client_id} ERROR: {message}")
            self._render()

    def on_connect(self, client_id):
        with self._lock:
            self._log(f"client {client_id} connected")
            self._render()

    def on_disconnect(self, client_id):
        with self._lock:
            self._log(f"client {client_id} disconnected")
            self._render()

    def log(self, message: str):
        with self._lock:
            self._log(message)
            self._render()

    # ----- internals -----
    def _log(self, message: str):
        self._log_lines.append(f"[{time.strftime('%H:%M:%S')}] {message}")
        self._log_lines = self._log_lines[-12:]

    def _plain_render(self) -> str:
        state = self._last_display_state
        clients = self.server.manager.client_count()
        btn = ""
        if state:
            btn = " ".join(k for k in BUTTON_NAMES if state.buttons.get(k)) or "-"
            ls = f"L({state.left_stick['x']:+.2f},{state.left_stick['y']:+.2f})"
            rs = f"R({state.right_stick['x']:+.2f},{state.right_stick['y']:+.2f})"
            tr = f"LT={state.triggers['LT']:.2f} RT={state.triggers['RT']:.2f}"
            state_line = f"{btn} | {ls} {rs} | {tr}"
        else:
            state_line = "waiting for input..."
        lines = [
            "=== Xbox Virtual Controller Bridge (V3 research — no physical console output) ===",
            f"clients: {clients} | inputs: {self._input_count} | errors: {self._error_count}",
            f"state: {state_line}",
            "--- log ---",
        ]
        lines.extend(self._log_lines)
        return "\n".join(lines)

    def _render(self):
        text = self._plain_render()
        if _RICH:
            self._console.clear()
            self._console.print(text)
        else:
            sys.stdout.write("\x1b[2J\x1b[H" + text + "\n")
            sys.stdout.flush()

    def run(self):
        """Blocking main loop; refresh display periodically until interrupted."""
        try:
            while True:
                time.sleep(1.0)
                with self._lock:
                    self._render()
        except KeyboardInterrupt:
            pass
