package com.droidsiege.challenges.auth

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class JwtCodecTest {
    private val key = "test-key".toByteArray()

    @Test
    fun encodeDecodeRoundTripsSignedTokens() {
        val token = JwtCodec.encode(
            header = mapOf("alg" to "HS256", "typ" to "JWT"),
            payload = mapOf("role" to "player"),
            hmacKey = key,
        )
        val decoded = JwtCodec.decode(token)
        assertThat(decoded.header["alg"]).isEqualTo("HS256")
        assertThat(decoded.payload["role"]).isEqualTo("player")
    }

    @Test
    fun secureVerifyAcceptsCorrectlySignedTokens() {
        val token = JwtCodec.encode(
            header = mapOf("alg" to "HS256"),
            payload = mapOf("role" to "player"),
            hmacKey = key,
        )
        assertThat(JwtCodec.secureVerify(token, key)).isTrue()
    }

    @Test
    fun secureVerifyRejectsAlgNone() {
        val token = JwtCodec.encode(
            header = mapOf("alg" to "none"),
            payload = mapOf("role" to "admin"),
            hmacKey = null,
        )
        assertThat(JwtCodec.secureVerify(token, key)).isFalse()
        // and the insecure client check accepts it — the lesson
        assertThat(JwtCodec.insecureVerify(token, key)).isTrue()
    }

    @Test
    fun secureVerifyRejectsTamperedPayloads() {
        val token = JwtCodec.encode(
            header = mapOf("alg" to "HS256"),
            payload = mapOf("role" to "player"),
            hmacKey = key,
        )
        // swap the signed payload for an unsigned admin claim; same header, new sig missing
        val header = token.substringBefore(".")
        val forged = header + "." + JwtCodec.encode(
            header = mapOf("alg" to "HS256"),
            payload = mapOf("role" to "admin"),
            hmacKey = "attacker-key".toByteArray(),
        ).substringAfter(".").substringBefore(".")
        assertThat(JwtCodec.secureVerify(forged, key)).isFalse()
    }

    @Test
    fun insecureVerifyAcceptsForgedTokensWithTheShippedKey() {
        // the L3 forge: attacker signs admin claims with the shipped HMAC key
        val forged = JwtCodec.encode(
            header = mapOf("alg" to "HS256"),
            payload = mapOf("role" to "admin"),
            hmacKey = key,
        )
        assertThat(JwtCodec.insecureVerify(forged, key)).isTrue()
    }
}
