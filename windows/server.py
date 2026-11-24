"""Xbox Virtual Controller Bridge — TCP server (port 8765).

Accepts newline-delimited JSON controller_input messages from an Android
client (via adb reverse tcp:8765 tcp:8765), validates them, updates
controller state, and sends a JSON ack per message.

IMPORTANT: This bridge does NOT forward input to a physical Xbox console.
It only receives, validates and displays input (V3 research bridge).
"""
from __future__ import annotations

import json
import socket
import threading
import time
from typing import Callable, Optional

from connection_manager import ConnectionManager
from controller_state import ControllerState
from input_parser import ParseError, parse_payload

DEFAULT_HOST = "127.0.0.1"
DEFAULT_PORT = 8765


class BridgeServer:
    def __init__(self, host: str = DEFAULT_HOST, port: int = DEFAULT_PORT):
        self.host = host
        self.port = port
        self.manager = ConnectionManager()
        self.state = ControllerState()
        self._state_lock = threading.Lock()
        self._stop = threading.Event()
        self._server_sock: Optional[socket.socket] = None
        self._threads: list = []
        # callbacks: on_input(client_id, state, diff), on_error(client_id, message), on_connect(client_id), on_disconnect(client_id)
        self.on_input: Optional[Callable] = None
        self.on_error: Optional[Callable] = None
        self.on_connect: Optional[Callable] = None
        self.on_disconnect: Optional[Callable] = None

    # ---------- lifecycle ----------
    def start(self):
        self._server_sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self._server_sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self._server_sock.bind((self.host, self.port))
        self._server_sock.listen(5)
        self._server_sock.settimeout(0.5)
        t = threading.Thread(target=self._accept_loop, daemon=True, name="bridge-accept")
        t.start()
        self._threads.append(t)

    def stop(self):
        self._stop.set()
        if self._server_sock:
            try:
                self._server_sock.close()
            except OSError:
                pass
        for t in self._threads:
            t.join(timeout=2)

    # ---------- internals ----------
    def _accept_loop(self):
        while not self._stop.is_set():
            try:
                sock, addr = self._server_sock.accept()
            except socket.timeout:
                continue
            except OSError:
                break
            cid = self.manager.add_client(sock, addr)
            if self.on_connect:
                self.on_connect(cid)
            t = threading.Thread(target=self._client_loop, args=(cid,), daemon=True, name=f"bridge-client-{cid}")
            t.start()
            self._threads.append(t)

    def _client_loop(self, cid: int):
        client = self.manager.get_client(cid)
        if not client:
            return
        buf = b""
        try:
            while not self._stop.is_set():
                try:
                    data = client.sock.recv(4096)
                except OSError:
                    break
                if not data:
                    break
                buf += data
                while b"\n" in buf:
                    line, buf = buf.split(b"\n", 1)
                    if not line.strip():
                        continue
                    self._handle_line(cid, line.decode("utf-8", errors="replace"))
        finally:
            self.manager.remove_client(cid)
            if self.on_disconnect:
                self.on_disconnect(cid)

    def _handle_line(self, cid: int, line: str):
        try:
            payload, seq = parse_payload(line)
        except ParseError as e:
            if self.on_error:
                self.on_error(cid, str(e))
            client = self.manager.get_client(cid)
            if client:
                client.send_json({"type": "error", "message": str(e)})
            return

        if payload.get("type") == "ping":
            client = self.manager.get_client(cid)
            if client:
                client.send_json({"type": "pong", "seq": seq})
            return

        new_state = ControllerState().update_from(payload, seq)
        with self._state_lock:
            old = self.state
            self.state = new_state
            diff = old.diff(new_state)
        self.manager.mark_message(cid, new_state.summary())
        self.manager.ack(cid, seq)
        if self.on_input:
            self.on_input(cid, new_state, diff)

    # ---------- helpers ----------
    def current_state(self) -> ControllerState:
        with self._state_lock:
            return self.state

    def status(self) -> dict:
        return {
            "host": self.host,
            "port": self.port,
            "clients": self.manager.status(),
            "state": self.current_state().summary(),
            "note": "This bridge does NOT forward input to a physical Xbox console.",
        }


def main():
    server = BridgeServer()
    from monitor import Monitor

    monitor = Monitor(server)
    server.on_input = monitor.on_input
    server.on_error = monitor.on_error
    server.on_connect = monitor.on_connect
    server.on_disconnect = monitor.on_disconnect
    server.start()
    monitor.log(f"Bridge listening on {server.host}:{server.port} — waiting for Android clients")
    monitor.log("No physical Xbox console integration: input is received and displayed only.")
    try:
        monitor.run()
    except KeyboardInterrupt:
        pass
    finally:
        server.stop()
        monitor.log("Bridge stopped.")


if __name__ == "__main__":
    main()
