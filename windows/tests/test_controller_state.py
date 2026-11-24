import time

from controller_state import ControllerState


def sample_state(**overrides):
    s = ControllerState()
    s.buttons["A"] = True
    s.left_stick = {"x": 0.5, "y": -0.5}
    s.triggers["RT"] = 0.8
    for k, v in overrides.items():
        setattr(s, k, v)
    return s


def test_defaults():
    s = ControllerState()
    assert all(v is False for v in s.buttons.values())
    assert s.left_stick == {"x": 0.0, "y": 0.0}
    assert s.triggers == {"LT": 0.0, "RT": 0.0}
    assert s.last_updated <= time.time()


def test_diff_buttons_and_triggers():
    a = ControllerState()
    b = sample_state()
    d = a.diff(b)
    assert d["buttons"] == {"A": True}
    assert d["triggers"]["RT"] == 0.8
    assert d["left_stick"] == {"x": 0.5, "y": -0.5}
    assert a.has_changed(b)


def test_no_change():
    a = sample_state()
    b = sample_state()
    assert a.diff(b) == {}
    assert not a.has_changed(b)


def test_update_from():
    base = ControllerState()
    payload = {
        "buttons": {"A": True},
        "left_stick": {"x": 1.0, "y": 0.0},
        "right_stick": {"x": 0.0, "y": 0.0},
        "triggers": {"LT": 0.0, "RT": 1.0},
    }
    s = base.update_from(payload, seq=7)
    assert s.buttons["A"] is True
    assert s.triggers["RT"] == 1.0
    assert s.seq == 7


def test_summary_string():
    s = sample_state()
    text = s.summary()
    assert "btn=A" in text and "RT=0.80" in text
