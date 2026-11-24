package com.haikal.xboxcontroller.connection

/**
 * Connection lifecycle states exposed to the UI.
 */
sealed interface ConnectionStatus {
    /** Not connected, idle, no connection attempt running. */
    data object Disconnected : ConnectionStatus

    /** Actively trying to establish the TCP connection. */
    data object Connecting : ConnectionStatus

    /** TCP connection established and usable. */
    data class Connected(val host: String, val port: Int) : ConnectionStatus

    /** Waiting to retry after a failure (auto-reconnect backoff). */
    data class Retrying(val attempt: Int, val delayMs: Long) : ConnectionStatus

    /** Terminal failure after exhausting reconnect attempts. */
    data class Failed(val message: String) : ConnectionStatus
}
