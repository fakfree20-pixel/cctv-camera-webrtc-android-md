package com.example.network

import android.content.Context
import android.util.Log
import com.example.data.model.CameraTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.CopyOnWriteArrayList

class CctvHttpServer(
    private val context: Context,
    private val port: Int = 8080,
    private val onCommandReceived: (action: String) -> String,
    private val onAudioReceived: (audioData: ByteArray) -> Unit,
    private val getTelemetry: () -> CameraTelemetry,
    private val getLatestJpeg: () -> ByteArray?
) {
    private val TAG = "CctvHttpServer"

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private var isRunning = false

    private val connectedStreamSockets = CopyOnWriteArrayList<Socket>()
    private val connectedAudioSockets = CopyOnWriteArrayList<Socket>()

    var onClientCountChanged: ((Int) -> Unit)? = null

    fun start(scope: CoroutineScope): Int {
        if (isRunning) return serverSocket?.localPort ?: port

        var currentPort = port
        var socket: ServerSocket? = null

        // Try ports starting from 8080
        for (p in port..port + 10) {
            try {
                socket = ServerSocket(p)
                currentPort = p
                break
            } catch (e: Exception) {
                Log.w(TAG, "Port $p busy, trying next...")
            }
        }

        serverSocket = socket ?: throw IllegalStateException("Cannot bind to any port")
        isRunning = true

        serverJob = scope.launch(Dispatchers.IO) {
            Log.d(TAG, "CCTV Server started on port $currentPort")
            while (isActive && isRunning) {
                try {
                    val clientSocket = serverSocket?.accept() ?: break
                    launch(Dispatchers.IO) {
                        handleClient(clientSocket)
                    }
                } catch (e: SocketException) {
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "Error accepting client", e)
                }
            }
        }

        return currentPort
    }

    private fun handleClient(socket: Socket) {
        try {
            val input = socket.getInputStream()
            val reader = BufferedReader(InputStreamReader(input))
            val firstLine = reader.readLine() ?: return

            val parts = firstLine.split(" ")
            if (parts.size < 2) return

            val method = parts[0]
            val fullPath = parts[1]
            val path = fullPath.substringBefore("?")
            val query = if (fullPath.contains("?")) fullPath.substringAfter("?") else ""

            // Parse headers
            val headers = mutableMapOf<String, String>()
            var line: String?
            var contentLength = 0
            while (reader.readLine().also { line = it } != null) {
                if (line.isNullOrBlank()) break
                val colonIdx = line!!.indexOf(":")
                if (colonIdx > 0) {
                    val key = line!!.substring(0, colonIdx).trim().lowercase()
                    val value = line!!.substring(colonIdx + 1).trim()
                    headers[key] = value
                    if (key == "content-length") {
                        contentLength = value.toIntOrNull() ?: 0
                    }
                }
            }

            val out = socket.getOutputStream()

            when (path) {
                "/stream", "/video" -> {
                    serveMjpegStream(socket, out)
                }
                "/audio" -> {
                    serveAudioStream(socket, out)
                }
                "/control" -> {
                    handleControlEndpoint(query, out)
                    socket.close()
                }
                "/telemetry" -> {
                    serveTelemetry(out)
                    socket.close()
                }
                "/snapshot" -> {
                    serveSnapshot(out)
                    socket.close()
                }
                "/talk" -> {
                    handleTwoWayTalk(input, contentLength, out)
                    socket.close()
                }
                else -> {
                    serveWebClient(out)
                    socket.close()
                }
            }
        } catch (e: Exception) {
            try { socket.close() } catch (_: Exception) {}
        }
    }

    private fun serveMjpegStream(socket: Socket, out: OutputStream) {
        val boundary = "cctv_frame_boundary"
        val header = "HTTP/1.1 200 OK\r\n" +
                "Connection: close\r\n" +
                "Server: RemoteCCTV/1.0\r\n" +
                "Cache-Control: no-store, no-cache, must-revalidate, pre-check=0, post-check=0, max-age=0\r\n" +
                "Pragma: no-cache\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Content-Type: multipart/x-mixed-replace; boundary=--$boundary\r\n\r\n"

        out.write(header.toByteArray())
        out.flush()

        connectedStreamSockets.add(socket)
        notifyClientCount()

        // Write first available frame immediately
        getLatestJpeg()?.let { jpeg ->
            sendMjpegFrame(out, boundary, jpeg)
        }
    }

    fun broadcastJpegFrame(jpeg: ByteArray) {
        val boundary = "cctv_frame_boundary"
        val deadSockets = mutableListOf<Socket>()

        for (socket in connectedStreamSockets) {
            try {
                if (socket.isClosed || !socket.isConnected) {
                    deadSockets.add(socket)
                    continue
                }
                sendMjpegFrame(socket.getOutputStream(), boundary, jpeg)
            } catch (e: Exception) {
                deadSockets.add(socket)
            }
        }

        if (deadSockets.isNotEmpty()) {
            connectedStreamSockets.removeAll(deadSockets)
            for (s in deadSockets) {
                try { s.close() } catch (_: Exception) {}
            }
            notifyClientCount()
        }
    }

    private fun sendMjpegFrame(out: OutputStream, boundary: String, jpeg: ByteArray) {
        val frameHeader = "--$boundary\r\n" +
                "Content-Type: image/jpeg\r\n" +
                "Content-Length: ${jpeg.size}\r\n\r\n"
        out.write(frameHeader.toByteArray())
        out.write(jpeg)
        out.write("\r\n".toByteArray())
        out.flush()
    }

    private fun serveAudioStream(socket: Socket, out: OutputStream) {
        val header = "HTTP/1.1 200 OK\r\n" +
                "Connection: close\r\n" +
                "Server: RemoteCCTV/1.0\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Content-Type: audio/l16; rate=16000; channels=1\r\n\r\n"
        out.write(header.toByteArray())
        out.flush()
        connectedAudioSockets.add(socket)
    }

    fun broadcastAudioPacket(pcmPacket: ByteArray) {
        val deadSockets = mutableListOf<Socket>()
        for (socket in connectedAudioSockets) {
            try {
                if (socket.isClosed || !socket.isConnected) {
                    deadSockets.add(socket)
                    continue
                }
                val out = socket.getOutputStream()
                out.write(pcmPacket)
                out.flush()
            } catch (e: Exception) {
                deadSockets.add(socket)
            }
        }
        if (deadSockets.isNotEmpty()) {
            connectedAudioSockets.removeAll(deadSockets)
            for (s in deadSockets) {
                try { s.close() } catch (_: Exception) {}
            }
        }
    }

    private fun handleControlEndpoint(query: String, out: OutputStream) {
        val action = query.split("&")
            .firstOrNull { it.startsWith("action=") }
            ?.substringAfter("action=") ?: ""

        val result = onCommandReceived(action)
        val json = JSONObject().apply {
            put("status", "ok")
            put("action", action)
            put("result", result)
        }.toString()

        val response = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/json\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Content-Length: ${json.toByteArray().size}\r\n\r\n" + json
        out.write(response.toByteArray())
        out.flush()
    }

    private fun serveTelemetry(out: OutputStream) {
        val t = getTelemetry()
        val json = JSONObject().apply {
            put("cameraId", t.cameraId)
            put("ipAddress", t.ipAddress)
            put("port", t.port)
            put("lens", t.lens.name)
            put("isTorchOn", t.isTorchOn)
            put("isMicEnabled", t.isMicEnabled)
            put("isSirenPlaying", t.isSirenPlaying)
            put("batteryLevel", t.batteryLevel)
            put("isCharging", t.isCharging)
            put("motionDetected", t.motionDetected)
            put("motionCount", t.motionCount)
            put("connectedClients", connectedStreamSockets.size)
            put("fps", t.fps)
            put("timestamp", System.currentTimeMillis())
        }.toString()

        val response = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/json\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Content-Length: ${json.toByteArray().size}\r\n\r\n" + json
        out.write(response.toByteArray())
        out.flush()
    }

    private fun serveSnapshot(out: OutputStream) {
        val jpeg = getLatestJpeg()
        if (jpeg != null) {
            val response = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: image/jpeg\r\n" +
                    "Access-Control-Allow-Origin: *\r\n" +
                    "Content-Length: ${jpeg.size}\r\n\r\n"
            out.write(response.toByteArray())
            out.write(jpeg)
        } else {
            val err = "HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\n\r\n"
            out.write(err.toByteArray())
        }
        out.flush()
    }

    private fun handleTwoWayTalk(input: InputStream, contentLength: Int, out: OutputStream) {
        if (contentLength > 0) {
            val buffer = ByteArray(contentLength)
            var bytesRead = 0
            while (bytesRead < contentLength) {
                val read = input.read(buffer, bytesRead, contentLength - bytesRead)
                if (read == -1) break
                bytesRead += read
            }
            if (bytesRead > 0) {
                onAudioReceived(buffer.copyOf(bytesRead))
            }
        }
        val response = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nAccess-Control-Allow-Origin: *\r\nContent-Length: 2\r\n\r\nOK"
        out.write(response.toByteArray())
        out.flush()
    }

    private fun serveWebClient(out: OutputStream) {
        val html = """
            <!DOCTYPE html>
            <html lang="hi">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>Remote CCTV Live Stream</title>
              <style>
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; text-align: center; background: #0d1117; color: #fff; margin: 0; padding: 16px; }
                h2 { color: #00e5ff; margin-bottom: 8px; }
                .stream-card { background: #161b22; border: 1px solid #30363d; border-radius: 12px; padding: 12px; max-width: 520px; margin: 0 auto; }
                img.stream { width: 100%; border-radius: 8px; background: #000; display: block; }
                .controls { margin-top: 14px; display: flex; justify-content: center; gap: 8px; flex-wrap: wrap; }
                button { padding: 10px 14px; font-size: 14px; font-weight: bold; border-radius: 8px; border: none; background: #2563eb; color: #fff; cursor: pointer; transition: 0.2s; }
                button:active { transform: scale(0.96); }
                button.red { background: #ef4444; }
                button.teal { background: #0d9488; }
                .status-badge { display: inline-block; padding: 4px 10px; border-radius: 20px; background: #1f2937; color: #10b981; font-size: 13px; margin-top: 8px; }
              </style>
            </head>
            <body>
              <h2>📱 Remote CCTV Stream</h2>
              <div class="stream-card">
                <img class="stream" src="/stream" alt="Live CCTV Stream">
                <div class="status-badge" id="statusBadge">● LIVE CCTV FEED</div>
                <div class="controls">
                  <button onclick="sendCommand('SWITCH_CAMERA')">🔄 Switch Camera</button>
                  <button class="teal" onclick="sendCommand('TOGGLE_TORCH')">🔦 Torch</button>
                  <button class="red" onclick="sendCommand('TRIGGER_SIREN')">🚨 Siren Alarm</button>
                  <button onclick="sendCommand('STOP_SIREN')">🔇 Stop Siren</button>
                </div>
              </div>
              <script>
                function sendCommand(cmd) {
                  fetch('/control?action=' + cmd).then(r => r.json()).then(d => console.log('Command executed:', d));
                }
              </script>
            </body>
            </html>
        """.trimIndent()

        val response = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html; charset=UTF-8\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Content-Length: ${html.toByteArray().size}\r\n\r\n" + html
        out.write(response.toByteArray())
        out.flush()
    }

    private fun notifyClientCount() {
        onClientCountChanged?.invoke(connectedStreamSockets.size)
    }

    fun stop() {
        isRunning = false
        serverJob?.cancel()
        serverJob = null
        for (s in connectedStreamSockets) {
            try { s.close() } catch (_: Exception) {}
        }
        connectedStreamSockets.clear()
        for (s in connectedAudioSockets) {
            try { s.close() } catch (_: Exception) {}
        }
        connectedAudioSockets.clear()
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing server socket", e)
        }
        serverSocket = null
        notifyClientCount()
    }

    fun getConnectedClientsCount(): Int = connectedStreamSockets.size

    companion object {
        fun getLocalIpAddress(): String {
            try {
                val interfaces = NetworkInterface.getNetworkInterfaces()
                while (interfaces.hasMoreElements()) {
                    val intf = interfaces.nextElement()
                    val addresses = intf.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val addr = addresses.nextElement()
                        if (!addr.isLoopbackAddress && addr is Inet4Address) {
                            return addr.hostAddress ?: "127.0.0.1"
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("CctvHttpServer", "Error getting IP", e)
            }
            return "127.0.0.1"
        }
    }
}
