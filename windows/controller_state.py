"""ControllerState dataclass for the Xbox Virtual Controller bridge.

This bridge does NOT forward input to a physical Xbox console.
It only receives, parses, stores, and displays controller input from
an Android client (V3 research bridge).
"""
from __future__ import annotations

import time
from dataclasses import dataclass, field, replace
from typing import Any, Dict, Optional


BUTTON_NAMES = ("A", "B", "X", "Y", "LB", "RB", "View", "Menu", "Guide", "L3", "R3",
                "Up", "Down", "Left", "Right")  # Up/Down/Left/Right = D-Pad


@dataclass
class ControllerState:
    buttons: Dict[str, bool] = field(default_factory=lambda: {b: False for b in BUTTON_NAMES})
    left_stick: Dict[str, float] = field(default_factory=lambda: {"x": 0.0, "y": 0.0})
    right_stick: Dict[str, float] = field(default_factory=lambda: {"x": 0.0, "y": 0.0})
    triggers: Dict[str, float] = field(default_factory=lambda: {"LT": 0.0, "RT": 0.0})
    last_updated: float = field(default_factory=time.time)
    seq: Optional[int] = None

    def update_from(self, parsed: Dict[str, Any], seq: Optional[int] = None) -> "ControllerState":
        """Return a new ControllerState with values from a validated payload."""
        return ControllerState(
            buttons={b: parsed.get("buttons", {}).get(b, False) for b in BUTTON_NAMES},
            left_stick=dict(parsed.get("left_stick", self.left_stick)),
            right_stick=dict(parsed.get("right_stick", self.right_stick)),
            triggers=dict(parsed.get("triggers", self.triggers)),
            last_updated=parsed.get("last_updated", time.time()),
            seq=seq if seq is not None else parsed.get("seq"),
        )

    def diff(self, other: "ControllerState") -> Dict[str, Any]:
        """Return a dict of fields that changed between self and other."""
        changes: Dict[str, Any] = {}
        for name in BUTTON_NAMES:
            if self.buttons.get(name) != other.buttons.get(name):
                changes.setdefault("buttons", {})[name] = other.buttons.get(name)
        for stick in ("left_stick", "right_stick"):
            if self.__dict__[stick] != other.__dict__[stick]:
                changes[stick] = dict(other.__dict__[stick])
        if self.triggers != other.triggers:
            changes["triggers"] = dict(other.triggers)
        return changes

    def has_changed(self, other: "ControllerState") -> bool:
        return bool(self.diff(other))

    def summary(self) -> str:
        btn = "".join(k for k in BUTTON_NAMES if self.buttons.get(k)) or "-"
        return (
            f"btn={btn} L=({self.left_stick['x']:+.2f},{self.left_stick['y']:+.2f}) "
            f"R=({self.right_stick['x']:+.2f},{self.right_stick['y']:+.2f}) "
            f"LT={self.triggers['LT']:.2f} RT={self.triggers['RT']:.2f}"
        )
