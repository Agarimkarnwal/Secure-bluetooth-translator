package com.setu.app.bluetooth

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoManager {

    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BITS = 128
    private const val IV_LENGTH_BYTES = 12
    private const val PRE_SHARED_KEY_PASS = "Setu_iQOO_Offline_Security_Key_2026"
    private const val SALT = "Setu_NPU_AES_Salt"

    private val secretKey: SecretKey by lazy {
        deriveKey(PRE_SHARED_KEY_PASS, SALT)
    }

    private fun deriveKey(password: String, salt: String): SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt.toByteArray(), 1000, 256)
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, "AES")
    }

    /**
     * Encrypts plain payload into format: ENC:<IV_Base64>:<Ciphertext_Base64>
     */
    fun encrypt(plainText: String): String {
        return try {
            val iv = ByteArray(IV_LENGTH_BYTES)
            SecureRandom().nextBytes(iv)
            val cipher = Cipher.getInstance(ALGORITHM)
            val parameterSpec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec)

            val cipherTextBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val ivB64 = Base64.encodeToString(iv, Base64.NO_WRAP)
            val cipherB64 = Base64.encodeToString(cipherTextBytes, Base64.NO_WRAP)

            "ENC:$ivB64:$cipherB64"
        } catch (e: Exception) {
            DebugLogManager.log("CryptoManager", "Encryption failed: ${e.localizedMessage}")
            plainText
        }
    }

    /**
     * Decrypts ENC payload format back to plaintext.
     * If input is not encrypted, returns raw text.
     */
    fun decrypt(encryptedPayload: String): String {
        if (!encryptedPayload.startsWith("ENC:")) return encryptedPayload

        return try {
            val parts = encryptedPayload.split(":")
            if (parts.size < 3) return encryptedPayload

            val iv = Base64.decode(parts[1], Base64.NO_WRAP)
            val cipherTextBytes = Base64.decode(parts[2], Base64.NO_WRAP)

            val cipher = Cipher.getInstance(ALGORITHM)
            val parameterSpec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec)

            val plainTextBytes = cipher.doFinal(cipherTextBytes)
            String(plainTextBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            DebugLogManager.log("CryptoManager", "Decryption failed / payload tampered: ${e.localizedMessage}")
            "[TAMPERED_OR_CORRUPT_PAYLOAD]"
        }
    }
}
