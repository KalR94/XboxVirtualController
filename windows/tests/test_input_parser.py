import pytest

from input_parser import ParseError, parse_payload, validate_message


def valid_payload(**overrides):
    p = {
        "type": "controller_input",
        "console": "XBOX_ONE",
        "buttons": {"A": True, "B": False, "LB": False, "RB": False,
                    "View": False, "Menu": False, "Guide": False, "L3": False, "R3": False},
        "left_stick": {"x": 0.0, "y": 0.0},
        "right_stick": {"x": 0.0, "y": 0.0},
        "triggers": {"LT": 0.0, "RT": 0.0},
    }
    p.update(overrides)
    return p


def test_valid_payload():
    out, seq = validate_message(valid_payload())
    assert out["type"] == "controller_input"
    assert out["buttons"]["A"] is True
    assert out["left_stick"] == {"x": 0.0, "y": 0.0}
    assert out["triggers"] == {"LT": 0.0, "RT": 0.0}


def test_missing_optional_fields_defaults():
    out, seq = validate_message({"type": "controller_input", "console": "XBOX_ONE"})
    assert out["left_stick"] == {"x": 0.0, "y": 0.0}
    assert out["triggers"] == {"LT": 0.0, "RT": 0.0}


def test_stick_clamp():
    out, _ = validate_message(valid_payload(left_stick={"x": 5, "y": -7}))
    assert out["left_stick"] == {"x": 1.0, "y": -1.0}


def test_trigger_clamp():
    out, _ = validate_message(valid_payload(triggers={"LT": 2.5, "RT": -1}))
    assert out["triggers"] == {"LT": 1.0, "RT": 0.0}


def test_reject_wrong_type():
    with pytest.raises(ParseError, match="type"):
        validate_message({"type": "mouse_input"})


def test_reject_not_object():
    with pytest.raises(ParseError, match="object"):
        validate_message([1, 2, 3])


def test_reject_bad_json_string():
    with pytest.raises(ParseError, match="invalid JSON"):
        parse_payload("{not json")


def test_reject_unknown_button():
    with pytest.raises(ParseError, match="unknown button"):
        validate_message(valid_payload(buttons={"A": True, "Z": True}))


def test_reject_non_bool_button():
    with pytest.raises(ParseError, match="boolean"):
        validate_message(valid_payload(buttons={"A": "yes"}))


def test_reject_non_numeric_stick():
    with pytest.raises(ParseError, match="number"):
        validate_message(valid_payload(left_stick={"x": "fast", "y": 0}))


def test_reject_nan():
    with pytest.raises(ParseError):
        validate_message(valid_payload(triggers={"LT": float("nan"), "RT": 0}))


def test_reject_unsupported_console():
    with pytest.raises(ParseError, match="console"):
        validate_message(valid_payload(console="PS5"))


def test_seq_passthrough():
    out, seq = validate_message(valid_payload(seq=42))
    assert seq == 42
