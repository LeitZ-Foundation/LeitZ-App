package com.leitz.app.network

data class StunTurnConfig(
    val stunUrls: List<String>,
    val turnUrls: List<String>,
    val turnUsername: String,
    val turnPassword: String
) {
    companion object {
        fun default(): StunTurnConfig {
            return StunTurnConfig(
                stunUrls = listOf(
                    "stun:stun.leitz.org:3478",
                    "stun:stun.l.google.com:19302"
                ),
                turnUrls = listOf(
                    "turn:turn.leitz.org:3478?transport=udp",
                    "turn:turn.leitz.org:3478?transport=tcp"
                ),
                turnUsername = "leitz_user",
                turnPassword = "leitz_password"
            )
        }
    }
}