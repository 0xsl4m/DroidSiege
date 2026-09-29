package com.droidsiege

import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidsiege.challenges.network.MockBackend
import com.droidsiege.challenges.network.NetworkClients
import com.google.common.truth.Truth.assertThat
import okhttp3.Request
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** M5: the insecure channels deliver the payloads; the hardened ones refuse. */
@RunWith(AndroidJUnit4::class)
class NetworkChallengesInstrumentedTest {
    @Test
    fun cleartextDeliversThePayloadVulnerably() {
        MockBackend.ensureStarted()
        val client = MockBackend.plainClient()
        val response = client.newCall(
            Request.Builder()
                .url("http://127.0.0.1:${MockBackend.plainPort}/clear/L1")
                .build(),
        ).execute()
        response.use {
            assertThat(it.code).isEqualTo(200)
            assertThat(it.body!!.string()).contains("DS{network_cleartext_L1_61d4b8}")
        }
    }

    @Test
    fun trustAllClientAcceptsTheImpostorCertificate() {
        MockBackend.ensureStarted()
        // the MITM endpoint presents the attacker's self-signed certificate
        val client = NetworkClients.trustAllClient()
        val response = client.newCall(
            Request.Builder()
                .url(NetworkClients.httpsUrl(viaMitm = true, path = "/trust/L1"))
                .build(),
        ).execute()
        response.use {
            assertThat(it.code).isEqualTo(200)
            assertThat(it.body!!.string()).contains("DS{network_trustall_L1_78b4c2}")
        }
    }

    @Test
    fun defaultTrustRejectsTheImpostorCertificate() {
        MockBackend.ensureStarted()
        val client = MockBackend.defaultTlsClient()
        val handshakeFailed = runCatching {
            client.newCall(
                Request.Builder()
                    .url(NetworkClients.httpsUrl(viaMitm = true, path = "/trust/L1"))
                    .build(),
            ).execute()
        }.isFailure
        assertThat(handshakeFailed).isTrue()
    }

    @Test
    fun wrongHostPinDoesNotProtectTheChannel() {
        MockBackend.ensureStarted()
        // the L3 misconfiguration: a pin registered for a host that is never used
        val client = NetworkClients.pinnedClient(viaMitm = true, correctPin = false)
        val response = client.newCall(
            Request.Builder()
                .url(NetworkClients.httpsUrl(viaMitm = true, path = "/pin/L3"))
                .build(),
        ).execute()
        response.use {
            assertThat(it.body!!.string()).contains("DS{network_pinning_L3_b26f45}")
        }
    }

    @Test
    fun correctPinStopsTheImpostor() {
        MockBackend.ensureStarted()
        val client = NetworkClients.pinnedClient(viaMitm = true, correctPin = true)
        val pinHeld = runCatching {
            client.newCall(
                Request.Builder()
                    .url(NetworkClients.httpsUrl(viaMitm = true, path = "/pin/L3"))
                    .build(),
            ).execute()
        }.isFailure
        assertThat(pinHeld).isTrue()
    }
}

/** F1 regression: in-app network KitActions must run off-main and surface the flag. */
@RunWith(AndroidJUnit4::class)
class NetworkKitActionUiTest {
    @get:Rule
    val composeRule = androidx.compose.ui.test.junit4.createComposeRule()

    @Test
    fun kitActionFetchRunsOffMainAndShowsTheFlag() {
        MockBackend.ensureStarted()
        composeRule.setContent {
            com.droidsiege.ui.theme.DroidSiegeTheme {
                com.droidsiege.challenges.common.ActionChallengeScreen(
                    secureMode = false,
                    actions = listOf(
                        com.droidsiege.challenges.common.KitAction("fetch") { _, secure ->
                            val client =
                                if (!secure) MockBackend.plainClient() else MockBackend.defaultTlsClient()
                            client.newCall(
                                Request.Builder()
                                    .url("http://127.0.0.1:${MockBackend.plainPort}/clear/L1")
                                    .build(),
                            ).execute().use { it.body!!.string() }
                        },
                    ),
                )
            }
        }
        composeRule.onNodeWithText("fetch").performClick()
        composeRule.waitUntil(10_000) {
            composeRule
                .onAllNodesWithText("DS{network_cleartext_L1_61d4b8}", substring = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }
}
