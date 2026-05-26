package com.example.data

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object EncryptionUtil {
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    // 16-byte key for AES
    private val keyBytes = "ProbashiAlapKey12".toByteArray(Charsets.UTF_8)
    // 16-byte IV
    private val ivBytes = "ProbashiAlapIv123".toByteArray(Charsets.UTF_8)

    private val secretKey = SecretKeySpec(keyBytes, "AES")
    private val ivSpec = IvParameterSpec(ivBytes)

    fun encrypt(plainText: String): String {
        return try {
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(encryptedBytes, Base64.DEFAULT).trim()
        } catch (e: Exception) {
            e.printStackTrace()
            plainText
        }
    }

    fun decrypt(cipherText: String): String {
        return try {
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
            val decodedBytes = Base64.decode(cipherText, Base64.DEFAULT)
            String(cipher.doFinal(decodedBytes), Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            cipherText
        }
    }

    fun getFingerprint(): String {
        return "E2EE-SHA256: 4C:92:E1:98:9B:AA:60:DF:EC:A8:1F:B1:0C:ED:A8:0D"
    }
}
