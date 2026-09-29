package com.droidsiege.challenges.components

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.droidsiege.engine.SecureModeStore
import kotlinx.coroutines.flow.first
import java.io.File

private class VaultDb(context: Context) :
    SQLiteOpenHelper(context, "components_vault.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        // L1 table: only what /secrets may ever return
        db.execSQL("CREATE TABLE secrets(tag TEXT PRIMARY KEY, value TEXT)")
        db.execSQL("INSERT INTO secrets VALUES('hint', 'the lookup filter is injectable — see L3')")
        db.execSQL("INSERT INTO secrets VALUES('recovery', 'DS{components_provider_L1_3f86d1}')")
        // L3 target table: reachable only through the injectable /lookup path
        db.execSQL("CREATE TABLE hidden_secrets(tag TEXT PRIMARY KEY, value TEXT)")
        db.execSQL("INSERT INTO hidden_secrets VALUES('admin_recovery', 'DS{components_provider_L3_e07b52}')")
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int,
    ) = Unit
}

/**
 * M8 provider family. Exported (the seed); the hardened paths are enforced inside the
 * provider, gated on the global secure mode.
 */
class SiegeVaultProvider : ContentProvider() {
    private lateinit var db: VaultDb

    override fun onCreate(): Boolean {
        db = VaultDb(context!!)
        return true
    }

    private fun secureMode(): Boolean {
        val store = SecureModeStore(context!!)
        return kotlinx.coroutines.runBlocking { store.secureMode.first() }
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        val cursor = MatrixCursor(arrayOf("tag", "value"))
        when (uri.pathSegments.firstOrNull()) {
            "secrets" -> querySecrets(cursor)
            "realm" -> queryRealm(uri, cursor)
            "lookup" -> queryLookup(uri, cursor)
        }
        return cursor
    }

    /** L1 — the whole table for anyone. */
    private fun querySecrets(cursor: MatrixCursor) {
        db.readableDatabase.rawQuery("SELECT tag, value FROM secrets", null).use { src ->
            while (src.moveToNext()) cursor.addRow(arrayOf(src.getString(0), src.getString(1)))
        }
    }

    /** L2 — only the realm/core path segment carries the recovery row. */
    private fun queryRealm(
        uri: Uri,
        cursor: MatrixCursor,
    ) {
        val realm = uri.pathSegments.getOrNull(1)
        val record = uri.pathSegments.getOrNull(2)
        if (realm == "core") {
            val revealed = !secureMode() && record == "secret"
            cursor.addRow(arrayOf("core_recovery", if (revealed) "DS{components_provider_L2_82c4a9}" else "[redacted]"))
        }
    }

    /** L3 — injectable filter in the insecure mode, parameterized in the hardened one. */
    private fun queryLookup(
        uri: Uri,
        cursor: MatrixCursor,
    ) {
        val filter = uri.getQueryParameter("filter") ?: ""
        val query =
            if (secureMode()) {
                db.readableDatabase.rawQuery(
                    "SELECT tag, value FROM hidden_secrets WHERE tag = ?",
                    arrayOf(filter),
                )
            } else {
                db.readableDatabase.rawQuery(
                    "SELECT tag, value FROM hidden_secrets WHERE tag = '$filter'",
                    null,
                )
            }
        query.use { src ->
            while (src.moveToNext()) cursor.addRow(arrayOf(src.getString(0), src.getString(1)))
        }
    }

    /** L4 — file access with a traversable path (insecure) vs canonicalized (hardened). */
    override fun openFile(
        uri: Uri,
        mode: String,
    ): ParcelFileDescriptor? {
        val context = context!!
        val relative = uri.pathSegments.joinToString("/")
        val base = File(context.filesDir, "provider_files")
        base.mkdirs()
        val target =
            if (secureMode()) {
                val requested = File(base, relative).canonicalFile
                if (!requested.path.startsWith(base.canonicalFile.path)) {
                    throw SecurityException("path escapes the provider root")
                }
                requested
            } else {
                // insecure: the raw (traversable) path is used as-is
                File(base, relative)
            }
        if (!target.exists()) target.writeText("(empty provider file)")
        return ParcelFileDescriptor.open(target, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun getType(uri: Uri): String = "text/plain"

    override fun insert(
        uri: Uri,
        values: ContentValues?,
    ): Uri? = null

    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0
}

/** L4 helper: plants the app-private file the traversal is meant to reach. */
object ProviderFiles {
    fun plantSecret(context: Context): File {
        context.filesDir.mkdirs()
        return File(context.filesDir, "secret_flag.txt").apply {
            writeText("DS{components_provider_L4_46d9f8}")
        }
    }
}
