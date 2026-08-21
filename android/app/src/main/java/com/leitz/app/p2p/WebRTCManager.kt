package com.leitz.app.p2p

import android.content.Context
import com.leitz.app.network.SignalClient
import com.leitz.app.network.SignalEvent
import com.leitz.app.network.StunTurnConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.webrtc.*
import java.util.concurrent.ConcurrentHashMap

class WebRTCManager(
    private val context: Context,
    private val signalClient: SignalClient
) {
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var eglBase: EglBase? = null

    private val peerConnections = ConcurrentHashMap<String, PeerConnectionManager>()
    private val iceCandidatesQueue = ConcurrentHashMap<String, MutableList<IceCandidate>>()

    private var listener: Listener? = null

    interface Listener {
        fun onPeerConnected(userId: String)
        fun onPeerDisconnected(userId: String)
        fun onLocalIceCandidate(userId: String, candidate: IceCandidate)
        fun onRemoteStreamAdded(userId: String, stream: MediaStream)
        fun onError(userId: String, error: String)
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    fun initialize() {
        if (peerConnectionFactory != null) return

        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(false)
                .createInitializationOptions()
        )

        eglBase = EglBase.create()
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase!!.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase!!.eglBaseContext))
            .createPeerConnectionFactory()

        observeSignaling()
    }

    fun createPeerConnection(userId: String) {
        if (peerConnections.containsKey(userId)) return

        val rtcConfig = PeerConnection.RTCConfiguration(
            StunTurnConfig.default().let { config ->
                config.stunUrls.map { PeerConnection.IceServer.builder(it).createIceServer() } +
                config.turnUrls.map { PeerConnection.IceServer.builder(it)
                    .setUsername(config.turnUsername)
                    .setPassword(config.turnPassword)
                    .createIceServer() }
            }
        ).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            keyType = PeerConnection.KeyType.ECDSA
            tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.ENABLED
        }

        val observer = object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) {
                listener?.onLocalIceCandidate(userId, candidate)
                signalClient.sendIceCandidate(
                    toUserId = userId,
                    candidate = candidate.sdp,
                    sdpMid = candidate.sdpMid,
                    sdpMLineIndex = candidate.sdpMLineIndex
                )
            }

            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}

            override fun onSignalingChange(state: PeerConnection.SignalingState?) {}

            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                when (state) {
                    PeerConnection.IceConnectionState.CONNECTED,
                    PeerConnection.IceConnectionState.COMPLETED -> {
                        listener?.onPeerConnected(userId)
                    }
                    PeerConnection.IceConnectionState.DISCONNECTED,
                    PeerConnection.IceConnectionState.FAILED,
                    PeerConnection.IceConnectionState.CLOSED -> {
                        listener?.onPeerDisconnected(userId)
                    }
                    else -> {}
                }
            }

            override fun onIceConnectionReceivingChange(receiving: Boolean) {}

            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}

            override fun onAddStream(stream: MediaStream?) {
                stream?.let { listener?.onRemoteStreamAdded(userId, it) }
            }

            override fun onRemoveStream(stream: MediaStream?) {}

            override fun onDataChannel(channel: DataChannel?) {}

            override fun onRenegotiationNeeded() {}

            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {}
        }

        val peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, observer)
        if (peerConnection != null) {
            val manager = PeerConnectionManager(userId, peerConnection)
            peerConnections[userId] = manager

            // Add any queued ICE candidates
            iceCandidatesQueue.remove(userId)?.forEach { candidate ->
                manager.addIceCandidate(candidate)
            }
        } else {
            listener?.onError(userId, "Failed to create PeerConnection")
        }
    }

    fun createOffer(userId: String) {
        val manager = peerConnections[userId] ?: return
        manager.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                sdp?.let {
                    manager.setLocalDescription(this, it)
                    signalClient.sendOffer(userId, it.description)
                }
            }

            override fun onSetSuccess() {}

            override fun onCreateFailure(error: String?) {
                listener?.onError(userId, "Offer creation failed: $error")
            }

            override fun onSetFailure(error: String?) {
                listener?.onError(userId, "Set local description failed: $error")
            }
        })
    }

    fun createAnswer(userId: String) {
        val manager = peerConnections[userId] ?: return
        manager.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                sdp?.let {
                    manager.setLocalDescription(this, it)
                    signalClient.sendAnswer(userId, it.description)
                }
            }

            override fun onSetSuccess() {}

            override fun onCreateFailure(error: String?) {
                listener?.onError(userId, "Answer creation failed: $error")
            }

            override fun onSetFailure(error: String?) {
                listener?.onError(userId, "Set local description failed: $error")
            }
        })
    }

    fun setRemoteDescription(userId: String, sdp: String, isOffer: Boolean) {
        val manager = peerConnections[userId] ?: return
        val type = if (isOffer) SessionDescription.Type.OFFER else SessionDescription.Type.ANSWER
        val sessionDescription = SessionDescription(type, sdp)
        manager.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {}

            override fun onSetSuccess() {
                // If we received an offer, we should create an answer
                if (isOffer) {
                    createAnswer(userId)
                }
            }

            override fun onCreateFailure(error: String?) {
                listener?.onError(userId, "Set remote description failed: $error")
            }

            override fun onSetFailure(error: String?) {
                listener?.onError(userId, "Set remote description failed: $error")
            }
        }, sessionDescription)
    }

    fun addIceCandidate(userId: String, candidate: String, sdpMid: String?, sdpMLineIndex: Int?) {
        val manager = peerConnections[userId]
        if (manager == null) {
            // Queue candidate until peer connection exists
            val iceCandidate = IceCandidate(sdpMid, sdpMLineIndex ?: -1, candidate)
            iceCandidatesQueue.getOrPut(userId) { mutableListOf() }.add(iceCandidate)
            return
        }
        val iceCandidate = IceCandidate(sdpMid, sdpMLineIndex ?: -1, candidate)
        manager.addIceCandidate(iceCandidate)
    }

    fun closePeerConnection(userId: String) {
        peerConnections.remove(userId)?.close()
        iceCandidatesQueue.remove(userId)
    }

    fun closeAll() {
        peerConnections.values.forEach { it.close() }
        peerConnections.clear()
        iceCandidatesQueue.clear()
    }

    fun dispose() {
        closeAll()
        peerConnectionFactory?.dispose()
        peerConnectionFactory = null
        eglBase?.release()
        eglBase = null
    }

    private fun observeSignaling() {
        CoroutineScope(Dispatchers.Main).launch {
            signalClient.signalingEvents.collect { event ->
                when (event) {
                    is SignalEvent.OfferReceived -> {
                        createPeerConnection(event.fromUserId)
                        setRemoteDescription(event.fromUserId, event.sdp, isOffer = true)
                    }
                    is SignalEvent.AnswerReceived -> {
                        createPeerConnection(event.fromUserId)
                        setRemoteDescription(event.fromUserId, event.sdp, isOffer = false)
                    }
                    is SignalEvent.IceCandidateReceived -> {
                        addIceCandidate(event.fromUserId, event.candidate, event.sdpMid, event.sdpMLineIndex)
                    }
                    is SignalEvent.PresenceUpdated -> {
                        // Handle presence if needed
                    }
                }
            }
        }
    }
}