"""End-to-end server integration test: real socket server, real client, real JSON."""
import json
import socket
import time

import pytest

from server import BridgeServer

HOST = "127.0.0.1"


def free_port():
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.bind((HOST, 0))
    port = s.getsockname()[1]
    s.close()
    return port


def payload(**overrides):
    p = {
        "type": "controller_input",
        "console": "XBOX_ONE",
        "buttons": {"A": True},
        "left_stick": {"x": 0.5, "y": -0.25},
        "right_stick": {"x": 0.0, "y": 0.0},
        "triggers": {"LT": 0.0, "RT": 0.9},
        "seq": 1,
    }
    p.update(overrides)
    return p


def recv_json(sock, timeout=5.0):
    sock.settimeout(timeout)
    buf = b""
    while b"\n" not in buf:
        chunk = sock.recv(4096)
        if not chunk:
            raise AssertionError("connection closed before newline")
        buf += chunk
    return json.loads(buf.split(b"\n", 1)[0])


@pytest.fixture()
def server():
    port = free_port()
    srv = BridgeServer(port=port)
    srv.start()
    time.sleep(0.1)
    yield srv
    srv.stop()


def connect(port):
    sock = socket.create_connection((HOST, port), timeout=5)
    return sock


def test_end_to_end_input_updates_state_and_acks(server):
    client = connect(server.port)
    try:
        client.sendall((json.dumps(payload()) + "\n").encode())
        ack = recv_json(client)
        assert ack == {"type": "ack", "received": True, "seq": 1}

        deadline = time.time() + 5
        while time.time() < deadline and server.current_state().seq != 1:
            time.sleep(0.02)
        st = server.current_state()
        assert st.buttons["A"] is True
        assert st.left_stick == {"x": 0.5, "y": -0.25}
        assert st.triggers["RT"] == 0.9
    finally:
        client.close()


def test_invalid_payload_returns_error_and_keeps_state(server):
    client = connect(server.port)
    try:
        bad = {"type": "controller_input", "buttons": {"NOPE": True}}
        client.sendall((json.dumps(bad) + "\n").encode())
        err = recv_json(client)
        assert err["type"] == "error"
        assert "unknown button" in err["message"]
        assert server.current_state().seq is None
    finally:
        client.close()


def test_multiple_messages_and_diff_callback(server):
    seen = []
    server.on_input = lambda cid, st, diff: seen.append(diff)
    client = connect(server.port)
    try:
        client.sendall((json.dumps(payload(seq=1)) + "\n").encode())
        assert recv_json(client)["seq"] == 1
        client.sendall((json.dumps(payload(seq=2, buttons={"B": True},
                                            triggers={"LT": 1.0, "RT": 0.0})) + "\n").encode())
        assert recv_json(client)["seq"] == 2
        deadline = time.time() + 5
        while time.time() < deadline and len(seen) < 2:
            time.sleep(0.02)
        assert len(seen) == 2
        assert seen[0]["buttons"] == {"A": True}
        assert seen[1]["buttons"] == {"A": False, "B": True}
        assert seen[1]["triggers"] == {"LT": 1.0, "RT": 0.0}
    finally:
        client.close()


def test_two_clients_tracked(server):
    c1 = connect(server.port)
    c2 = connect(server.port)
    try:
        c1.sendall((json.dumps(payload(seq=1)) + "\n").encode())
        c2.sendall((json.dumps(payload(seq=1)) + "\n").encode())
        recv_json(c1)
        recv_json(c2)
        deadline = time.time() + 5
        while time.time() < deadline and server.manager.client_count() < 2:
            time.sleep(0.02)
        assert server.manager.client_count() == 2
    finally:
        c1.close()
        c2.close()


def test_disconnect_cleans_up(server):
    client = connect(server.port)
    client.sendall((json.dumps(payload()) + "\n").encode())
    recv_json(client)
    client.close()
    deadline = time.time() + 5
    while time.time() < deadline and server.manager.client_count() > 0:
        time.sleep(0.02)
    assert server.manager.client_count() == 0


def test_ping_pong(server):
    client = connect(server.port)
    try:
        client.sendall(b'{"type":"ping","seq":9}\n')
        pong = recv_json(client)
        assert pong == {"type": "pong", "seq": 9}
    finally:
        client.close()


def test_status_dict(server):
    st = server.status()
    assert st["port"] == server.port
    assert "note" in st and "does NOT forward" in st["note"]
