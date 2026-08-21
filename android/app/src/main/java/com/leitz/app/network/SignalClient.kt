package com.leitz.app.network

import com.leitz.app.util.Constants
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import org.json.JSONObject

class SignalClient(
    private val webSocketClient: WebSocketClient
) {
    private val _signalingEvents = MutableSharedFlow<SignalEvent>()
    val signalingEvents: SharedFlow<SignalEvent> = _signalingEvents

    init {
        webSocketClient.setListener(object : WebSocketClient.Listener {
            override fun onConnected() {
                // Could emit connected event
            }

            override fun onDisconnected(code: Int, reason: String) {
                // Handle disconnect
            }

            override fun onMessageReceived(message: String) {
                handleIncomingMessage(message)
            }

            override fun onError(error: String) {
                // Emit error event
            }
        })
    }

    fun connect() {
        webSocketClient.connect()
    }

    fun disconnect() {
        webSocketClient.disconnect()
    }

    fun sendLogin(userId: String, token: String) {
        val json = JSONObject().apply {
            put("type", "login")
            put("userId", userId)
            put("token", token)
        }
        webSocketClient.sendMessage(json.toString())
    }

    fun sendOffer(toUserId: String, sdp: String) {
        val json = JSONObject().apply {
            put("type", "offer")
            put("to", toUserId)
            put("sdp", sdp)
        }
        webSocketClient.sendMessage(json.toString())
    }

    fun sendAnswer(toUserId: String, sdp: String) {
        val json = JSONObject().apply {
            put("type", "answer")
            put("to", toUserId)
            put("sdp", sdp)
        }
        webSocketClient.sendMessage(json.toString())
    }

    fun sendIceCandidate(toUserId: String, candidate: String, sdpMid: String?, sdpMLineIndex: Int?) {
        val json = JSONObject().apply {
            put("type", "ice")
            put("to", toUserId)
            put("candidate", candidate)
            put("sdpMid", sdpMid ?: "")
            put("sdpMLineIndex", sdpMLineIndex ?: -1)
        }
        webSocketClient.sendMessage(json.toString())
    }

    fun sendPresence(status: String) {
        val json = JSONObject().apply {
            put("type", "presence")
            put("status", status)
        }
        webSocketClient.sendMessage(json.toString())
    }

    private fun handleIncomingMessage(message: String) {
        try {
            val json = JSONObject(message)
            val type = json.optString("type")
            when (type) {
                "offer" -> {
                    val from = json.optString("from")
                    val sdp = json.optString("sdp")
                    _signalingEvents.tryEmit(SignalEvent.OfferReceived(from, sdp))
                }
                "answer" -> {
                    val from = json.optString("from")
                    val sdp = json.optString("sdp")
                    _signalingEvents.tryEmit(SignalEvent.AnswerReceived(from, sdp))
                }
                "ice" -> {
                    val from = json.optString("from")
                    val candidate = json.optString("candidate")
                    val sdpMid = json.optString("sdpMid").takeIf { it.isNotEmpty() }
                    val sdpMLineIndex = json.optInt("sdpMLineIndex", -1).takeIf { it >= 0 }
                    _signalingEvents.tryEmit(SignalEvent.IceCandidateReceived(from, candidate, sdpMid, sdpMLineIndex))
                }
                "presence" -> {
                    val userId = json.optString("userId")
                    val status = json.optString("status")
                    _signalingEvents.tryEmit(SignalEvent.PresenceUpdated(userId, status))
                }
                else -> {
                    // Unknown type, ignore or log
                }
            }
        } catch (e: Exception) {
            // Handle malformed JSON
        }
    }
}

sealed class SignalEvent {
    data class OfferReceived(val fromUserId: String, val sdp: String) : SignalEvent()
    data class AnswerReceived(val fromUserId: String, val sdp: String) : SignalEvent()
    data class IceCandidateReceived(
        val fromUserId: String,
        val candidate: String,
        val sdpMid: String?,
        val sdpMLineIndex: Int?
    ) : SignalEvent()
    data class PresenceUpdated(val userId: String, val status: String) : SignalEvent()
    // Add other events as needed
}