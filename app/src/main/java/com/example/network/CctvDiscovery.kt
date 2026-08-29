package com.example.network

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import com.example.data.model.DiscoveredCamera
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap

class CctvDiscovery(private val context: Context) {
    private val TAG = "CctvDiscovery"
    private val BROADCAST_PORT = 8889

    private var broadcastJob: Job? = null
    private var listenJob: Job? = null
    private var isBroadcasting = false
    private var isListening = false

    private val discoveredMap = ConcurrentHashMap<String, DiscoveredCamera>()

    // Broadcast Camera Presence
    fun startBroadcasting(scope: CoroutineScope, cameraId: String, port: Int, deviceName: String = "CCTV Camera") {
        if (isBroadcasting) return
        isBroadcasting = true

        broadcastJob = scope.launch(Dispatchers.IO) {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket()
                socket.broadcast = true

                val message = "CCTV_BEACON|$cameraId|$port|$deviceName"
                val data = message.toByteArray()

                while (isActive && isBroadcasting) {
                    try {
                        val broadcastAddr = InetAddress.getByName("255.255.255.255")
                        val packet = DatagramPacket(data, data.size, broadcastAddr, BROADCAST_PORT)
                        socket.send(packet)
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        if (isBroadcasting) {
                            Log.w(TAG, "Failed to send beacon", e)
                        }
                    }
                    delay(2000)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Clean cancellation, do nothing
            } catch (e: Exception) {
                if (isBroadcasting) {
                    Log.e(TAG, "Error in broadcast", e)
                }
            } finally {
                socket?.close()
            }
        }
    }

    fun stopBroadcasting() {
        isBroadcasting = false
        broadcastJob?.cancel()
        broadcastJob = null
    }

    // Viewer Mode: Listen for nearby CCTV cameras
    fun startListening(scope: CoroutineScope, onCamerasUpdated: (List<DiscoveredCamera>) -> Unit) {
        if (isListening) return
        isListening = true

        // Acquire multicast lock if on Wi-Fi
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val lock = wifi?.createMulticastLock("CctvDiscoveryMulticastLock")?.apply {
            setReferenceCounted(true)
            acquire()
        }

        listenJob = scope.launch(Dispatchers.IO) {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket(BROADCAST_PORT)
                val buffer = ByteArray(1024)

                while (isActive && isListening) {
                    try {
                        val packet = DatagramPacket(buffer, buffer.size)
                        socket.receive(packet)
                        val text = String(packet.data, 0, packet.length).trim()

                        if (text.startsWith("CCTV_BEACON|")) {
                            val parts = text.split("|")
                            if (parts.size >= 3) {
                                val cameraId = parts[1]
                                val port = parts[2].toIntOrNull() ?: 8080
                                val deviceName = if (parts.size > 3) parts[3] else "CCTV Camera"
                                val host = packet.address.hostAddress ?: ""

                                val discovered = DiscoveredCamera(
                                    cameraId = cameraId,
                                    host = host,
                                    port = port,
                                    deviceName = deviceName,
                                    lastSeen = System.currentTimeMillis()
                                )
                                discoveredMap[cameraId] = discovered

                                // Clean stale (>10s)
                                val now = System.currentTimeMillis()
                                discoveredMap.entries.removeIf { now - it.value.lastSeen > 10000 }
                                onCamerasUpdated(discoveredMap.values.toList())
                            }
                        }
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        break
                    } catch (e: Exception) {
                        if (!isListening) break
                        Log.w(TAG, "Receive error in discovery listener", e)
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Clean shutdown
            } catch (e: Exception) {
                if (isListening) {
                    Log.e(TAG, "Error starting discovery socket", e)
                }
            } finally {
                socket?.close()
                try { lock?.release() } catch (_: Exception) {}
            }
        }
    }

    fun stopListening() {
        isListening = false
        listenJob?.cancel()
        listenJob = null
        discoveredMap.clear()
    }
}
