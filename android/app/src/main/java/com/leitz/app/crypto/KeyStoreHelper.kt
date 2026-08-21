package com.leitz.app.crypto

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import javax.crypto.Cipher

class KeyStoreHelper(private val context: Context) {

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "leitz_identity_key"
        private const val KEY_ALGORITHM = KeyProperties.KEY_ALGORITHM_EC
        private const val KEY_PURPOSE = KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
    }

    private val keyStore: KeyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
        load(null)
    }

    fun generateKeyPairIfNeeded() {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyPairGenerator = KeyPairGenerator.getInstance(KEY_ALGORITHM, KEYSTORE_PROVIDER)
            keyPairGenerator.initialize(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KEY_PURPOSE
                )
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .build()
            )
            keyPairGenerator.generateKeyPair()
        }
    }

    fun getPublicKey(): PublicKey {
        return keyStore.getCertificate(KEY_ALIAS).publicKey
    }

    fun getPrivateKey(): PrivateKey {
        return keyStore.getKey(KEY_ALIAS, null) as PrivateKey
    }

    fun getPublicKeyBase64(): String {
        return Base64.encodeToString(getPublicKey().encoded, Base64.NO_WRAP)
    }

    fun sign(data: ByteArray): ByteArray {
        val privateKey = getPrivateKey()
        val signature = java.security.Signature.getInstance("SHA256withECDSA")
        signature.initSign(privateKey)
        signature.update(data)
        return signature.sign()
    }

    fun verify(data: ByteArray, signature: ByteArray, publicKey: PublicKey): Boolean {
        val verifier = java.security.Signature.getInstance("SHA256withECDSA")
        verifier.initVerify(publicKey)
        verifier.update(data)
        return verifier.verify(signature)
    }

    fun encryptWithPublicKey(data: ByteArray, publicKey: PublicKey): ByteArray {
        val cipher = Cipher.getInstance("ECIES")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        return cipher.doFinal(data)
    }

    fun decryptWithPrivateKey(data: ByteArray): ByteArray {
        val privateKey = getPrivateKey()
        val cipher = Cipher.getInstance("ECIES")
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        return cipher.doFinal(data)
    }
}
