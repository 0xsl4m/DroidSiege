package com.droidsiege.challenges.webview

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.droidsiege.challenges.common.ChallengeConsole
import com.droidsiege.challenges.common.TieredChallenge
import com.droidsiege.core.Difficulty
import com.droidsiege.core.LearnContent
import java.io.File

private const val INJECT_FLAG = "DS{webview_inject_L1_9e27b4}"
private const val TRAVERSE_FLAG = "DS{webview_inject_L2_6c3f81}"
private const val FORMAT_FLAG = "DS{webview_inject_L3_4a98d2}"
private const val PARSER_FLAG = "DS{webview_inject_L4_b8516e}"

private const val SECRET_POOL = "user=user1;user=user2;secret=DS{webview_inject_L4_b8516e};"

private class InjectNotesDb(context: Context) :
    SQLiteOpenHelper(context, "inject_notes.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE notes(tag TEXT PRIMARY KEY, body TEXT)")
        db.execSQL("INSERT INTO notes VALUES('welcome', 'welcome to the notes vault')")
        db.execSQL("INSERT INTO notes VALUES('recovery', 'DS{webview_inject_L1_9e27b4}')")
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int,
    ) = Unit
}

object InjectLab {
    private var db: InjectNotesDb? = null

    private fun database(context: Context): InjectNotesDb =
        db ?: InjectNotesDb(context.applicationContext).also { db = it }

    /** L1 — string-concatenated search (insecure) vs parameterized (hardened). */
    fun search(
        @Suppress("UnusedParameter") context: Context,
        term: String,
        secure: Boolean,
    ): String {
        val cursor =
            if (secure) {
                database(context).readableDatabase.rawQuery(
                    "SELECT tag, body FROM notes WHERE body LIKE ?",
                    arrayOf("%$term%"),
                )
            } else {
                database(context).readableDatabase.rawQuery(
                    "SELECT tag, body FROM notes WHERE body LIKE '%$term%'",
                    null,
                )
            }
        return cursor.use { c ->
            val rows = mutableListOf<String>()
            while (c.moveToNext()) rows += "${c.getString(0)}: ${c.getString(1)}"
            if (rows.isEmpty()) "(no matches)" else rows.joinToString("\n")
        }
    }

    /** L2 — file open by user-supplied name inside notes/, traversal to filesDir. */
    fun openNote(
        context: Context,
        name: String,
        secure: Boolean,
    ): String {
        val base = File(context.filesDir, "notes").apply { mkdirs() }
        val file =
            if (secure) {
                val resolved = File(base, name).canonicalFile
                if (!resolved.path.startsWith(base.canonicalFile.path)) {
                    return "REFUSED: path escapes the notes directory"
                }
                resolved
            } else {
                File(base, name)
            }
        if (!file.exists()) file.writeText("(empty note)")
        return file.readText()
    }

    /** L3 — format string: the user text IS the format (insecure). */
    fun formatReceipt(
        @Suppress("UnusedParameter") context: Context,
        text: String,
        secure: Boolean,
    ): String =
        if (secure) {
            // hardened: fixed format, user text as an argument
            String.format(java.util.Locale.US, "receipt: %s", text)
        } else {
            // insecure: the user text IS the format; the secret is the formatter's
            // only vararg, so a %s in the caller text prints it
            String.format(text, FORMAT_FLAG)
        }

    /** L4 — placeholder parser that trusts an attacker-supplied length. */
    fun nativeParse(
        input: String,
        declaredLength: Int,
        secure: Boolean,
    ): String {
        // PLACEHOLDER Phase-5 native; the over-read is simulated with a pooled buffer.
        // the secret sits AFTER the user's record in the pool, so the over-read walks
        // into it when the declared length exceeds the record.
        val pool = input + SECRET_POOL
        val length =
            if (secure) {
                // hardened: clamp to the caller's own record region — the secret is
                // never inside [0, recordLength)
                input.length
            } else {
                declaredLength.coerceIn(0, pool.length)
            }
        return pool.substring(0, length)
    }
}

class InjectL1Challenge : TieredChallenge(
    category = "webview",
    slug = "inject",
    level = Difficulty.EASY,
    title = "Notes Search",
    brief = "The notes vault search takes your term and pastes it into the query. " +
        "Dump the hidden recovery row.",
    owaspRefs = listOf("M4", "MASVS-CODE-4", "MASTG-TEST-0x57"),
    hints = listOf(
        "The LIKE clause closes with '%<term>%'.",
        "Term: x%' OR '1'='1' -- dumps every row, recovery included.",
        "Secure builds parameterize the LIKE argument.",
    ),
    flag = INJECT_FLAG,
    learn = LearnContent(
        theory = "Local SQL is injectable exactly like remote SQL — string-built " +
            "queries plus attacker input equals attacker-written queries, even when " +
            "the database never leaves the device.\n\n" +
            "selectionArgs / parameterized statements everywhere, local included.",
        mastgRefs = listOf("MASVS-CODE-4", "MASTG-TEST-0x57"),
        vulnerableSnippet = "rawQuery(\"SELECT … WHERE body LIKE '%${'$'}term%'\", null)",
        fixSnippet = "rawQuery(\"SELECT … WHERE body LIKE ?\", arrayOf(\"%<term>%\"))",
        takeaway = "Local databases are attacker-readable databases.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        val context = LocalContext.current
        var term by rememberSaveable { mutableStateOf("welcome") }
        var output by remember { mutableStateOf("") }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = term,
                onValueChange = { term = it },
                label = { Text("search term") },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { output = InjectLab.search(context, term, secureMode) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Search notes") }
            MaterialTheme.typography.bodySmall
            ChallengeConsole(output)
        }
    }
}

