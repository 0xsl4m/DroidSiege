package com.droidsiege.backend

import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.text.Charsets.UTF_8

/** Minimal JWT codec for the brokenauth L2 lab (HS256 plus the alg:none trap). */
object Jwt {
    private val enc = Base64.getUrlEncoder().withoutPadding()
    private val dec = Base64.getUrlDecoder()

    data class Decoded(
        val headerJson: String,
        val payloadJson: String,
        val providedSig: String,
        val signingInput: String,
    )

    fun encode(
        payloadJson: String,
        secret: String,
    ): String {
        val header = enc.encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".toByteArray(UTF_8))
        val body = enc.encodeToString(payloadJson.toByteArray(UTF_8))
        return "$header.$body.${sign("$header.$body", secret)}"
    }

    fun sign(
        data: String,
        secret: String,
    ): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(UTF_8), "HmacSHA256"))
        return enc.encodeToString(mac.doFinal(data.toByteArray(UTF_8)))
    }

    fun decode(token: String): Decoded? {
        val parts = token.split(".")
        if (parts.size !in 2..3) return null
        return runCatching {
            Decoded(
                headerJson = dec.decode(parts[0]).toString(UTF_8),
                payloadJson = dec.decode(parts[1]).toString(UTF_8),
                providedSig = parts.getOrElse(2) { "" },
                signingInput = parts[0] + "." + parts[1],
            )
        }.getOrNull()
    }

    /** alg:none tokens are 2-part; HS256 tokens carry a third part. */
    fun declaredAlg(headerJson: String): String =
        Regex("\"alg\"\\s*:\\s*\"([^\"]+)\"").find(headerJson)?.groupValues?.get(1) ?: ""
}
