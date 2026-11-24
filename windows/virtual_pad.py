"""Feed bridge controller state into a virtual Xbox 360 gamepad (ViGEm).

Uses `vgamepad` (pip install vgamepad) which drives the ViGEmBus driver to
create a virtual Xbox 360 controller on Windows. Games and emulators that
support XInput — Xenia Canary, Forza, most PC games — see it as a real
Xbox 360 pad.

Usage (in server.py or standalone):
    pad = VirtualPad()
    pad.apply(state)          # state = controller_state.ControllerState
    pad.close()

Requires Windows + ViGEmBus driver (vgamepad installs it automatically on
first pip install). On other platforms this module is unavailable and the
bridge keeps working as a display-only bridge.
"""
from __future__ import annotations

import logging
from typing import Optional

log = logging.getLogger(__name__)

# XInput deadzone-free passthrough: we forward raw values, the game
# applies its own response curves.
_VGAMEPAD_BUTTONS = None  # resolved lazily; None if vgamepad is missing

try:
    import vgamepad as vg

    _VG_AVAILABLE = True
except Exception:  # pragma: no cover - Windows/driver dependent
    vg = None
    _VG_AVAILABLE = False


def is_available() -> bool:
    return _VG_AVAILABLE


class VirtualPad:
    """Mirrors ControllerState onto a virtual Xbox 360 gamepad."""

    def __init__(self):
        if not _VG_AVAILABLE:
            raise RuntimeError(
                "vgamepad is not available (needs Windows + ViGEmBus driver: "
                "pip install vgamepad)"
            )
        self.gamepad = vg.VX360Gamepad()
        self._last: Optional[tuple] = None
        log.info("Virtual Xbox 360 gamepad created via ViGEmBus")

    def apply(self, state) -> None:
        """Push a controller_state.ControllerState snapshot to the pad."""
        buttons = state.buttons          # dict name->bool
        sticks = state.sticks            # {"left": {"x","y"}, "right": {...}}
        triggers = state.triggers        # {"LT","RT"}
        sig = (tuple(sorted(buttons.items())),
               sticks["left"]["x"], sticks["left"]["y"],
               sticks["right"]["x"], sticks["right"]["y"],
               triggers["LT"], triggers["RT"])
        if sig == self._last:
            return  # no change, skip report update
        self._last = sig
        g = self.gamepad
        gp = vg.XUSB_BUTTON
        mapping = {
            "A": gp.XUSB_GAMEPAD_A,
            "B": gp.XUSB_GAMEPAD_B,
            "X": gp.XUSB_GAMEPAD_X,
            "Y": gp.XUSB_GAMEPAD_Y,
            "LB": gp.XUSB_GAMEPAD_LEFT_SHOULDER,
            "RB": gp.XUSB_GAMEPAD_RIGHT_SHOULDER,
            "View": gp.XUSB_GAMEPAD_BACK,
            "Menu": gp.XUSB_GAMEPAD_START,
            "Guide": gp.XUSB_GAMEPAD_GUIDE,
            "L3": gp.XUSB_GAMEPAD_LEFT_THUMB,
            "R3": gp.XUSB_GAMEPAD_RIGHT_THUMB,
            "Up": gp.XUSB_GAMEPAD_DPAD_UP,
            "Down": gp.XUSB_GAMEPAD_DPAD_DOWN,
            "Left": gp.XUSB_GAMEPAD_DPAD_LEFT,
            "Right": gp.XUSB_GAMEPAD_DPAD_RIGHT,
        }
        # Build the target wButtons mask
        w = 0
        for name, flag in mapping.items():
            if buttons.get(name):
                w |= flag
        g.report.wButtons = w

        # Sticks: XInput uses signed 16-bit; vgamepad floats are -1..1.
        g.left_joystick_float(x_value_float=sticks["left"]["x"],
                              y_value_float=sticks["left"]["y"])
        g.right_joystick_float(x_value_float=sticks["right"]["x"],
                               y_value_float=sticks["right"]["y"])
        g.left_trigger_float(value_float=triggers["LT"])
        g.right_trigger_float(value_float=triggers["RT"])
        g.update()

    def close(self) -> None:
        try:
            self.gamepad.reset()
            self.gamepad.update()
        except Exception:
            pass
        log.info("Virtual gamepad closed")
