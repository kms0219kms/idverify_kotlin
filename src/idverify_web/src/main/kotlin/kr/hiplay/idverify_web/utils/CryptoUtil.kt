package kr.hiplay.idverify_web.utils

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


class CryptoUtil {
    /**
     * 데이터의 AES 암호화 처리를 위한 함수
     *
     * @param specName  스펙명 (e.g. AES/CBC/PKCS5Padding)
     * @param keyString 암호화 키값
     * @param ivString  암호화 iv값
     * @param plainText 암호화할 원본 string 텍스트 값
     */
    fun AESEncrypt(
        specName: String,
        keyString: String,
        ivString: String,
        plainText: String
    ): String {
        val encryptionKeyBytes: ByteArray = keyString.toByteArray(StandardCharsets.UTF_8)
        val key: SecretKey = SecretKeySpec(encryptionKeyBytes, "AES")
        val iv = IvParameterSpec(ivString.toByteArray())

        val cipher: Cipher = Cipher.getInstance(specName)
        cipher.init(Cipher.ENCRYPT_MODE, key, iv)

        val encrypted: ByteArray =
            cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
        return String(Base64.getEncoder().encode(encrypted))
    }

    /**
     * 데이터의 AES 복호화 처리를 위한 함수
     *
     * @param specName   스펙명 (e.g. AES/CBC/PKCS5Padding)
     * @param keyString  암호화 키값
     * @param ivString   암호화 iv값
     * @param cipherText 복호화할 암호화된 string 텍스트 값
     */
    fun AESDecrypt(
        specName: String,
        keyString: String,
        ivString: String,
        cipherText: String
    ): String {
        val encryptionKeyBytes: ByteArray = keyString.toByteArray(StandardCharsets.UTF_8)
        val key: SecretKey = SecretKeySpec(encryptionKeyBytes, "AES")
        val iv = IvParameterSpec(ivString.toByteArray())

        val cipher: Cipher = Cipher.getInstance(specName)
        cipher.init(Cipher.DECRYPT_MODE, key, iv)

        val decrypted: ByteArray = cipher.doFinal(Base64.getDecoder().decode(cipherText))
        return String(decrypted, StandardCharsets.UTF_8)
    }

    /**
     * 데이터의 SHA256 해시 처리를 위한 함수
     *
     * @param plainText 해시 처리할 원본 string 텍스트 값
     */
    fun SHA256Hash(plainText: String): String {
        val bytes = plainText.toByteArray(StandardCharsets.UTF_8)
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
