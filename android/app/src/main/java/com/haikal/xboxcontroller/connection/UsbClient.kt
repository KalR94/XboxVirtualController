package com.haikal.xboxcontroller.connection

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Raw TCP line-based client used by [ConnectionManager].
 * Sends newline-terminated JSON frames to the bridge (USB-to-Windows
 * bridge reachable via `adb reverse tcp:8765 tcp:8765`, so localhost:8765
 * on the device maps to the bridge on the host).
 */
class UsbClient(
    private val host: String,
    private val port: Int,
    private val connectTimeoutMs: Int = 3_000,
    private val sendTimeoutMs: Int = 1_000,
) {
    private var socket: Socket? = null
    private var writer: BufferedWriter? = null

    val isConnected: Boolean
        get() = socket?.isConnected == true && socket?.isClosed == false

    @Throws(Exception::class)
    suspend fun connect() = withContext(Dispatchers.IO) {
        close()
        val s = Socket()
        try {
            s.tcpNoDelay = true
            s.soTimeout = sendTimeoutMs
            s.connect(InetSocketAddress(host, port), connectTimeoutMs)
            writer = BufferedWriter(OutputStreamWriter(s.getOutputStream(), Charsets.UTF_8))
            socket = s
        } catch (e: Exception) {
            runCatching { s.close() }
            throw e
        }
    }

    @Throws(Exception::class)
    suspend fun sendLine(line: String) = withContext(Dispatchers.IO) {
        val w = writer ?: throw IllegalStateException("UsbClient not connected")
        w.write(line)
        w.write("\n")
        w.flush()
    }

    suspend fun close() = withContext(Dispatchers.IO) {
        runCatching { writer?.close() }
        runCatching { socket?.close() }
        writer = null
        socket = null
    }
}
