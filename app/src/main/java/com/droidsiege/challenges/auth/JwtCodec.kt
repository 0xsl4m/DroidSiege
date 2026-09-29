package com.droidsiege.challenges.auth

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * A minimal, spec-faithful JWT codec for the auth tiers. Every part is standard:
 * base64url without padding, dot-separated header.payload.signature, HMAC-SHA256 for
 * signed tokens, and the alg:none variant that carries an EMPTY signature.
 */
object JwtCodec {
    data class Token(val header: Map<String, String>, val payload: Map<String, String>, val signature: ByteArray)

    // pure-Kotlin base64url so the codec is unit-testable on the JVM
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"

    private fun b64url(bytes: ByteArray): String {
        val out = StringBuilder()
        var i = 0
        while (i + 2 < bytes.size + 1 && i < bytes.size) {
            val b0 = bytes[i].toInt() and 0xFF
            val b1 = if (i + 1 < bytes.size) bytes[i + 1].toInt() and 0xFF else 0
            val b2 = if (i + 2 < bytes.size) bytes[i + 2].toInt() and 0xFF else 0
            out.append(ALPHABET[b0 shr 2])
            out.append(ALPHABET[(b0 and 0x3) shl 4 or (b1 shr 4)])
            if (i + 1 < bytes.size) out.append(ALPHABET[(b1 and 0xF) shl 2 or (b2 shr 6)])
            if (i + 2 < bytes.size) out.append(ALPHABET[b2 and 0x3F])
            i += 3
        }
        return out.toString()
    }

    private fun unb64url(text: String): ByteArray {
        val clean = text.replace("=", "")
        val out = ByteArray(clean.length * 6 / 8)
        var buffer = 0
        var bits = 0
        var index = 0
        for (ch in clean) {
            val value = ALPHABET.indexOf(ch)
            require(value >= 0) { "invalid base64url char $ch" }
            buffer = (buffer shl 6) or value
            bits += 6
            if (bits >= 8) {
                bits -= 8
                out[index++] = (buffer shr bits).toByte()
            }
        }
        return out.copyOf(index)
    }

    fun hmacSha256(
        key: ByteArray,
        data: ByteArray,
    ): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }

    private fun json(map: Map<String, String>): String =
        map.entries.joinToString(",", "{", "}") { "\"${it.key}\":\"${it.value}\"" }

    /** Parses a flat JSON object of string values without a full JSON parser. */
    fun flatJson(json: String): Map<String, String> {
        val regex = Regex("\"([^\"]+)\"\\s*:\\s*\"([^\"]+)\"")
        return regex.findAll(json).associate { it.groupValues[1] to it.groupValues[2] }
    }

    fun encode(
        header: Map<String, String>,
        payload: Map<String, String>,
        hmacKey: ByteArray?,
    ): String {
        val signingInput = b64url(json(header).toByteArray()) + "." + b64url(json(payload).toByteArray())
        val signature =
            when (header["alg"]) {
                "none" -> ""
                "HS256" -> b64url(hmacSha256(hmacKey!!, signingInput.toByteArray()))
                else -> throw IllegalArgumentException("unsupported alg ${header["alg"]}")
            }
        return "$signingInput.$signature"
    }

    fun decode(token: String): Token {
        val parts = token.split(".")
        val header = flatJson(String(unb64url(parts[0])))
        val payload = flatJson(String(unb64url(parts[1])))
        val signature = if (parts.size > 2) unb64url(parts[2]) else ByteArray(0)
        return Token(header, payload, signature)
    }

    /** The vulnerable client check: signature verified only if alg != none. */
    fun insecureVerify(
        token: String,
        hmacKey: ByteArray,
    ): Boolean {
        val decoded = decode(token)
        return when (decoded.header["alg"]) {
            "none" -> true // the bug: unsigned tokens are accepted outright
            "HS256" -> {
                val parts = token.split(".")
                val expected = hmacSha256(hmacKey, (parts[0] + "." + parts[1]).toByteArray())
                expected.contentEquals(decoded.signature)
            }
            else -> false
        }
    }

    /** The hardened check: alg must be HS256 AND the signature must verify. */
    fun secureVerify(
        token: String,
        hmacKey: ByteArray,
    ): Boolean {
        val decoded = decode(token)
        if (decoded.header["alg"] != "HS256") return false
        val parts = token.split(".")
        if (parts.size < 3 || parts[2].isEmpty()) return false
        val expected = hmacSha256(hmacKey, (parts[0] + "." + parts[1]).toByteArray())
        return expected.contentEquals(decoded.signature)
    }
}
