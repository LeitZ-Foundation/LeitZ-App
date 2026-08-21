package com.leitz.app.network

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

class WebSocketClient(private val serverUrl: String) {

    private var webSocket: WebSocket? = null
    private var isConnected = false
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5
    private val reconnectDelayMs = 3000L

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private var listener: Listener? = null

    interface Listener {
        fun onConnected()
        fun onDisconnected(code: Int, reason: String)
        fun onMessageReceived(message: String)
        fun onError(error: String)
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    fun connect() {
        if (isConnected) return

        val request = Request.Builder()
            .url(serverUrl)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected = true
                reconnectAttempts = 0
                listener?.onConnected()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                listener?.onMessageReceived(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                isConnected = false
                webSocket.close(code, reason)
                listener?.onDisconnected(code, reason)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isConnected = false
                listener?.onDisconnected(code, reason)
                scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                listener?.onError(t.message ?: "Unknown error")
                scheduleReconnect()
            }
        })
    }

    fun disconnect() {
        webSocket?.close(1000, "Client disconnected")
        webSocket = null
        isConnected = false
    }

    fun sendMessage(message: String): Boolean {
        return if (isConnected && webSocket != null) {
            webSocket?.send(message) ?: false
        } else {
            false
        }
    }

    private fun scheduleReconnect() {
        if (reconnectAttempts >= maxReconnectAttempts) {
            listener?.onError("Max reconnect attempts reached")
            return
        }
        reconnectAttempts++
        Thread.sleep(reconnectDelayMs)
        connect()
    }
}