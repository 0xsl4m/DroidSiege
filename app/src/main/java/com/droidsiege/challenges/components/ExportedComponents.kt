package com.droidsiege.challenges.components

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import com.droidsiege.engine.SecureModeStore
import kotlinx.coroutines.flow.first

/** Flags for the exported-component tiers. */
object ComponentFlags {
    const val EXPORTED_L1 = "DS{components_exported_L1_5c9e14}"
    const val EXPORTED_L2 = "DS{components_exported_L2_7d2f68}"
    const val EXPORTED_L3 = "DS{components_exported_L3_94ba30}"
    const val EXPORTED_L4 = "DS{components_exported_L4_c1e75b}"
    const val REDIRECT_L1 = "DS{components_intentredir_L1_b3d670}"
    const val REDIRECT_L2 = "DS{components_intentredir_L2_25f8c4}"
    const val REDIRECT_L3 = "DS{components_intentredir_L3_71a3e9}"
    const val REDIRECT_L4 = "DS{components_intentredir_L4_d84c16}"
}

private const val MASTER_KEY = "siege-master-key-2026"
private const val CHAIN_PREFS = "siege_chain_prefs"

internal fun chainArmed(context: Context): Boolean =
    context.getSharedPreferences(CHAIN_PREFS, Context.MODE_PRIVATE).getBoolean("armed", false)

internal fun armChain(
    context: Context,
    armed: Boolean,
) {
    context.getSharedPreferences(CHAIN_PREFS, Context.MODE_PRIVATE)
        .edit().putBoolean("armed", armed).commit()
}

/** Plain screen helper for the manifest components (no Compose — these launch cold). */
internal fun renderScreen(
    activity: Activity,
    title: String,
    body: String,
    secret: String?,
    denied: Boolean = false,
) {
    val context = activity
    val pad = (16 * context.resources.displayMetrics.density).toInt()
    val layout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(pad, pad, pad, pad)
        setBackgroundColor(Color.rgb(15, 23, 42))
    }

    fun text(
        value: String,
        size: Float,
        color: Int,
        mono: Boolean = false,
    ): TextView =
        TextView(context).apply {
            text = value
            textSize = size
            setTextColor(color)
            setPadding(0, pad / 2, 0, pad / 2)
            if (mono) typeface = android.graphics.Typeface.MONOSPACE
        }
    layout.addView(text(title, 20f, Color.WHITE))
    layout.addView(text(body, 15f, Color.rgb(203, 213, 225)))
    when {
        denied -> layout.addView(text("⛔ caller not trusted — refused", 18f, Color.rgb(248, 113, 113), mono = true))
        secret != null -> layout.addView(text(secret, 18f, Color.rgb(74, 222, 128), mono = true))
    }
    activity.setContentView(layout)
}

internal fun secureMode(context: Context): Boolean {
    val store = SecureModeStore(context.applicationContext)
    return kotlinx.coroutines.runBlocking {
        store.secureMode.first()
    }
}

/** L1 — exported activity that renders the recovery flag to any launcher. */
class ExportFlagActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val secure = secureMode(this)
        if (secure) {
            val fromTrusted = CallerGuard.isTrustedCaller(this, callingPackage)
            renderScreen(
                this,
                "Recovery portal",
                if (fromTrusted) {
                    "Trusted internal caller."
                } else {
                    "Hardened: external launches are refused. This component should not be exported."
                },
                secret = if (fromTrusted) ComponentFlags.EXPORTED_L1 else null,
                denied = !fromTrusted,
            )
            return
        }
        renderScreen(
            this,
            "Recovery portal",
            "Session recovered for this device. Keep this code safe:",
            secret = ComponentFlags.EXPORTED_L1,
        )
    }
}

/** L2 — exported activity gated by a specific intent extra. */
class KeyedExportActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val secure = secureMode(this)
        val supplied = intent.getStringExtra("master_key")
        if (secure && !CallerGuard.isTrustedCaller(this, callingPackage)) {
            renderScreen(this, "Keyed export", "Caller validation failed.", secret = null, denied = true)
            return
        }
        if (supplied == MASTER_KEY) {
            renderScreen(
                this,
                "Keyed export",
                "Master key accepted. Vault recovery code:",
                secret = ComponentFlags.EXPORTED_L2,
            )
        } else {
            renderScreen(
                this,
                "Keyed export",
                "Provide the master_key intent extra to unlock the vault.",
                secret = null,
            )
        }
    }
}

/** L4 — exported receiver that arms the chain state from any broadcast. */
class ChainReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: android.content.Intent,
    ) {
        if (intent.action != "com.droidsiege.CHAIN_ARM") return
        val secure = secureMode(context)
        val senderId = intent.getStringExtra("sender_id")
        // hardened path: only broadcasts carrying the app's own sender id are honored
        if (secure && senderId != "droidsiege-internal") return
        armChain(context, intent.getBooleanExtra("arm", true))
    }
}

/** L4 — exported activity that exposes the flag only while the chain is armed. */
class ChainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val secure = secureMode(this)
        val armed = chainArmed(this)
        when {
            !armed -> renderScreen(
                this,
                "Escrow chain",
                "Chain disarmed. Arm it first (see the receiver).",
                secret = null,
            )
            secure && !CallerGuard.isTrustedCaller(this, callingPackage) -> renderScreen(
                this,
                "Escrow chain",
                "Caller validation failed.",
                secret = null,
                denied = true,
            )
            else -> renderScreen(
                this,
                "Escrow chain",
                "Chain armed. Escrow code:",
                secret = ComponentFlags.EXPORTED_L4,
            )
        }
    }
}
