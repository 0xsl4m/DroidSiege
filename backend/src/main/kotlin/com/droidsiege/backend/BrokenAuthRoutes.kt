package com.droidsiege.backend

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.security.SecureRandom

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class TokenDto(val token: String, val role: String)

@Serializable
data class ForgotRequest(val username: String)

@Serializable
data class ResetRequest(val username: String, val token: String, val newPassword: String)

private const val LOCKOUT_THRESHOLD = 5

/**
 * Broken authentication family — predictable tokens (L1), weak/absent JWT
 * signature checks (L2), no login rate limiting (L3), guessable non-expiring
 * reset tokens (L4).
 */
fun Route.routeBrokenAuth() {
    brokenAuthLogin()
    brokenAuthWhoami()
    brokenAuthIssueJwt()
    brokenAuthVerifyJwt()
    brokenAuthForgot()
    brokenAuthReset()
}

/** Login; the vulnerable mode issues predictable tokens and never locks out. */
private fun Route.brokenAuthLogin() =
    post("/api/auth/login") {
        val body = call.receive<LoginRequest>()

        if (SecureMode.enabled && (Store.loginFails[body.username] ?: 0) >= LOCKOUT_THRESHOLD) {
            call.respond(
                HttpStatusCode.TooManyRequests,
                buildJsonObject {
                    put("error", "account locked — too many failed attempts")
                },
            )
            return@post
        }

        val user = Store.userByName(body.username)
        if (user != null && user.role == "system") {
            // the BOLA victim is a service account: refusing it in BOTH modes keeps
            // the idor flags off any path the player can authenticate into
            call.respond(HttpStatusCode.Forbidden, ErrorDto("service accounts cannot log in"))
            return@post
        }
        if (user == null || user.password != body.password) {
            Store.loginFails.merge(body.username, 1, Int::plus)
            call.respond(HttpStatusCode.Unauthorized, ErrorDto("bad credentials"))
            return@post
        }

        Store.loginFails.remove(body.username)
        val token = issueToken(user.id)
        Store.sessions[token] = user.id

        // L3 reveal: the brute-forced svcadmin account proves no lockout existed.
        // In hardened mode the lockout above makes the brute force impossible.
        if (!SecureMode.enabled && body.username == "svcadmin") {
            call.respond(
                buildJsonObject {
                    put("token", token)
                    put("role", user.role)
                    put("flag", Store.BROKENAUTH_L3)
                    put("note", "login after brute-forceable password — no rate limit, no lockout")
                },
            )
        } else {
            call.respond(TokenDto(token, user.role))
        }
    }

private fun issueToken(userId: Int): String =
    if (SecureMode.enabled) {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        "sess-" + bytes.joinToString("") { "%02x".format(it) }
    } else {
        Store.predictableToken(userId)
    }

/**
 * L1 reveal — a never-issued but correctly predicted sequential token
 * authenticates in the vulnerable mode.
 */
private fun Route.brokenAuthWhoami() =
    get("/api/auth/whoami") {
        val bearer = call.request.headers["Authorization"]
        val user = Store.callerId(bearer)?.let { Store.user(it) }
        if (user == null) {
            call.respond(HttpStatusCode.Unauthorized, ErrorDto("invalid token"))
            return@get
        }
        call.respond(
            buildJsonObject {
                put("user", user.username)
                put("role", user.role)
                val issued = bearer?.removePrefix("Bearer ")?.let { Store.sessions.containsKey(it) } == true
                if (!SecureMode.enabled && !issued) {
                    put("flag", Store.BROKENAUTH_L1)
                    put("note", "authenticated with a predicted, never-issued token")
                }
            },
        )
    }

/** Issues an HS256 token — weak secret in the vulnerable mode, strong when hardened. */
private fun Route.brokenAuthIssueJwt() =
    post("/api/auth/jwt") {
        val caller = Store.callerId(call.request.headers["Authorization"])
            ?: return@post call.respond(HttpStatusCode.Unauthorized, ErrorDto("login first"))
        val user = Store.user(caller) ?: return@post call.respond(HttpStatusCode.NotFound, ErrorDto("no such user"))
        val secret = if (SecureMode.enabled) Store.STRONG_JWT_SECRET else Store.WEAK_JWT_SECRET
        val payload = buildJsonObject {
            put("sub", user.id)
            put("username", user.username)
            put("role", user.role)
        }
        call.respond(buildJsonObject { put("token", Jwt.encode(payload.toString(), secret)) })
    }

/**
 * L2 — the vulnerable verifier accepts alg:none (no signature at all) and
 * checks HS256 tokens against the weak dictionary secret.
 */
