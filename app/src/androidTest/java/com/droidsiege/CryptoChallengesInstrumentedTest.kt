package com.droidsiege

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidsiege.challenges.common.HexCodec
import com.droidsiege.challenges.common.RawAes
import com.droidsiege.challenges.crypto.EcbIvVault
import com.droidsiege.challenges.crypto.HardcodedKeyVault
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import javax.crypto.spec.SecretKeySpec

/** M10: the vulnerable artifact decrypts with the leaked key; secureMode seals it. */
@RunWith(AndroidJUnit4::class)
class CryptoChallengesInstrumentedTest {
    @Test
    fun hardcodedLicenseDecryptsWithStaticKeyVulnerably() {
        val lines = HardcodedKeyVault.licenseLines(secureMode = false).toMap()
        val blob = android.util.Base64.decode(
            lines.getValue("License blob (base64, iv||ct)"),
            android.util.Base64.NO_WRAP,
        )
        val iv = blob.copyOfRange(0, 12)
        val ct = blob.copyOfRange(12, blob.size)
        val key = SecretKeySpec("droidsiege-static".toByteArray().copyOf(16), "AES")
        val plaintext = RawAes.gcmDecrypt(key, iv, ct)
        assertThat(String(plaintext)).isEqualTo("DS{crypto_hardcoded_L1_41f7c2}")
    }

    @Test
    fun hardenedLicenseDefeatsTheStaticKey() {
        val lines = HardcodedKeyVault.licenseLines(secureMode = true).toMap()
        assertThat(lines.getValue("Key location")).contains("AndroidKeyStore")
        val blob = android.util.Base64.decode(
            lines.getValue("License blob (base64, iv||ct)"),
            android.util.Base64.NO_WRAP,
        )
        val iv = blob.copyOfRange(0, 12)
        val ct = blob.copyOfRange(12, blob.size)
        val staticKey = SecretKeySpec("droidsiege-static".toByteArray().copyOf(16), "AES")
        val failed = runCatching { RawAes.gcmDecrypt(staticKey, iv, ct) }.isFailure
        assertThat(failed).isTrue()
    }

    @Test
    fun oracleKeyIsPerInstallAndBehavesPerMode() {
        val context = androidx.test.platform.app.InstrumentationRegistry
            .getInstrumentation()
            .targetContext
        val token = com.droidsiege.challenges.crypto.EcbIvVault.oracleToken(context)

        // sanity: the oracle accepts its own token in the insecure mode
        val accepted = com.droidsiege.challenges.crypto.EcbIvVault
            .probeToken(context, token, secureMode = false)
        assertThat(accepted).contains("token accepted")

        // the intended attack: a last byte of 0x00 always yields the padding error
        val bytes = com.droidsiege.challenges.common.HexCodec.fromHex(token)
        bytes[bytes.size - 1] = 0x00
        val tampered = com.droidsiege.challenges.common.HexCodec.toHex(bytes)
        val answered = com.droidsiege.challenges.crypto.EcbIvVault
            .probeToken(context, tampered, secureMode = false)
        assertThat(answered).isEqualTo("padding error")

        // hardened: the validator answers identically for every input
        val hardened = com.droidsiege.challenges.crypto.EcbIvVault
            .probeToken(context, token, secureMode = true)
        assertThat(hardened).isEqualTo("token rejected")
    }

    @Test
    fun nonceReuseRecoversTheRecoveryMessageVulnerably() {
        val lines = EcbIvVault.gcmLines(secureMode = false).toMap()
        val known = lines.getValue("Welcome plaintext (public)")
        val c1 = HexCodec.fromHex(lines.getValue("Welcome ciphertext (hex)"))
        val c2 = HexCodec.fromHex(lines.getValue("Recovery ciphertext (hex)"))
        val ct1 = c1.copyOfRange(0, c1.size - 16)
        val ct2 = c2.copyOfRange(0, c2.size - 16)
        val keystream = ct1.mapIndexed { i, b -> (b.toInt() xor known[i].code).toByte() }.toByteArray()
        val recovered = ct2
            .mapIndexed { i, b -> (b.toInt() xor keystream[i].toInt()).toByte() }
            .toByteArray()
            .toString(Charsets.UTF_8)
        assertThat(recovered).isEqualTo("DS{crypto_ecbiv_L4_82ea6f}")
    }
}