class InjectL2Challenge : TieredChallenge(
    category = "webview",
    slug = "inject",
    level = Difficulty.MEDIUM,
    title = "Note File Open",
    brief = "Notes are stored as files and opened by name. The escrow file lives one " +
        "directory up.",
    owaspRefs = listOf("M4", "MASVS-CODE-4", "M9", "MASTG-TEST-0x57"),
    hints = listOf(
        "openNote resolves notes/<name> non-canonically.",
        "Name: ../note_secret.txt (planted on entry).",
        "Secure builds canonicalize and refuse escapes.",
    ),
    flag = TRAVERSE_FLAG,
    learn = LearnContent(
        theory = "File opens by caller-supplied name are the file-shaped version of " +
            "SQLi: ../ climbs, symlinks redirect, and the resolved path lands anywhere " +
            "in the sandbox.\n\n" +
            "Canonicalize, then verify the path still starts with the intended root — " +
            "in that order.",
        mastgRefs = listOf("MASVS-CODE-4", "MASTG-TEST-0x57"),
        vulnerableSnippet = "File(notesDir, name).readText()",
        fixSnippet = "val r = File(notesDir, name).canonicalFile\n" +
            "if (!r.path.startsWith(notesDir.canonicalPath)) refuse()",
        takeaway = "Resolve, canonicalize, verify — then open.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        val context = LocalContext.current
        androidx.compose.runtime.LaunchedEffect(Unit) {
            File(context.filesDir, "note_secret.txt").writeText(TRAVERSE_FLAG)
        }
        var name by rememberSaveable { mutableStateOf("welcome.txt") }
        var output by remember { mutableStateOf("") }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("note file name") },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { output = InjectLab.openNote(context, name, secureMode) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Open note") }
            ChallengeConsole(output)
        }
    }
}

class InjectL3Challenge : TieredChallenge(
    category = "webview",
    slug = "inject",
    level = Difficulty.HARD,
    title = "Format Receipt",
    brief = "The receipt printer formats its header from the customer text. A secret " +
        "sits in scope inside the formatter.",
    owaspRefs = listOf("M4", "MASVS-CODE-4", "MASTG-TEST-0x57"),
    hints = listOf(
        "The customer text is used AS the format string.",
        "Payload: %s — the formatter prints the next argument in scope.",
        "Secure builds pass the text as an argument to a fixed format.",
    ),
    flag = FORMAT_FLAG,
    learn = LearnContent(
        theory = "Using attacker text as a format string leaks whatever sits in the " +
            "formatter's argument scope (%s walks the args) and can crash the process " +
            "with %n-class specifiers.\n\n" +
            "The format is code. User text is an argument. Never swap the two.",
        mastgRefs = listOf("MASVS-CODE-4", "MASTG-TEST-0x57"),
        vulnerableSnippet = "String.format(userText) // user text IS the format",
        fixSnippet = "String.format(\"receipt: %s\", userText)",
        takeaway = "User text belongs in the arguments, never in the format.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        val context = LocalContext.current
        var text by rememberSaveable { mutableStateOf("coffee") }
        var output by remember { mutableStateOf("") }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("customer text") },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { output = InjectLab.formatReceipt(context, text, secureMode) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Print receipt") }
            ChallengeConsole(output)
        }
    }
}

class InjectL4Challenge : TieredChallenge(
    category = "webview",
    slug = "inject",
    level = Difficulty.INSANE,
    title = "Parser Over-read",
    brief = "The record parser trusts the caller-declared length and copies from a " +
        "pooled buffer that holds more than your record. Declare a longer length.",
    owaspRefs = listOf("M4", "MASVS-CODE-4", "M7", "MASTG-TEST-0x57"),
    hints = listOf(
        "The pool contains user records AND the secret — the read starts at offset 0.",
        "Declare a length longer than your input; the over-read walks into the secret.",
        "Secure builds clamp the declared length to the actual input.",
    ),
    flag = PARSER_FLAG,
    learn = LearnContent(
        theory = "Trusting a caller-declared length is the native over-read pattern " +
            "(Heartbleed-shaped): the buffer holds more than the record and the copy " +
            "walks past the boundary. In Kotlin this simulates what a C parser would " +
            "leak.\n\n" +
            "Bounds-check every declared length against the real buffer before any " +
            "copy — in native code, in wrappers, everywhere.",
        mastgRefs = listOf("MASVS-CODE-4", "M7", "MASTG-TEST-0x57"),
        vulnerableSnippet = "copy(buffer, declaredLength) // buffer holds more",
        fixSnippet = "val len = declaredLength.coerceAtMost(input.length)\n" +
            "copy(buffer, len)",
        takeaway = "Never trust a length the caller declares.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        var input by rememberSaveable { mutableStateOf("my-record") }
        var lengthText by rememberSaveable { mutableStateOf("9") }
        var output by remember { mutableStateOf("") }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("record") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = lengthText,
                onValueChange = { lengthText = it },
                label = { Text("declared length") },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    output = InjectLab.nativeParse(
                        input,
                        lengthText.toIntOrNull() ?: 0,
                        secureMode,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Parse record") }
            ChallengeConsole(output)
        }
    }
}
