package com.automatic.main

import java.security.KeyFactory
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import android.util.Base64

class HybridEncryptor(private val publicKeyPem: String) {

    private val publicKey: PublicKey = loadPublicKey(publicKeyPem)

    fun encrypt(message: String): Map<String, String> {
        // Генерация симметричного ключа AES
        val aesKey = generateAesKey()

        // Генерация случайного вектора инициализации (IV)
        val iv = generateIv()

        // Шифрование сообщения с использованием AES
        val aesEncryptedMessage = aesEncrypt(message, aesKey, iv)

        // Шифрование симметричного ключа с использованием RSA
        val encryptedAesKey = rsaEncrypt(aesKey.encoded, publicKey)

        // Возврат зашифрованных данных
        return mapOf(
            "encryptedKey" to Base64.encodeToString(encryptedAesKey, Base64.NO_WRAP),
            "iv" to Base64.encodeToString(iv, Base64.NO_WRAP),
            "ciphertext" to Base64.encodeToString(aesEncryptedMessage, Base64.NO_WRAP)
        )
    }

    private fun loadPublicKey(publicKeyPem: String): PublicKey {
        val publicKeyBase64 = publicKeyPem
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\\s".toRegex(), "")
        val keyBytes = Base64.decode(publicKeyBase64, Base64.DEFAULT)
        val keySpec = X509EncodedKeySpec(keyBytes)
        return KeyFactory.getInstance("RSA").generatePublic(keySpec)
    }

    private fun generateAesKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(256) // 256-битный ключ
        return keyGenerator.generateKey()
    }

    private fun generateIv(): ByteArray {
        val iv = ByteArray(16) // 16 байт для AES
        SecureRandom().nextBytes(iv)
        return iv
    }

    private fun aesEncrypt(message: String, key: SecretKey, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))
        return cipher.doFinal(message.toByteArray(Charsets.UTF_8))
    }

    private fun rsaEncrypt(data: ByteArray, publicKey: PublicKey): ByteArray {
        val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        return cipher.doFinal(data)
    }
}
