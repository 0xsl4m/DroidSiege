package com.droidsiege.challenges.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.runtime.Composable
import androidx.core.content.FileProvider
import com.droidsiege.challenges.common.ActionChallengeScreen
import com.droidsiege.challenges.common.KitAction
import com.droidsiege.challenges.common.TieredChallenge
import com.droidsiege.core.Difficulty
import com.droidsiege.core.LearnContent
import java.io.File
import java.io.FileOutputStream

private const val FLAG_L1 = "DS{storage_extstorage_L1_6c2e85}"
private const val FLAG_L2 = "DS{storage_extstorage_L2_f43a09}"
private const val FLAG_L3 = "DS{storage_extstorage_L3_91d6b3}"
private const val FLAG_L4 = "DS{storage_extstorage_L4_57ae20}"

object ExtStorageVault {
    fun exportReceipt(
        context: Context,
        secureMode: Boolean,
    ): String {
        val dir = context.getExternalFilesDir(null) ?: return "External storage unavailable."
        val receipt = File(dir, "receipt_export.txt")
        if (!secureMode) {
            receipt.writeText(
                """
                SiegeApp receipt export
                -----------------------
                order: SR-20451
                total: ${'$'}42.10
                recovery: $FLAG_L1
                """.trimIndent(),
            )
        } else {
            receipt.writeText(
                """
                SiegeApp receipt export
                -----------------------
                order: SR-20451
                total: ${'$'}42.10
                """.trimIndent(),
            )
        }
        return if (!secureMode) {
            "Receipt exported to ${receipt.absolutePath}"
        } else {
            "Receipt exported without account secrets to ${receipt.absolutePath}"
        }
    }

    fun writeReportCache(
        context: Context,
        secureMode: Boolean,
    ): String {
        val report = File(context.cacheDir, "tmp_report_1337.txt")
        if (!secureMode) {
            report.writeText("diagnostic session: $FLAG_L2")
        } else {
            report.writeText("diagnostic session: [redacted]")
        }
        return "Diagnostic written to cache (${report.name})."
    }

    fun stageShareFile(
        context: Context,
        secureMode: Boolean,
    ): String {
        val dir = File(context.filesDir, "shared")
            .apply { mkdirs() }
        val staged = File(dir, "transfer_summary.txt")
        staged.writeText(
            if (!secureMode) {
                "transfer summary\nreference: $FLAG_L3\n"
            } else {
                "transfer summary\nreference: [redacted]\n"
            },
        )
        return if (!secureMode) {
            "Staged at ${staged.absolutePath} — shared through the app's FileProvider."
        } else {
            "Staged redacted summary; the sensitive file is no longer provider-shared."
        }
    }

    fun shareStagedFile(context: Context): String {
        val staged = File(File(context.filesDir, "shared"), "transfer_summary.txt")
        if (!staged.exists()) {
            stageShareFile(context, secureMode = false)
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.transfer",
            staged,
        )
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            android.content.Intent.createChooser(intent, "Send transfer summary")
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        return "Share sheet opened; receivers get a read grant to $uri"
    }

    fun savePhotoWithMetadata(
        context: Context,
        secureMode: Boolean,
    ): String {
        val bitmap = Bitmap.createBitmap(480, 320, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawColor(Color.rgb(15, 23, 42))
        Paint().apply {
            color = Color.WHITE
            textSize = 28f
            isAntiAlias = true
        }.let { paint ->
            Canvas(bitmap).drawText("SiegeApp photowalk", 24f, 160f, paint)
        }
        val photos = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), "photowalk")
            .apply { mkdirs() }
        val photo = File(photos, "session_photo.jpg")
        FileOutputStream(photo).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        androidx.exifinterface.media.ExifInterface(photo).apply {
            if (!secureMode) {
                setAttribute(
                    androidx.exifinterface.media.ExifInterface.TAG_USER_COMMENT,
                    FLAG_L4,
                )
            } else {
                setAttribute(
                    androidx.exifinterface.media.ExifInterface.TAG_USER_COMMENT,
                    "siege photowalk 2026",
                )
            }
            saveAttributes()
        }
        return if (!secureMode) {
            "Photo saved with session metadata: ${photo.absolutePath}"
        } else {
            "Photo saved with clean metadata: ${photo.absolutePath}"
        }
    }
}

