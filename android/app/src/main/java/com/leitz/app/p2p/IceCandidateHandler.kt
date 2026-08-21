package com.leitz.app.p2p

import org.webrtc.IceCandidate
import org.webrtc.PeerConnection

class IceCandidateHandler(
    private val peerConnection: PeerConnection
) {
    private var isRemoteDescriptionSet = false
    private val pendingCandidates = mutableListOf<IceCandidate>()

    fun addCandidate(candidate: IceCandidate) {
        if (isRemoteDescriptionSet) {
            peerConnection.addIceCandidate(candidate)
        } else {
            pendingCandidates.add(candidate)
        }
    }

    fun onRemoteDescriptionSet() {
        isRemoteDescriptionSet = true
        pendingCandidates.forEach { peerConnection.addIceCandidate(it) }
        pendingCandidates.clear()
    }

    fun reset() {
        isRemoteDescriptionSet = false
        pendingCandidates.clear()
    }
}