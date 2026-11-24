"""Connection manager: tracks Android clients, sends acks, cleans up dead clients."""
from __future__ import annotations

import socket
import threading
import time
from typing import Dict, List, Optional


class ClientConnection:
    def __init__(self, sock: socket.socket, addr):
        self.sock = sock
        self.addr = addr
        self.connected_at = time.time()
        self.last_message_at: Optional[float] = None
        self.messages_received = 0
        self.last_state: Optional[dict] = None
        self._send_lock = threading.Lock()
        self.alive = True

    def send_json(self, payload: dict) -> bool:
        import json
        try:
            data = (json.dumps(payload) + "\n").encode("utf-8")
            with self._send_lock:
                self.sock.sendall(data)
            return True
        except OSError:
            self.alive = False
            return False

    def close(self):
        self.alive = False
        try:
            self.sock.shutdown(socket.SHUT_RDWR)
        except OSError:
            pass
        try:
            self.sock.close()
        except OSError:
            pass


class ConnectionManager:
    def __init__(self):
        self._clients: Dict[int, ClientConnection] = {}
        self._lock = threading.Lock()
        self._next_id = 1
        self._seq_counters: Dict[int, int] = {}

    def add_client(self, sock: socket.socket, addr) -> int:
        client = ClientConnection(sock, addr)
        with self._lock:
            cid = self._next_id
            self._next_id += 1
            self._clients[cid] = client
            self._seq_counters[cid] = 0
        return cid

    def remove_client(self, cid: int):
        with self._lock:
            client = self._clients.pop(cid, None)
            self._seq_counters.pop(cid, None)
        if client:
            client.close()

    def get_client(self, cid: int) -> Optional[ClientConnection]:
        with self._lock:
            return self._clients.get(cid)

    def next_seq(self, cid: int) -> int:
        with self._lock:
            self._seq_counters[cid] = self._seq_counters.get(cid, 0) + 1
            return self._seq_counters[cid]

    def ack(self, cid: int, seq: Optional[int] = None) -> bool:
        client = self.get_client(cid)
        if not client:
            return False
        n = self.next_seq(cid)
        return client.send_json({"type": "ack", "received": True, "seq": seq if seq is not None else n})

    def mark_message(self, cid: int, state: Optional[dict] = None):
        client = self.get_client(cid)
        if client:
            client.last_message_at = time.time()
            client.messages_received += 1
            if state is not None:
                client.last_state = state

    def cleanup_dead(self):
        with self._lock:
            dead = [cid for cid, c in self._clients.items() if not c.alive]
            for cid in dead:
                self._clients.pop(cid, None)
                self._seq_counters.pop(cid, None)
        return dead

    def status(self) -> List[dict]:
        with self._lock:
            return [
                {
                    "id": cid,
                    "addr": f"{c.addr[0]}:{c.addr[1]}" if isinstance(c.addr, tuple) else str(c.addr),
                    "connected_at": c.connected_at,
                    "messages_received": c.messages_received,
                    "alive": c.alive,
                    "last_state": c.last_state,
                }
                for cid, c in self._clients.items()
            ]

    def client_count(self) -> int:
        with self._lock:
            return len(self._clients)