class ExtStorageL1Challenge : TieredChallenge(
    category = "storage",
    slug = "extstorage",
    level = Difficulty.EASY,
    title = "Receipt Export",
    brief = "Users asked for their receipts as files, so the wallet exports them to " +
        "device storage. Recover the account recovery code from an export.",
    owaspRefs = listOf("M9", "MASVS-STORAGE-3", "MASTG-TEST-0x55"),
    hints = listOf(
        "The app-specific external dir lands under /sdcard/Android/data/com.droidsiege/files/.",
        "adb shell ls /sdcard/Android/data/com.droidsiege/files works from any shell.",
        "cat receipt_export.txt.",
    ),
    flag = FLAG_L1,
    learn = LearnContent(
        theory = "External storage trades privacy for convenience. App-scoped external " +
            "directories are still readable by any shell/adb session on the device, and " +
            "on legacy apps the world-readable variant is worse. Files written there " +
            "must be treated as published.\n\n" +
            "Secrets belong in internal storage — or better, in memory.",
        mastgRefs = listOf("MASVS-STORAGE-3", "MASTG-TEST-0x55"),
        vulnerableSnippet = "val dir = context.getExternalFilesDir(null)\n" +
            "File(dir, \"receipt_export.txt\").writeText(\"recovery: ${'$'}recoveryCode\")",
        fixSnippet = "// Internal, app-private storage only:\n" +
            "File(context.filesDir, \"receipts/receipt_export.txt\")",
        takeaway = "Anything under /sdcard is effectively public on a compromised device.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        ActionChallengeScreen(
            secureMode = secureMode,
            actions = listOf(
                KitAction("Export receipt") { ctx, secure -> ExtStorageVault.exportReceipt(ctx, secure) },
            ),
        )
    }
}

class ExtStorageL2Challenge : TieredChallenge(
    category = "storage",
    slug = "extstorage",
    level = Difficulty.MEDIUM,
    title = "Diagnostic Cache",
    brief = "Diagnostics write a session report into the app cache for support tooling. " +
        "Support never reads it twice — maybe someone else will.",
    owaspRefs = listOf("M9", "MASVS-STORAGE-3", "MASTG-TEST-0x55"),
    hints = listOf(
        "Cache lives in /data/data/com.droidsiege/cache/.",
        "The file name is deterministic — support tooling needs it to be.",
        "run-as com.droidsiege cat cache/tmp_report_1337.txt",
    ),
    flag = FLAG_L2,
    learn = LearnContent(
        theory = "The cache directory promises ephemerality, not secrecy. Files there " +
            "survive as long as the system allows and are fully readable inside the " +
            "sandbox's trust boundary — which run-as and root cross trivially.\n\n" +
            "Predictable names make it worse: tooling and attackers both know exactly " +
            "where to look.",
        mastgRefs = listOf("MASVS-STORAGE-3", "MASTG-TEST-0x55"),
        vulnerableSnippet = "File(cacheDir, \"tmp_report_1337.txt\")\n" +
            "    .writeText(\"diagnostic session: ${'$'}sessionId\")",
        fixSnippet = "// Diagnostics carry no identifiers; if they must, encrypt or drop them:\n" +
            "File(cacheDir, \"session.txt\").writeText(\"diagnostic session: [redacted]\")",
        takeaway = "Cache is disposable storage — anything sensitive there is already leaked.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        ActionChallengeScreen(
            secureMode = secureMode,
            actions = listOf(
                KitAction("Write diagnostic report") { ctx, secure -> ExtStorageVault.writeReportCache(ctx, secure) },
            ),
        )
    }
}

