"""Parse & validate Android controller_input JSON payloads.

Expected shape:
{"type":"controller_input","console":"XBOX_ONE",
 "buttons":{"A":true,...,"LB","RB","View","Menu","Guide","L3","R3"},
 "left_stick":{"x":..,"y":..},"right_stick":{"x":..,"y":..},
 "triggers":{"LT":..,"RT":..}}

Invalid payloads are rejected with a clear error message.
Sticks are clamped to -1..1, triggers to 0..1.
"""
from __future__ import annotations

import json
import math
from typing import Any, Dict, Optional, Tuple


class ParseError(ValueError):
    """Raised when a payload is not a valid controller_input message."""


BUTTON_NAMES = ("A", "B", "X", "Y", "LB", "RB", "View", "Menu", "Guide", "L3", "R3")


def _clamp(value: float, low: float, high: float) -> float:
    return max(low, min(high, value))


def _num(value: Any, name: str) -> float:
    if isinstance(value, bool) or not isinstance(value, (int, float)):
        raise ParseError(f"{name} must be a number, got {value!r}")
    v = float(value)
    if math.isnan(v) or math.isinf(v):
        raise ParseError(f"{name} must be a finite number, got {value!r}")
    return v


def parse_payload(raw: str) -> Tuple[Dict[str, Any], Optional[int]]:
    """Parse a newline-delimited JSON string.

    Returns (validated_dict, seq). Raises ParseError with a clear message.
    """
    try:
        data = json.loads(raw)
    except json.JSONDecodeError as e:
        raise ParseError(f"invalid JSON: {e}") from None
    return validate_message(data)


def validate_message(data: Any) -> Tuple[Dict[str, Any], Optional[int]]:
    if not isinstance(data, dict):
        raise ParseError(f"payload must be a JSON object, got {type(data).__name__}")

    seq = data.get("seq")
    if seq is not None and (isinstance(seq, bool) or not isinstance(seq, int)):
        raise ParseError(f"seq must be an integer, got {seq!r}")

    msg_type = data.get("type")
    if msg_type == "ping":
        return {"type": "ping"}, seq
    if msg_type != "controller_input":
        raise ParseError(f"type must be 'controller_input', got {msg_type!r}")

    console = data.get("console", "XBOX_ONE")
    if console != "XBOX_ONE":
        raise ParseError(f"unsupported console {console!r}, expected 'XBOX_ONE'")

    # buttons
    raw_buttons = data.get("buttons", {})
    if not isinstance(raw_buttons, dict):
        raise ParseError("buttons must be a JSON object")
    buttons: Dict[str, bool] = {}
    for name, value in raw_buttons.items():
        if name not in BUTTON_NAMES:
            raise ParseError(f"unknown button {name!r}; valid: {BUTTON_NAMES}")
        if not isinstance(value, bool):
            raise ParseError(f"button {name} must be a boolean, got {value!r}")
        buttons[name] = value

    # sticks
    sticks: Dict[str, Dict[str, float]] = {}
    for key in ("left_stick", "right_stick"):
        raw = data.get(key, {"x": 0.0, "y": 0.0})
        if not isinstance(raw, dict):
            raise ParseError(f"{key} must be a JSON object")
        x = _clamp(_num(raw.get("x", 0.0), f"{key}.x"), -1.0, 1.0)
        y = _clamp(_num(raw.get("y", 0.0), f"{key}.y"), -1.0, 1.0)
        sticks[key] = {"x": x, "y": y}

    # triggers
    raw_triggers = data.get("triggers", {"LT": 0.0, "RT": 0.0})
    if not isinstance(raw_triggers, dict):
        raise ParseError("triggers must be a JSON object")
    triggers: Dict[str, float] = {}
    for t in ("LT", "RT"):
        triggers[t] = _clamp(_num(raw_triggers.get(t, 0.0), f"triggers.{t}"), 0.0, 1.0)

    return {
        "type": "controller_input",
        "console": console,
        "buttons": buttons,
        "left_stick": sticks["left_stick"],
        "right_stick": sticks["right_stick"],
        "triggers": triggers,
    }, seq
