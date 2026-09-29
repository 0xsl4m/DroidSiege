package com.droidsiege.challenges.common

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/** Small hex/base64 helpers shared by the storage and crypto challenges. */
object HexCodec {
    fun toHex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }

    fun fromHex(hex: String): ByteArray {
        val clean = hex.trim().removePrefix("0x")
        require(clean.length % 2 == 0) { "odd-length hex" }
        return ByteArray(clean.length / 2) { i ->
            clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }

    fun toBase64(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)

    fun fromBase64(value: String): ByteArray = Base64.decode(value, Base64.NO_WRAP)
}

/** XOR helpers for the "homegrown crypto" family. */
object XorCipher {
    fun xorSingle(
        input: ByteArray,
        key: Byte,
    ): ByteArray =
        ByteArray(input.size) { i ->
            (input[i].toInt() xor key.toInt()).toByte()
        }

    fun xorRepeating(
        input: ByteArray,
        key: ByteArray,
    ): ByteArray =
        ByteArray(input.size) { i ->
            (input[i].toInt() xor key[i % key.size].toInt()).toByte()
        }
}

/** Symmetric-crypto helpers the challenges intentionally misuse. */
object RawAes {
    fun keyFrom(bytes: ByteArray): SecretKeySpec = SecretKeySpec(bytes, "AES")

    fun ecbEncrypt(
        key: java.security.Key,
        plaintext: ByteArray,
    ): ByteArray =
        Cipher.getInstance("AES/ECB/PKCS5Padding").run {
            init(Cipher.ENCRYPT_MODE, key)
            doFinal(plaintext)
        }

    fun ecbDecrypt(
        key: java.security.Key,
        ciphertext: ByteArray,
    ): ByteArray =
        Cipher.getInstance("AES/ECB/PKCS5Padding").run {
            init(Cipher.DECRYPT_MODE, key)
            doFinal(ciphertext)
        }

    fun cbcEncrypt(
        key: java.security.Key,
        iv: ByteArray,
        plaintext: ByteArray,
    ): ByteArray =
        Cipher.getInstance("AES/CBC/PKCS5Padding").run {
            init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))
            doFinal(plaintext)
        }

    fun cbcDecrypt(
        key: java.security.Key,
        iv: ByteArray,
        ciphertext: ByteArray,
    ): ByteArray =
        Cipher.getInstance("AES/CBC/PKCS5Padding").run {
            init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))
            doFinal(ciphertext)
        }

    fun gcmEncrypt(
        key: java.security.Key,
        nonce: ByteArray,
        plaintext: ByteArray,
    ): ByteArray =
        Cipher.getInstance("AES/GCM/NoPadding").run {
            init(Cipher.ENCRYPT_MODE, key, javax.crypto.spec.GCMParameterSpec(128, nonce))
            doFinal(plaintext)
        }

    fun gcmDecrypt(
        key: java.security.Key,
        nonce: ByteArray,
        ciphertext: ByteArray,
    ): ByteArray =
        Cipher.getInstance("AES/GCM/NoPadding").run {
            init(Cipher.DECRYPT_MODE, key, javax.crypto.spec.GCMParameterSpec(128, nonce))
            doFinal(ciphertext)
        }
}

object WeakKdf {
    fun md5(input: ByteArray): ByteArray = MessageDigest.getInstance("MD5").digest(input)

    fun sha256(input: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(input)

    fun pbkdf2(
        password: CharArray,
        salt: ByteArray,
        iterations: Int,
        keyLengthBits: Int = 256,
    ): ByteArray {
        val spec = javax.crypto.spec.PBEKeySpec(password, salt, iterations, keyLengthBits)
        val factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }
}

/** Keystore-backed AES key used by the hardened (secureMode) paths. */
object KeystoreVault {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"

    /**
     * Returns the hardware-backed key itself — its bytes never leave the TEE. The
     * Keystore generates and manages the GCM nonce; use [SealedBox] to seal/unseal.
     */
    fun loadOrCreateKey(alias: String): javax.crypto.SecretKey {
        val keyStore = java.security.KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        keyStore.getKey(alias, null)?.let { return it as javax.crypto.SecretKey }
        val generator = javax.crypto.KeyGenerator.getInstance("AES", ANDROID_KEYSTORE)
        generator.init(
            android.security.keystore.KeyGenParameterSpec.Builder(
                alias,
                android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or
                    android.security.keystore.KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }
}

/**
 * The canonical hardened seal: the (Keystore) key generates its own random nonce, and
 * the returned blob is `iv ++ ciphertext(++ tag)`. This is the pattern every
 * secureMode path should teach — never a caller-managed IV.
 */
object SealedBox {
    fun seal(
        key: java.security.Key,
        plaintext: ByteArray,
    ): ByteArray =
        Cipher.getInstance("AES/GCM/NoPadding").run {
            init(Cipher.ENCRYPT_MODE, key)
            iv + doFinal(plaintext)
        }

    fun unseal(
        key: java.security.Key,
        sealed: ByteArray,
    ): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            key,
            javax.crypto.spec.GCMParameterSpec(128, sealed.copyOf(12)),
        )
        return cipher.doFinal(sealed.copyOfRange(12, sealed.size))
    }
}
