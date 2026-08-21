package com.leitz.app.p2p

import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

class PeerConnectionManager(
    private val userId: String,
    private val peerConnection: PeerConnection
) {
    fun createOffer(observer: SdpObserver) {
        peerConnection.createOffer(observer, MediaConstraints())
    }

    fun createAnswer(observer: SdpObserver) {
        peerConnection.createAnswer(observer, MediaConstraints())
    }

    fun setLocalDescription(observer: SdpObserver, sdp: SessionDescription) {
        peerConnection.setLocalDescription(observer, sdp)
    }

    fun setRemoteDescription(observer: SdpObserver, sdp: SessionDescription) {
        peerConnection.setRemoteDescription(observer, sdp)
    }

    fun addIceCandidate(candidate: IceCandidate) {
        peerConnection.addIceCandidate(candidate)
    }

    fun close() {
        peerConnection.close()
    }
}