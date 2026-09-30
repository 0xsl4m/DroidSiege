package com.droidsiege.backend

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import kotlinx.serialization.Serializable

@Serializable
data class ProfileUpdate(
    val fullname: String? = null,
    val role: String? = null,
    val isPremium: Boolean? = null,
    val flagAccess: Boolean? = null,
)

@Serializable
data class ProfileDto(
    val id: Int,
    val username: String,
    val fullname: String,
    val role: String,
    val isPremium: Boolean,
    val flagAccess: Boolean,
    val premiumVault: String = "",
)

@Serializable
data class AccountEnvelope(
    val fullname: String? = null,
    val prefs: PrefsFields? = null,
)

/** The nested object only the L3 exploit binds — deliberately distinct from L2's fields. */
@Serializable
data class PrefsFields(val vaultSync: Boolean? = null)

@Serializable
data class AccountDto(
    val id: Int,
    val username: String,
    val fullname: String,
    val flagAccess: Boolean,
    val vaultSync: Boolean,
    val secureNote: String = "",
)

@Serializable
data class MetricsDto(
    val requestsPerDay: Int = 4213,
    val activeUsers: Int = 3,
    val flag: String = "",
)

@Serializable
data class UserSummaryDto(val id: Int, val username: String, val fullname: String)

@Serializable
data class UserExposureDto(
    val id: Int,
    val username: String,
    val fullname: String,
    val role: String,
    val internalFlag: String,
    val secret: String,
)

/**
 * Mass assignment family — client-controlled `role` (L1), hidden premium/flag
 * fields (L2), nested-object binding (L3), over-exposed list responses (L4).
 */
fun Route.routeMassAssign() {
    massAssignPutProfile()
    massAssignGetProfile()
    massAssignAdminMetrics()
    massAssignPostAccount()
    massAssignGetAccount()
    massAssignListUsers()
}

/** L1/L2 — PUT /profile binds every field the JSON carries, including role/flags. */
private fun Route.massAssignPutProfile() =
    put("/api/profile") {
        val user = call.callerOrRespond() ?: return@put
        val body = call.receive<ProfileUpdate>()
        // vulnerable: every optional field is bound straight onto the record
        body.fullname?.let { user.fullname = it }
        if (!SecureMode.enabled) {
            body.role?.let { user.role = it }
            body.isPremium?.let { user.isPremium = it }
            body.flagAccess?.let { user.flagAccess = it }
        }
        // hardened path: fullname is the only client-writable field
        call.respond(profileDto(user))
    }

/** L2 — the premium vault opens for whoever carries flagAccess/premium. */
private fun Route.massAssignGetProfile() =
    get("/api/profile") {
        val user = call.callerOrRespond() ?: return@get
        call.respond(profileDto(user))
    }

/** L1 proof endpoint — admin metrics the elevated user can now read. */
private fun Route.massAssignAdminMetrics() =
    get("/api/admin/metrics") {
        val user = call.callerOrRespond() ?: return@get
        if (user.role != "admin") {
            call.respond(HttpStatusCode.Forbidden, ErrorDto("admin role required"))
            return@get
        }
        call.respond(MetricsDto(flag = if (SecureMode.enabled) "" else Store.MASSASSIGN_L1))
    }

/** L3 — nested-object mass assignment: the envelope binds prefs.flagAccess. */
private fun Route.massAssignPostAccount() =
    post("/api/account") {
        val user = call.callerOrRespond() ?: return@post
        if (!SecureMode.enabled) {
            val envelope = call.receive<AccountEnvelope>()
            envelope.fullname?.let { user.fullname = it }
            envelope.prefs?.vaultSync?.let { user.vaultSync = it }
        } else {
            // hardened: flat explicit DTO only — nested prefs never decoded
            val flat = call.receive<ProfileUpdate>()
            flat.fullname?.let { user.fullname = it }
        }
        call.respond(accountDto(user))
    }

private fun Route.massAssignGetAccount() =
    get("/api/account") {
        val user = call.callerOrRespond() ?: return@get
        call.respond(accountDto(user))
    }

/** L4 — the user listing over-exposes internal fields to any caller. */
private fun Route.massAssignListUsers() =
    get("/api/users") {
        val caller = Store.callerId(call.request.headers["Authorization"])
            ?: return@get call.respond(HttpStatusCode.Unauthorized, ErrorDto("login first"))
        if (SecureMode.enabled) {
            val admin = Store.user(caller)?.role == "admin"
            if (!admin) {
                call.respond(HttpStatusCode.Forbidden, ErrorDto("admin role required"))
                return@get
            }
            call.respond(Store.users.values.map { UserSummaryDto(it.id, it.username, it.fullname) })
        } else {
            call.respond(Store.users.values.map { user -> exposureDto(user) })
        }
    }

/**
 * Resolves the bearer caller into its User; null means a 401/404 was already
 * sent.
 */
private suspend fun io.ktor.server.application.ApplicationCall.callerOrRespond(): User? {
    val caller = Store.callerId(request.headers["Authorization"])
    if (caller == null) {
        respond(HttpStatusCode.Unauthorized, ErrorDto("login first"))
        return null
    }
    val user = Store.user(caller)
    if (user == null) {
        respond(HttpStatusCode.NotFound, ErrorDto("no such user"))
        return null
    }
    return user
}

private fun profileDto(user: User): ProfileDto =
    ProfileDto(
        id = user.id,
        username = user.username,
        fullname = user.fullname,
        role = user.role,
        isPremium = user.isPremium,
        flagAccess = user.flagAccess,
        premiumVault = if (user.isPremium || user.flagAccess) Store.MASSASSIGN_L2 else "",
    )

private fun accountDto(user: User): AccountDto =
    AccountDto(
        id = user.id,
        username = user.username,
        fullname = user.fullname,
        flagAccess = user.flagAccess,
        vaultSync = user.vaultSync,
        secureNote = if (user.vaultSync) Store.MASSASSIGN_L3 else "",
    )

private fun exposureDto(user: User): UserExposureDto =
    UserExposureDto(
        id = user.id,
        username = user.username,
        fullname = user.fullname,
        role = user.role,
        internalFlag = Store.MASSASSIGN_L4,
        secret = "internal record ${user.id}",
    )
