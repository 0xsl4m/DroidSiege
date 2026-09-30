package com.droidsiege.backend

import com.google.common.truth.Truth.assertThat
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Every family tier is exercised twice: the exploit must reveal its flag with
 * SECURE_MODE=off, and the hardened variant must refuse with SECURE_MODE=on.
 */
class BackendRoutesTest {
    @Before
    fun setUp() {
        SecureMode.enabled = false
        Store.reset()
    }

    @After
    fun tearDown() {
        Store.reset()
    }

    // --- helpers -----------------------------------------------------------

    private fun withApp(test: suspend (io.ktor.client.HttpClient) -> Unit) =
        testApplication {
            application { module() }
            test(client)
        }

    private suspend fun login(
        client: io.ktor.client.HttpClient,
        username: String,
        password: String,
    ): String {
        val response = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"$username","password":"$password"}""")
        }
        check(response.status == HttpStatusCode.OK) { "login failed: ${response.bodyAsText()}" }
        return Json.parseToJsonElement(response.bodyAsText()).jsonObject["token"]!!.toString()
            .trim('"')
    }

    private suspend fun HttpResponse.json(): JsonObject = Json.parseToJsonElement(bodyAsText()).jsonObject

    // --- health ------------------------------------------------------------

    @Test
    fun `health endpoint responds`() =
        withApp { client ->
            val response = client.get("/health")
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            assertThat(response.bodyAsText()).contains("droidsiege")
        }

    // --- idor L1 -----------------------------------------------------------

    @Test
    fun `idor L1 vuln - any user id returns its secret`() =
        withApp { client ->
            val token = login(client, "alice", "hunter2")
            val response = client.get("/api/users/2/secret") { header(HttpHeaders.Authorization, "Bearer $token") }
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            assertThat(response.bodyAsText()).contains(Store.IDOR_L1)
        }

    @Test
    fun `idor L1 hardened - other user's secret is forbidden, own is allowed`() =
        withApp { client ->
            SecureMode.enabled = true
            val alice = login(client, "alice", "hunter2")
            val bob = login(client, "bob", "password1")
            val foreign = client.get("/api/users/2/secret") { header(HttpHeaders.Authorization, "Bearer $alice") }
            assertThat(foreign.status).isEqualTo(HttpStatusCode.Forbidden)
            val own = client.get("/api/users/1/secret") { header(HttpHeaders.Authorization, "Bearer $bob") }
            assertThat(own.status).isEqualTo(HttpStatusCode.Forbidden)
            val aliceOwn = client.get("/api/users/1/secret") { header(HttpHeaders.Authorization, "Bearer $alice") }
            assertThat(aliceOwn.status).isEqualTo(HttpStatusCode.OK)
            assertThat(aliceOwn.bodyAsText()).contains(Store.IDOR_L1)
        }

    // --- idor L2 -----------------------------------------------------------

    @Test
    fun `idor L2 vuln - enumerated hid opens the vault`() =
        withApp { client ->
            val hid = Store.hidFor(Store.VAULT_L2_ID)
            val response = client.get("/api/vaults/$hid")
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            assertThat(response.bodyAsText()).contains(Store.IDOR_L2)
        }

    @Test
    fun `idor L2 hardened - vault requires its owner`() =
        withApp { client ->
            SecureMode.enabled = true
            val hid = Store.hidFor(Store.VAULT_L2_ID)
            val bob = login(client, "bob", "password1")
            val bobToken = "Bearer $bob"
            val denied = client.get("/api/vaults/$hid") { header(HttpHeaders.Authorization, bobToken) }
            assertThat(denied.status).isEqualTo(HttpStatusCode.Forbidden)
            val alice = login(client, "alice", "hunter2")
            val allowed = client.get("/api/vaults/$hid") { header(HttpHeaders.Authorization, "Bearer $alice") }
            assertThat(allowed.status).isEqualTo(HttpStatusCode.OK)
            assertThat(allowed.bodyAsText()).contains(Store.IDOR_L2)
        }

    // --- idor L3 -----------------------------------------------------------

    @Test
    fun `idor L3 vuln - PUT skips the ownership check GET enforces`() =
        withApp { client ->
            val alice = login(client, "alice", "hunter2")
            val getForeign = client.get("/api/notes/2") { header(HttpHeaders.Authorization, "Bearer $alice") }
            assertThat(getForeign.status).isEqualTo(HttpStatusCode.Forbidden)
            val putForeign = client.put("/api/notes/2") {
                header(HttpHeaders.Authorization, "Bearer $alice")
                contentType(ContentType.Application.Json)
                setBody("""{"note":"hacked"}""")
            }
            assertThat(putForeign.status).isEqualTo(HttpStatusCode.OK)
            assertThat(putForeign.bodyAsText()).contains(Store.IDOR_L3)
        }

    @Test
    fun `idor L3 hardened - PUT on another user's note is forbidden`() =
        withApp { client ->
            SecureMode.enabled = true
            val alice = login(client, "alice", "hunter2")
            val response = client.put("/api/notes/2") {
                header(HttpHeaders.Authorization, "Bearer $alice")
                contentType(ContentType.Application.Json)
                setBody("""{"note":"hacked"}""")
            }
            assertThat(response.status).isEqualTo(HttpStatusCode.Forbidden)
            assertThat(response.bodyAsText()).doesNotContain(Store.IDOR_L3)
        }

    // --- idor L4 -----------------------------------------------------------

    @Test
    fun `idor L4 vuln - nested path serves an item from another order`() =
        withApp { client ->
            val alice = login(client, "alice", "hunter2")
            val response = client.get("/api/orders/101/items/502") {
                header(HttpHeaders.Authorization, "Bearer $alice")
            }
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            assertThat(response.bodyAsText()).contains(Store.IDOR_L4)
        }

    @Test
    fun `idor L4 hardened - full-path authorization rejects cross-order items`() =
        withApp { client ->
            SecureMode.enabled = true
            val alice = login(client, "alice", "hunter2")
            val crossOrder = client.get("/api/orders/101/items/502") {
                header(HttpHeaders.Authorization, "Bearer $alice")
            }
            assertThat(crossOrder.status).isEqualTo(HttpStatusCode.Forbidden)
            assertThat(crossOrder.bodyAsText()).doesNotContain(Store.IDOR_L4)
            val own = client.get("/api/orders/101/items/501") {
                header(HttpHeaders.Authorization, "Bearer $alice")
            }
            assertThat(own.status).isEqualTo(HttpStatusCode.OK)
        }

    // --- brokenauth L1 -----------------------------------------------------

    @Test
    fun `brokenauth L1 vuln - predicted never-issued token authenticates`() =
        withApp { client ->
            login(client, "alice", "hunter2")
            val predicted = client.get("/api/auth/whoami") {
                header(HttpHeaders.Authorization, "Bearer " + Store.predictableToken(2))
            }
            assertThat(predicted.status).isEqualTo(HttpStatusCode.OK)
            assertThat(predicted.bodyAsText()).contains(Store.BROKENAUTH_L1)
        }

    @Test
    fun `brokenauth L1 hardened - unissued tokens are rejected`() =
        withApp { client ->
            SecureMode.enabled = true
            val response = client.get("/api/auth/whoami") {
                header(HttpHeaders.Authorization, "Bearer " + Store.predictableToken(2))
            }
            assertThat(response.status).isEqualTo(HttpStatusCode.Unauthorized)
            assertThat(response.bodyAsText()).doesNotContain(Store.BROKENAUTH_L1)
        }

    // --- brokenauth L2 -----------------------------------------------------

    @Test
    fun `brokenauth L2 vuln - alg none forgery verifies and reveals the flag`() =
        withApp { client ->
            login(client, "alice", "hunter2")
            val forged = "eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0." +
                java.util.Base64.getUrlEncoder().withoutPadding()
                    .encodeToString("""{"sub":9,"role":"admin"}""".toByteArray())
            val response = client.get("/api/auth/jwt/verify?token=$forged")
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            assertThat(response.bodyAsText()).contains(Store.BROKENAUTH_L2)
        }

    @Test
    fun `brokenauth L2 vuln - weak secret forgery verifies`() =
        withApp { client ->
            login(client, "alice", "hunter2")
            val forged = Jwt.encode("""{"sub":9,"role":"admin"}""", Store.WEAK_JWT_SECRET)
            val response = client.get("/api/auth/jwt/verify?token=$forged")
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            assertThat(response.bodyAsText()).contains(Store.BROKENAUTH_L2)
        }

    @Test
    fun `brokenauth L2 hardened - strong-secret verifier rejects forgeries`() =
        withApp { client ->
            SecureMode.enabled = true
            val unsigned = "eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0." +
                java.util.Base64.getUrlEncoder().withoutPadding()
                    .encodeToString("""{"sub":9,"role":"admin"}""".toByteArray())
            val noneResult = client.get("/api/auth/jwt/verify?token=$unsigned")
            assertThat(noneResult.status).isEqualTo(HttpStatusCode.Unauthorized)
            val weakForged = Jwt.encode("""{"sub":9,"role":"admin"}""", Store.WEAK_JWT_SECRET)
            val weakResult = client.get("/api/auth/jwt/verify?token=$weakForged")
            assertThat(weakResult.status).isEqualTo(HttpStatusCode.Unauthorized)
            assertThat(noneResult.bodyAsText() + weakResult.bodyAsText())
                .doesNotContain(Store.BROKENAUTH_L2)
        }

    // --- brokenauth L3 -----------------------------------------------------

    @Test
    fun `brokenauth L3 vuln - svcadmin survives brute-force attempts with no lockout`() =
        withApp { client ->
            repeat(6) {
                client.post("/api/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"username":"svcadmin","password":"wrong"}""")
                }
            }
            val success = client.post("/api/auth/login") {
                contentType(ContentType.Application.Json)
                setBody("""{"username":"svcadmin","password":"123456"}""")
            }
            assertThat(success.status).isEqualTo(HttpStatusCode.OK)
            assertThat(success.bodyAsText()).contains(Store.BROKENAUTH_L3)
        }

    @Test
    fun `brokenauth L3 hardened - account locks and brute force can never succeed`() =
        withApp { client ->
            SecureMode.enabled = true
            repeat(6) {
                client.post("/api/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"username":"svcadmin","password":"wrong"}""")
                }
            }
            val brute = client.post("/api/auth/login") {
                contentType(ContentType.Application.Json)
                setBody("""{"username":"svcadmin","password":"123456"}""")
            }
            assertThat(brute.status).isEqualTo(HttpStatusCode.TooManyRequests)
            assertThat(brute.bodyAsText()).doesNotContain(Store.BROKENAUTH_L3)
        }

    // --- brokenauth L4 -----------------------------------------------------

    @Test
    fun `brokenauth L4 vuln - deterministic reset token hijacks the account`() =
        withApp { client ->
            client.post("/api/auth/forgot") {
                contentType(ContentType.Application.Json)
                setBody("""{"username":"bob"}""")
            }
            val guessed = Store.legacyResetToken("bob")
            val response = client.post("/api/auth/reset") {
                contentType(ContentType.Application.Json)
                setBody("""{"username":"bob","token":"$guessed","newPassword":"pwned123"}""")
            }
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            assertThat(response.bodyAsText()).contains(Store.BROKENAUTH_L4)
            val relogin = client.post("/api/auth/login") {
                contentType(ContentType.Application.Json)
                setBody("""{"username":"bob","password":"pwned123"}""")
            }
            assertThat(relogin.status).isEqualTo(HttpStatusCode.OK)
        }

    @Test
    fun `brokenauth L4 hardened - guessed tokens fail, real token works once`() =
        withApp { client ->
            SecureMode.enabled = true
            client.post("/api/auth/forgot") {
                contentType(ContentType.Application.Json)
                setBody("""{"username":"bob"}""")
            }
            val guessed = Store.legacyResetToken("bob")
            val guess = client.post("/api/auth/reset") {
                contentType(ContentType.Application.Json)
                setBody("""{"username":"bob","token":"$guessed","newPassword":"pwned123"}""")
            }
            assertThat(guess.status).isEqualTo(HttpStatusCode.BadRequest)
            val real = Store.resetTokens.getValue("bob").value
            val reset = client.post("/api/auth/reset") {
                contentType(ContentType.Application.Json)
                setBody("""{"username":"bob","token":"$real","newPassword":"fresh-pass"}""")
            }
            assertThat(reset.status).isEqualTo(HttpStatusCode.OK)
            assertThat(reset.bodyAsText()).doesNotContain(Store.BROKENAUTH_L4)
            val replay = client.post("/api/auth/reset") {
                contentType(ContentType.Application.Json)
                setBody("""{"username":"bob","token":"$real","newPassword":"again"}""")
            }
            assertThat(replay.status).isEqualTo(HttpStatusCode.BadRequest)
        }

    // --- massassign L1 -----------------------------------------------------

    @Test
    fun `massassign L1 vuln - role binding elevates into admin metrics`() =
        withApp { client ->
            val alice = login(client, "alice", "hunter2")
            client.put("/api/profile") {
                header(HttpHeaders.Authorization, "Bearer $alice")
                contentType(ContentType.Application.Json)
                setBody("""{"fullname":"Alice Lab","role":"admin"}""")
            }
            val metrics = client.get("/api/admin/metrics") {
                header(HttpHeaders.Authorization, "Bearer $alice")
            }
            assertThat(metrics.status).isEqualTo(HttpStatusCode.OK)
            assertThat(metrics.bodyAsText()).contains(Store.MASSASSIGN_L1)
        }

    @Test
    fun `massassign L1 hardened - role ignored, admin endpoint refuses`() =
        withApp { client ->
            SecureMode.enabled = true
            val alice = login(client, "alice", "hunter2")
            client.put("/api/profile") {
                header(HttpHeaders.Authorization, "Bearer $alice")
                contentType(ContentType.Application.Json)
                setBody("""{"fullname":"Alice Lab","role":"admin"}""")
            }
            val metrics = client.get("/api/admin/metrics") {
                header(HttpHeaders.Authorization, "Bearer $alice")
            }
            assertThat(metrics.status).isEqualTo(HttpStatusCode.Forbidden)
            assertThat(metrics.bodyAsText()).doesNotContain(Store.MASSASSIGN_L1)
        }

    // --- massassign L2 -----------------------------------------------------

    @Test
    fun `massassign L2 vuln - hidden flagAccess opens the premium vault`() =
        withApp { client ->
            val alice = login(client, "alice", "hunter2")
            client.put("/api/profile") {
                header(HttpHeaders.Authorization, "Bearer $alice")
                contentType(ContentType.Application.Json)
                setBody("""{"flagAccess":true}""")
            }
            val profile = client.get("/api/profile") { header(HttpHeaders.Authorization, "Bearer $alice") }
            assertThat(profile.bodyAsText()).contains(Store.MASSASSIGN_L2)
        }

    @Test
    fun `massassign L2 hardened - flagAccess stays server-controlled`() =
        withApp { client ->
            SecureMode.enabled = true
            val alice = login(client, "alice", "hunter2")
            client.put("/api/profile") {
                header(HttpHeaders.Authorization, "Bearer $alice")
                contentType(ContentType.Application.Json)
                setBody("""{"flagAccess":true}""")
            }
            val profile = client.get("/api/profile") { header(HttpHeaders.Authorization, "Bearer $alice") }
            assertThat(profile.bodyAsText()).doesNotContain(Store.MASSASSIGN_L2)
        }

    // --- massassign L3 -----------------------------------------------------

    @Test
    fun `massassign L3 vuln - nested prefs object binds flagAccess`() =
        withApp { client ->
            val alice = login(client, "alice", "hunter2")
            val account = client.post("/api/account") {
                header(HttpHeaders.Authorization, "Bearer $alice")
                contentType(ContentType.Application.Json)
                setBody("""{"fullname":"Alice Lab","prefs":{"flagAccess":true}}""")
            }
            assertThat(account.status).isEqualTo(HttpStatusCode.OK)
            assertThat(account.bodyAsText()).contains(Store.MASSASSIGN_L3)
        }

    @Test
    fun `massassign L3 hardened - nested prefs never decoded`() =
        withApp { client ->
            SecureMode.enabled = true
            val alice = login(client, "alice", "hunter2")
            val account = client.post("/api/account") {
                header(HttpHeaders.Authorization, "Bearer $alice")
                contentType(ContentType.Application.Json)
                setBody("""{"fullname":"Alice Lab","prefs":{"flagAccess":true}}""")
            }
            assertThat(account.status).isEqualTo(HttpStatusCode.OK)
            assertThat(account.bodyAsText()).doesNotContain(Store.MASSASSIGN_L3)
        }

    // --- massassign L4 -----------------------------------------------------

    @Test
    fun `massassign L4 vuln - user listing over-exposes internal fields`() =
        withApp { client ->
            val alice = login(client, "alice", "hunter2")
            val response = client.get("/api/users") { header(HttpHeaders.Authorization, "Bearer $alice") }
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            assertThat(response.bodyAsText()).contains(Store.MASSASSIGN_L4)
        }

    @Test
    fun `massassign L4 hardened - minimal summary for admins only`() =
        withApp { client ->
            SecureMode.enabled = true
            val alice = login(client, "alice", "hunter2")
            val denied = client.get("/api/users") { header(HttpHeaders.Authorization, "Bearer $alice") }
            assertThat(denied.status).isEqualTo(HttpStatusCode.Forbidden)
            val svcadmin = login(client, "svcadmin", "123456")
            val allowed = client.get("/api/users") { header(HttpHeaders.Authorization, "Bearer $svcadmin") }
            assertThat(allowed.status).isEqualTo(HttpStatusCode.OK)
            assertThat(allowed.bodyAsText()).doesNotContain(Store.MASSASSIGN_L4)
        }
}
