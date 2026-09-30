package com.droidsiege.backend

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import kotlinx.serialization.Serializable

@Serializable
data class SecretDto(val id: Int, val owner: String, val secret: String)

@Serializable
data class VaultDto(val vault: Int, val contents: String)

@Serializable
data class NoteDto(val id: Int, val owner: String, val note: String)

@Serializable
data class NoteUpdate(val note: String)

@Serializable
data class ItemDto(val id: Int, val order: Int, val label: String, val flag: String)

@Serializable
data class ErrorDto(val error: String)

/**
 * BOLA / IDOR family — four escalation tiers, each with the vulnerable behavior
 * under SECURE_MODE=off and the ownership-enforcing behavior under SECURE_MODE=on.
 */
fun Route.routeIdor() {
    idorL1Secret()
    idorL2Vault()
    idorL3GetNote()
    idorL3PutNote()
    idorL3DeleteNote()
    idorL4OrderItem()
}

/** L1 — no ownership check at all: any id answers with its secret. */
private fun Route.idorL1Secret() =
    get("/api/users/{id}/secret") {
        val id = call.parameters["id"]?.toIntOrNull()
        val user = id?.let { Store.user(it) }
        when {
            user == null -> call.respond(HttpStatusCode.NotFound, ErrorDto("no such user"))
            !SecureMode.enabled -> call.respond(SecretDto(user.id, user.username, user.secret))
            else -> {
                val caller = Store.callerId(call.request.headers["Authorization"])
                if (caller == user.id) {
                    call.respond(SecretDto(user.id, user.username, user.secret))
                } else {
                    call.respond(HttpStatusCode.Forbidden, ErrorDto("object does not belong to caller"))
                }
            }
        }
    }

/**
 * L2 — object ids obfuscated with a salted digest over a tiny numeric space;
 * the vulnerable server accepts any enumerated hid without authorizing it.
 */
private fun Route.idorL2Vault() =
    get("/api/vaults/{hid}") {
        val hid = call.parameters["hid"] ?: ""
        val vaultId = (0..64).firstOrNull { Store.hidFor(it) == hid }
        when {
            vaultId == null -> call.respond(HttpStatusCode.NotFound, ErrorDto("unknown vault"))
            !SecureMode.enabled -> call.respond(VaultDto(vaultId, Store.vaultContents(vaultId) ?: "empty"))
            else -> {
                val caller = Store.callerId(call.request.headers["Authorization"])
                if (caller != null && Store.vaultOwner(vaultId) == caller) {
                    call.respond(VaultDto(vaultId, Store.vaultContents(vaultId) ?: "empty"))
                } else {
                    call.respond(HttpStatusCode.Forbidden, ErrorDto("vault does not belong to caller"))
                }
            }
        }
    }

/** L3 read side — ownership enforced in BOTH modes (the family's premise). */
private fun Route.idorL3GetNote() =
    get("/api/notes/{id}") {
        val (user, caller) = call.noteOwnerOrRespond() ?: return@get
        if (caller == user.id) {
            call.respond(NoteDto(user.id, user.username, user.note))
        } else {
            call.respond(HttpStatusCode.Forbidden, ErrorDto("note belongs to another user"))
        }
    }

/** L3 — the write verb skips the ownership check the GET performs. */
private fun Route.idorL3PutNote() =
    put("/api/notes/{id}") {
        val (user, caller) = call.noteOwnerOrRespond() ?: return@put
        if (SecureMode.enabled && caller != user.id) {
            call.respond(HttpStatusCode.Forbidden, ErrorDto("note belongs to another user"))
            return@put
        }
        val body = call.receive<NoteUpdate>()
        user.note = body.note
        call.respond(
            if (SecureMode.enabled) {
                NoteDto(user.id, user.username, user.note)
            } else {
                NoteDto(user.id, user.username, Store.IDOR_L3)
            },
        )
    }

/** L3 — the delete verb has the same missing ownership check. */
private fun Route.idorL3DeleteNote() =
    delete("/api/notes/{id}") {
        val (user, caller) = call.noteOwnerOrRespond() ?: return@delete
        if (SecureMode.enabled && caller != user.id) {
            call.respond(HttpStatusCode.Forbidden, ErrorDto("note belongs to another user"))
            return@delete
        }
        user.note = "(deleted)"
        call.respond(
            if (SecureMode.enabled) {
                NoteDto(user.id, user.username, user.note)
            } else {
                NoteDto(user.id, user.username, Store.IDOR_L3)
            },
        )
    }

/**
 * L4 — nested resource: the order is authorized, the item is looked up globally
 * without checking that it belongs to the order on the path.
 */
private fun Route.idorL4OrderItem() =
    get("/api/orders/{oid}/items/{iid}") {
        val oid = call.parameters["oid"]?.toIntOrNull()
        val iid = call.parameters["iid"]?.toIntOrNull()
        val order = oid?.let { Store.orders[it] }
        val item = iid?.let { Store.items[it] }
        if (order == null || item == null) {
            call.respond(HttpStatusCode.NotFound, ErrorDto("no such order or item"))
            return@get
        }
        val caller = Store.callerId(call.request.headers["Authorization"])
        val ownsOrder = caller != null && order.ownerId == caller
        // the vulnerable gap: order ownership verified, item taken globally
        val itemOnPath = !SecureMode.enabled || item.orderId == order.id
        when {
            ownsOrder && itemOnPath -> call.respond(ItemDto(item.id, order.id, item.label, item.flag))
            ownsOrder -> call.respond(HttpStatusCode.Forbidden, ErrorDto("full-path authorization failed"))
            else -> call.respond(HttpStatusCode.Forbidden, ErrorDto("order does not belong to caller"))
        }
    }

/**
 * Resolves the note target and the caller for the /api/notes/{id} handlers;
 * null means a response was already sent (bad id, unknown note).
 */
private suspend fun io.ktor.server.application.ApplicationCall.noteOwnerOrRespond(): Pair<User, Int?>? {
    val id = parameters["id"]?.toIntOrNull()
    if (id == null) {
        respond(HttpStatusCode.BadRequest, ErrorDto("bad id"))
        return null
    }
    val user = Store.user(id)
    if (user == null) {
        respond(HttpStatusCode.NotFound, ErrorDto("no such note"))
        return null
    }
    return user to Store.callerId(request.headers["Authorization"])
}
