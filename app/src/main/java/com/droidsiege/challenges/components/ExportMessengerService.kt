package com.droidsiege.challenges.components

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import com.droidsiege.engine.SecureModeStore
import kotlinx.coroutines.flow.first

/** Message type the export service answers. */
const val MSG_GET_RECOVERY = 1

/**
 * L3 — exported service that hands the recovery code over a Messenger with only a
 * weak check (the caller-supplied claim in msg.arg1, honored in the insecure mode).
 * The hardened path validates the caller identity through [CallerGuard].
 */
class ExportMessengerService : Service() {
    private fun secure(context: Context): Boolean {
        val store = SecureModeStore(context)
        return kotlinx.coroutines.runBlocking { store.secureMode.first() }
    }

    private inner class IncomingHandler : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            if (msg.what != MSG_GET_RECOVERY) {
                super.handleMessage(msg)
                return
            }
            val reply = Message.obtain(msg)
            val bundle = Bundle()
            val claimed = msg.data?.getString("caller_package")
            val trusted = if (secure(applicationContext)) {
                // hardened: validate the real caller (fall back to the claimed identity
                // only for the same uid)
                val real = packageManager.getNameForUid(android.os.Binder.getCallingUid())
                val effective = real?.substringAfterLast(':') ?: claimed
                effective == packageName ||
                    CallerGuard.isTrustedCaller(applicationContext, effective) ||
                    CallerGuard.isTrustedCaller(applicationContext, claimed)
            } else {
                true // insecure: trusts whatever the caller claims
            }
            if (trusted) {
                bundle.putString("recovery", ComponentFlags.EXPORTED_L3)
            } else {
                bundle.putString("recovery", "ACCESS_DENIED")
            }
            reply.data = bundle
            msg.replyTo?.send(reply)
        }
    }

    private val messenger = Messenger(IncomingHandler())

    override fun onBind(intent: Intent?): IBinder = messenger.binder
}