class ExtStorageL3Challenge : TieredChallenge(
    category = "storage",
    slug = "extstorage",
    level = Difficulty.HARD,
    title = "Transfer Share",
    brief = "Transfer summaries are shared between the wallet's components through the " +
        "app's FileProvider. Get the sensitive reference out of the shared file.",
    owaspRefs = listOf("M9", "MASVS-STORAGE-3", "MASTG-TEST-0x55"),
    hints = listOf(
        "Check the manifest: the transfer FileProvider lists a very broad path.",
        "res/xml/file_paths.xml exposes <files-path name=\"shared\" path=\".\"/> — the whole files dir.",
        "The staged file is files/shared/transfer_summary.txt — served as content://com.droidsiege.transfer/…",
        "…shared/transfer_summary.txt, once a grant exists.",
    ),
    flag = FLAG_L3,
    learn = LearnContent(
        theory = "FileProvider turns private files into shareable content:// URIs. The " +
            "configuration decides the blast radius: a broad <files-path path=\".\"/> " +
            "plus permissive grant flags means any receiver of an intent can read far " +
            "more than one file.\n\n" +
            "Providers must share narrow, dedicated directories, and grants must be " +
            "explicit and short-lived.",
        mastgRefs = listOf("MASVS-STORAGE-3", "MASVS-PLATFORM-1", "MASTG-TEST-0x55"),
        vulnerableSnippet = "<provider android:name=\"androidx.core.content.FileProvider\"\n" +
            "    android:grantUriPermissions=\"true\">\n" +
            "  <meta-data android:resource=\"@xml/file_paths\"/> <!-- files-path path=\".\" -->",
        fixSnippet = "<files-path name=\"transfer\" path=\"shared/outgoing/\" />\n" +
            "// + per-URI grants, no wildcards",
        takeaway = "A provider is a door in the sandbox wall — the path config is the door's width.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        ActionChallengeScreen(
            secureMode = secureMode,
            actions = listOf(
                KitAction("Stage transfer summary") { ctx, secure -> ExtStorageVault.stageShareFile(ctx, secure) },
                KitAction("Open share sheet (grants read)") { ctx, _ -> ExtStorageVault.shareStagedFile(ctx) },
            ),
        )
    }
}

class ExtStorageL4Challenge : TieredChallenge(
    category = "storage",
    slug = "extstorage",
    level = Difficulty.INSANE,
    title = "Photowalk Metadata",
    brief = "The community photowalk saves session photos to the gallery. Pictures are " +
        "just pictures — unless the app writes more into them than pixels.",
    owaspRefs = listOf("M9", "MASVS-STORAGE-3", "M6", "MASTG-TEST-0x55"),
    hints = listOf(
        "EXIF metadata travels with JPEG files and survives viewing apps.",
        "The session photo sits under Android/data/com.droidsiege/files/Pictures/photowalk/.",
        "Read TAG_USER_COMMENT with any EXIF tool (exiftool session_photo.jpg).",
    ),
    flag = FLAG_L4,
    learn = LearnContent(
        theory = "Images carry metadata planes — EXIF comments, GPS tags, thumbnails — " +
            "that outlive the pixels and travel wherever the file goes. Writing " +
            "identifiers or secrets into metadata publishes them to every reader, most " +
            "of which render none of it.\n\n" +
            "Photo pipelines must strip or never-write sensitive metadata; scoped " +
            "storage then limits who can reach the file at all.",
        mastgRefs = listOf("MASVS-STORAGE-3", "MASVS-PRIVACY-1", "MASTG-TEST-0x55"),
        vulnerableSnippet = "ExifInterface(photo).apply {\n" +
            "    setAttribute(TAG_USER_COMMENT, sessionSecret)\n" +
            "    saveAttributes()\n}",
        fixSnippet = "exif.setAttribute(TAG_USER_COMMENT, \"siege photowalk 2026\")\n" +
            "// or strip metadata entirely before saving/sharing",
        takeaway = "Metadata is content — audit every field a media pipeline writes.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        ActionChallengeScreen(
            secureMode = secureMode,
            actions = listOf(
                KitAction("Save session photo") { ctx, secure -> ExtStorageVault.savePhotoWithMetadata(ctx, secure) },
            ),
        )
    }
}
