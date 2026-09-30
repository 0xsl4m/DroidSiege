package com.droidsiege.backend

import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import kotlin.text.Charsets.UTF_8

data class User(
    val id: Int,
    val username: String,
    var password: String,
    var role: String,
    val secret: String,
    var fullname: String,
    var isPremium: Boolean,
    var flagAccess: Boolean,
    var vaultSync: Boolean,
    var note: String,
)

data class Order(
    val id: Int,
    val ownerId: Int,
    val manifest: String,
)

data class Item(
    val id: Int,
    val orderId: Int,
    val label: String,
    val flag: String,
)

/**
 * In-memory seed store. The only "secrets" present are the intended challenge
 * flags; everything else is lab data. Every loginable account starts
 * unprivileged, and every flag sits on an object whose owner either cannot log
 * in (the `system` account, role-gated out of /api/auth/login) or is reachable
 * only through the family's exploit — so hardened mode leaves no flag
 * reachable. [reset] restores the pristine seed so tests start known.
 */
object Store {
    // --- idor flags -------------------------------------------------------
    const val IDOR_L1 = "DS{backend_idor_L1_c39a17}"
    const val IDOR_L2 = "DS{backend_idor_L2_5f8e02}"
    const val IDOR_L3 = "DS{backend_idor_L3_e1d4c8}"
    const val IDOR_L4 = "DS{backend_idor_L4_a7b93f}"

    // --- brokenauth flags -------------------------------------------------
    const val BROKENAUTH_L1 = "DS{backend_brokenauth_L1_d20f6b}"
    const val BROKENAUTH_L2 = "DS{backend_brokenauth_L2_94c5e7}"
    const val BROKENAUTH_L3 = "DS{backend_brokenauth_L3_6a81d3}"
    const val BROKENAUTH_L4 = "DS{backend_brokenauth_L4_f53b19}"

    // --- massassign flags -------------------------------------------------
    const val MASSASSIGN_L1 = "DS{backend_massassign_L1_7c2e58}"
    const val MASSASSIGN_L2 = "DS{backend_massassign_L2_b94d70}"
    const val MASSASSIGN_L3 = "DS{backend_massassign_L3_3e6fa4}"
    const val MASSASSIGN_L4 = "DS{backend_massassign_L4_08d1c6}"

    const val WEAK_JWT_SECRET = "secret"
    const val STRONG_JWT_SECRET = "droidsiege-hardened-lab-key-4f9a2c7e1b8d5306"

    /**
     * The BOLA victim: a `system` account whose password is never disclosed and
     * which /api/auth/login refuses in both modes. The idor L1/L2/L4 flags live
     * on its objects, so the only access path is the exploit itself.
     */
    const val SYSTEM_USER_ID = 5
    const val SYSTEM_ORDER_ID = 103
    const val SYSTEM_ITEM_ID = 503
    const val SYSTEM_VAULT_ID = 7

    val users = LinkedHashMap<Int, User>()
    val orders = LinkedHashMap<Int, Order>()
    val items = LinkedHashMap<Int, Item>()

    val sessions = ConcurrentHashMap<String, Int>()
    val loginFails = ConcurrentHashMap<String, Int>()
    val resetTokens = ConcurrentHashMap<String, ResetToken>()

    data class ResetToken(val value: String, val issuedAtMs: Long)

