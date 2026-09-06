package com.setu.app.bluetooth

/**
 * Callback interface for Bluetooth connection events.
 * All methods are called on the MAIN thread — safe to update UI directly.
 */
interface ConnectionListener {

    /** Called when a Bluetooth socket is successfully established (either as Host or Joiner). */
    fun onConnected(deviceName: String)

    /**
     * Called every time a complete message arrives from the remote device.
     * @param raw The raw wire string (e.g. "TXT:Hello" or "SYS:LANG:hi")
     */
    fun onMessageReceived(raw: String)

    /** Called when the socket drops unexpectedly (remote disconnected, or IO error). */
    fun onConnectionLost()

    /**
     * Called when an outbound connection attempt fails before a socket is established.
     * @param reason Human-readable error description for debugging.
     */
    fun onConnectionFailed(reason: String)
}
