package com.leitz.app.crypto

import android.content.Context
import org.whispersystems.libsignal.SignalProtocolAddress
import org.whispersystems.libsignal.SessionBuilder
import org.whispersystems.libsignal.SessionCipher
import org.whispersystems.libsignal.protocol.CiphertextMessage
import org.whispersystems.libsignal.protocol.PreKeySignalMessage
import org.whispersystems.libsignal.protocol.SignalMessage
import org.whispersystems.libsignal.state.PreKeyBundle
import org.whispersystems.libsignal.state.PreKeyRecord
import org.whispersystems.libsignal.state.SignalProtocolStore
import org.whispersystems.libsignal.state.SignedPreKeyRecord
import org.whispersystems.libsignal.util.KeyHelper

class SignalProtocolManager(
    private val context: Context,
    private val signalProtocolStore: SignalProtocolStore
) {
    private val registrationId: Int = KeyHelper.generateRegistrationId(false)

    fun generateIdentityKeyPair() {
        // Identity key pair generation is handled by SignalProtocolStore
        // Usually stored on first initialization
    }

    fun generatePreKeys(start: Int, count: Int): List<PreKeyRecord> {
        return KeyHelper.generatePreKeys(start, count)
    }

    fun generateSignedPreKey(identityKeyPair: org.whispersystems.libsignal.IdentityKeyPair, signedPreKeyId: Int): SignedPreKeyRecord {
        return KeyHelper.generateSignedPreKey(identityKeyPair, signedPreKeyId)
    }

    fun encryptMessage(recipientUserId: String, message: ByteArray): ByteArray {
        val address = SignalProtocolAddress(recipientUserId, 1) // deviceId = 1
        val sessionCipher = SessionCipher(signalProtocolStore, address)
        val ciphertext = sessionCipher.encrypt(message)
        return when (ciphertext) {
            is SignalMessage -> ciphertext.serialize()
            is PreKeySignalMessage -> ciphertext.serialize()
            else -> throw IllegalArgumentException("Unknown ciphertext type")
        }
    }

    fun decryptMessage(senderUserId: String, encryptedMessage: ByteArray): ByteArray {
        val address = SignalProtocolAddress(senderUserId, 1)
        val sessionCipher = SessionCipher(signalProtocolStore, address)
        val plaintext = sessionCipher.decrypt(
            when {
                encryptedMessage.size > 0 && encryptedMessage[0].toInt() == CiphertextMessage.SIGNAL_TYPE -> SignalMessage(encryptedMessage)
                encryptedMessage.size > 0 && encryptedMessage[0].toInt() == CiphertextMessage.PREKEY_TYPE -> PreKeySignalMessage(encryptedMessage)
                else -> throw IllegalArgumentException("Invalid message type")
            }
        )
        return plaintext
    }

    fun processPreKeyBundle(recipientUserId: String, preKeyBundle: PreKeyBundle) {
        val address = SignalProtocolAddress(recipientUserId, 1)
        val sessionBuilder = SessionBuilder(signalProtocolStore, address)
        sessionBuilder.process(preKeyBundle)
    }

    fun isSessionEstablished(recipientUserId: String): Boolean {
        val address = SignalProtocolAddress(recipientUserId, 1)
        return signalProtocolStore.containsSession(address)
    }

    fun getLocalRegistrationId(): Int = registrationId
}