    fun reset() {
        synchronized(users) {
            users.clear()
            users[1] = User(
                id = 1,
                username = "alice",
                password = "hunter2",
                role = "user",
                secret = "internal record 1",
                fullname = "Alice Lab",
                isPremium = false,
                flagAccess = false,
                vaultSync = false,
                note = "alice note v1",
            )
            users[2] = User(
                id = 2,
                username = "bob",
                password = "password1",
                role = "user",
                secret = "internal record 2",
                fullname = "Bob Lab",
                isPremium = false,
                flagAccess = false,
                vaultSync = false,
                note = "bob note v1",
            )
            users[3] = User(
                id = 3,
                username = "carol",
                password = "correct-horse",
                role = "user",
                secret = "internal record 3",
                fullname = "Carol Lab",
                isPremium = false,
                flagAccess = false,
                vaultSync = false,
                note = "carol note v1",
            )
            users[SYSTEM_USER_ID] = User(
                id = SYSTEM_USER_ID,
                username = "vaultkeeper",
                password = "b7f3e9a1c2d84f6e0a5d3c8b1e7f4a2c",
                role = "system",
                secret = IDOR_L1,
                fullname = "Records Vault",
                isPremium = false,
                flagAccess = false,
                vaultSync = false,
                note = "keeper note v1",
            )
            users[9] = User(
                id = 9,
                username = "svcadmin",
                password = "123456",
                role = "admin",
                secret = "internal record 9",
                fullname = "Service Admin",
                isPremium = false,
                flagAccess = false,
                vaultSync = false,
                note = "svcadmin note v1",
            )
        }
        synchronized(orders) {
            orders.clear()
            orders[101] = Order(101, ownerId = 1, manifest = "alice order")
            orders[102] = Order(102, ownerId = 2, manifest = "bob order")
            orders[SYSTEM_ORDER_ID] = Order(SYSTEM_ORDER_ID, ownerId = SYSTEM_USER_ID, manifest = "keeper escrow")
        }
        synchronized(items) {
            items.clear()
            items[501] = Item(501, orderId = 101, label = "alice widget", flag = "item-alice-ok")
            items[502] = Item(502, orderId = 102, label = "bob widget", flag = "item-bob-ok")
            items[SYSTEM_ITEM_ID] = Item(
                SYSTEM_ITEM_ID,
                orderId = SYSTEM_ORDER_ID,
                label = "keeper escrow",
                flag = IDOR_L4,
            )
        }
        sessions.clear()
        loginFails.clear()
        resetTokens.clear()
    }

    fun user(id: Int): User? = users[id]

    fun userByName(username: String): User? = users.values.firstOrNull { it.username == username }

    /**
     * Resolves the caller from a bearer token. Hardened mode trusts only the
     * issued-session map; the vulnerable mode also accepts any derivable
     * sequential token ("sess-1000+userId"), so predicted tokens authenticate.
     */
    fun callerId(bearer: String?): Int? {
        val token = bearer?.removePrefix("Bearer ") ?: return null
        sessions[token]?.let { return it }
        if (!SecureMode.enabled) {
            val match = Regex("sess-(\\d+)").matchEntire(token) ?: return null
            val id = match.groupValues[1].toIntOrNull()?.minus(1000) ?: return null
            if (users.containsKey(id)) return id
        }
        return null
    }

    /** The predictable login token: sess-1000+userId, identical for every login. */
    fun predictableToken(userId: Int): String = "sess-" + (1000 + userId)

    /** The L2 "hashid": salted digest over a tiny numeric space — enumerable by design. */
    fun hidFor(id: Int): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(("siege$id").toByteArray(UTF_8))
        val alphabet = "abcdefghijklmnopqrstuvwxyz234567"
        return buildString {
            for (b in digest.copyOf(8)) {
                append(alphabet[b.toInt() and 0xFF and 0x1F])
            }
        }
    }

    fun vaultOwner(vaultId: Int): Int? =
        when (vaultId) {
            SYSTEM_VAULT_ID -> SYSTEM_USER_ID
            1 -> 1
            else -> null
        }

    fun vaultContents(vaultId: Int): String? =
        when (vaultId) {
            SYSTEM_VAULT_ID -> IDOR_L2
            1 -> "team standup notes"
            else -> null
        }

    /** L4 vuln reset token: deterministic md5(username + fixed salt), no expiry. */
    fun legacyResetToken(username: String): String {
        val digest = MessageDigest.getInstance("MD5").digest(("droidsiege$username").toByteArray(UTF_8))
        return digest.joinToString("") { "%02x".format(it) }.take(8)
    }

    init {
        reset()
    }
}