private fun Route.brokenAuthVerifyJwt() =
    get("/api/auth/jwt/verify") {
        val token = call.request.queryParameters["token"]
            ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorDto("token query param required"))
        val decoded = Jwt.decode(token)
            ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorDto("malformed token"))

        if (SecureMode.enabled) {
            call.verifyHardened(decoded)
        } else {
            call.verifyVulnerable(decoded)
        }
    }

private suspend fun io.ktor.server.application.ApplicationCall.verifyHardened(decoded: Jwt.Decoded) {
    val expected = Jwt.sign(decoded.signingInput, Store.STRONG_JWT_SECRET)
    val algOk = Jwt.declaredAlg(decoded.headerJson) == "HS256"
    if (!algOk || decoded.providedSig != expected) {
        respond(
            HttpStatusCode.Unauthorized,
            buildJsonObject {
                put("error", "signature verification failed")
            },
        )
    } else {
        respond(
            buildJsonObject {
                put("valid", true)
                put("payload", Json.parseToJsonElement(decoded.payloadJson))
            },
        )
    }
}

private suspend fun io.ktor.server.application.ApplicationCall.verifyVulnerable(decoded: Jwt.Decoded) {
    when (Jwt.declaredAlg(decoded.headerJson)) {
        "none" -> respond(
            buildJsonObject {
                put("valid", true)
                put("alg", "none")
                put("payload", Json.parseToJsonElement(decoded.payloadJson))
                put("flag", Store.BROKENAUTH_L2)
                put("note", "unsigned token accepted — alg:none forgery verified")
            },
        )
        "HS256" -> verifyWeakSecret(decoded)
        else -> respond(HttpStatusCode.Unauthorized, ErrorDto("unsupported alg"))
    }
}

private suspend fun io.ktor.server.application.ApplicationCall.verifyWeakSecret(decoded: Jwt.Decoded) {
    val expected = Jwt.sign(decoded.signingInput, Store.WEAK_JWT_SECRET)
    if (decoded.providedSig == expected) {
        respond(
            buildJsonObject {
                put("valid", true)
                put("alg", "HS256")
                put("payload", Json.parseToJsonElement(decoded.payloadJson))
                put("flag", Store.BROKENAUTH_L2)
                put("note", "verified with weak secret — forge any payload")
            },
        )
    } else {
        respond(HttpStatusCode.Unauthorized, ErrorDto("bad signature"))
    }
}

/** L4 step 1 — the reset token is stored, never revealed to the caller. */
private fun Route.brokenAuthForgot() =
    post("/api/auth/forgot") {
        val body = call.receive<ForgotRequest>()
        Store.resetTokens[body.username] =
            if (!SecureMode.enabled) {
                // legacy lab mode: the deterministic token is derivable, never expires
                Store.ResetToken(value = Store.legacyResetToken(body.username), issuedAtMs = 0)
            } else {
                val bytes = ByteArray(16)
                SecureRandom().nextBytes(bytes)
                Store.ResetToken(
                    value = bytes.joinToString("") { "%02x".format(it) },
                    issuedAtMs = System.currentTimeMillis(),
                )
            }
        call.respond(buildJsonObject { put("result", "if the account exists, a reset link was sent") })
    }

/** L4 step 2 — guessed tokens reset the password in the vulnerable mode. */
private fun Route.brokenAuthReset() =
    post("/api/auth/reset") {
        val body = call.receive<ResetRequest>()
        val user = Store.userByName(body.username)
        val stored = Store.resetTokens[body.username]
        if (user == null || stored == null) {
            call.respond(HttpStatusCode.BadRequest, ErrorDto("invalid reset request"))
            return@post
        }
        if (!SecureMode.enabled) {
            if (body.token == stored.value) {
                user.password = body.newPassword
                call.respond(
                    buildJsonObject {
                        put("result", "password reset")
                        put("flag", Store.BROKENAUTH_L4)
                        put("note", "reset token guessed — deterministic and never expires")
                    },
                )
            } else {
                call.respond(HttpStatusCode.BadRequest, ErrorDto("invalid token"))
            }
        } else {
            val expired = System.currentTimeMillis() - stored.issuedAtMs > 15 * 60 * 1000
            if (body.token != stored.value || expired) {
                call.respond(HttpStatusCode.BadRequest, ErrorDto("invalid or expired token"))
            } else {
                Store.resetTokens.remove(body.username)
                user.password = body.newPassword
                call.respond(buildJsonObject { put("result", "password reset") })
            }
        }
    }
