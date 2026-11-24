"""Connection manager tests over a REAL loopback TCP socket pair."""
import json
import socket
import threading
import time

import pytest

from connection_manager import ConnectionManager


def make_pair():
    srv = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    srv.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    srv.bind(("127.0.0.1", 0))
    srv.listen(1)
    port = srv.getsockname()[1]
    client = socket.create_connection(("127.0.0.1", port), timeout=5)
    conn, addr = srv.accept()
    srv.close()
    return conn, client


def recv_json(sock, timeout=5.0):
    sock.settimeout(timeout)
    buf = b""
    while b"\n" not in buf:
        chunk = sock.recv(4096)
        if not chunk:
            break
        buf += chunk
    line = buf.split(b"\n", 1)[0]
    return json.loads(line)


def test_add_and_status():
    conn, client = make_pair()
    mgr = ConnectionManager()
    cid = mgr.add_client(conn, ("127.0.0.1", 5555))
    assert mgr.client_count() == 1
    st = mgr.status()
    assert st[0]["id"] == cid
    assert st[0]["alive"] is True
    mgr.remove_client(cid)
    assert mgr.client_count() == 0
    client.close()


def test_ack_over_real_socket():
    conn, client = make_pair()
    mgr = ConnectionManager()
    cid = mgr.add_client(conn, client.getpeername())
    try:
        assert mgr.ack(cid, seq=3) is True
        msg = recv_json(client)
        assert msg == {"type": "ack", "received": True, "seq": 3}
    finally:
        mgr.remove_client(cid)
        client.close()


def test_ack_autoincrement_seq():
    conn, client = make_pair()
    mgr = ConnectionManager()
    cid = mgr.add_client(conn, client.getpeername())
    try:
        mgr.ack(cid)
        assert recv_json(client)["seq"] == 1
        mgr.ack(cid)
        assert recv_json(client)["seq"] == 2
    finally:
        mgr.remove_client(cid)
        client.close()


def test_mark_message_updates_counters():
    conn, client = make_pair()
    mgr = ConnectionManager()
    cid = mgr.add_client(conn, client.getpeername())
    try:
        mgr.mark_message(cid, "btn=A")
        st = mgr.status()[0]
        assert st["messages_received"] == 1
        assert st["last_state"] == "btn=A"
        assert mgr.get_client(cid).last_message_at is not None
    finally:
        mgr.remove_client(cid)
        client.close()


def test_cleanup_dead_clients():
    conn, client = make_pair()
    mgr = ConnectionManager()
    cid = mgr.add_client(conn, client.getpeername())
    client.close()
    time.sleep(0.1)
    # force dead flag (as a failed send would)
    mgr.get_client(cid).alive = False
    dead = mgr.cleanup_dead()
    assert cid in dead
    assert mgr.client_count() == 0


def test_ack_for_unknown_client_returns_false():
    mgr = ConnectionManager()
    assert mgr.ack(999, seq=1) is False
