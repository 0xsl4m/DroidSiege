package com.droidsiege.challenges.components

import androidx.compose.runtime.Composable
import com.droidsiege.challenges.common.ActionChallengeScreen
import com.droidsiege.challenges.common.KitAction
import com.droidsiege.challenges.common.TieredChallenge
import com.droidsiege.core.Difficulty
import com.droidsiege.core.LearnContent

private const val AUTHORITY = "com.droidsiege.vault"

class ProviderL1Challenge : TieredChallenge(
    category = "components",
    slug = "provider",
    level = Difficulty.EASY,
    title = "Open Vault",
    brief = "The wallet exposes a vault content provider. Read its recovery row.",
    owaspRefs = listOf("M8", "MASVS-PLATFORM-2", "MASTG-TEST-0x61"),
    hints = listOf(
        "Providers are queried over content:// URIs — the authority is com.droidsiege.vault.",
        "adb shell content query --uri content://com.droidsiege.vault/secrets",
        "The recovery row is tagged 'recovery'.",
    ),
    flag = "DS{components_provider_L1_3f86d1}",
    learn = LearnContent(
        theory = "An exported provider is a SQL table with a public door. The sandbox " +
            "counts for nothing once the component answers any caller: one content query " +
            "reads every row.\n\n" +
            "Providers must be non-exported, or guarded by permissions and path scoping.",
        mastgRefs = listOf("MASVS-PLATFORM-2", "MASTG-TEST-0x61"),
        vulnerableSnippet = "<provider android:exported=\"true\"\n" +
            "    android:authorities=\"com.droidsiege.vault\" />",
        fixSnippet = "android:exported=\"false\"\n// + path permissions for anything that must stay shared",
        takeaway = "Query a provider like a database — because it is one.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        ActionChallengeScreen(
            secureMode = secureMode,
            actions = listOf(
                KitAction("Show the query command") { _, _ ->
                    "adb shell content query --uri content://$AUTHORITY/secrets"
                },
            ),
        )
    }
}

class ProviderL2Challenge : TieredChallenge(
    category = "components",
    slug = "provider",
    level = Difficulty.MEDIUM,
    title = "Realm Gate",
    brief = "The provider stopped serving the secrets table directly. The recovery rows " +
        "moved behind a URI layout the docs never published.",
    owaspRefs = listOf("M8", "MASVS-PLATFORM-2", "MASTG-TEST-0x61"),
    hints = listOf(
        "Enumerate the path space: providers answer queries for any segment shape.",
        "The layout is /realm/<realm>/<record> — try core and secret as the values.",
        "content query --uri content://com.droidsiege.vault/realm/core/secret",
    ),
    flag = "DS{components_provider_L2_82c4a9}",
    learn = LearnContent(
        theory = "Obscure URI paths are the provider version of a hidden file name: " +
            "the caller controls the path, so short enumerable segments fall to a " +
            "handful of guesses. Nothing about a guessable URI restricts access.\n\n" +
            "Restrict access with permissions and explicit path grants, not spelling.",
        mastgRefs = listOf("MASVS-PLATFORM-2", "MASTG-TEST-0x61"),
        vulnerableSnippet = "when (uri.pathSegments.firstOrNull()) {\n" +
            "    \"realm\" -> if (segments[1] == \"core\" && segments[2] == \"secret\") reveal()",
        fixSnippet = "// permissions + narrow path grants in the manifest,\n" +
            "// not path spelling",
        takeaway = "If the caller picks the path, the path is not a secret.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        ActionChallengeScreen(
            secureMode = secureMode,
            actions = listOf(
                KitAction("Show the query shape") { _, _ ->
                    "adb shell content query --uri content://$AUTHORITY/realm/core/secret"
                },
            ),
        )
    }
}

class ProviderL3Challenge : TieredChallenge(
    category = "components",
    slug = "provider",
    level = Difficulty.HARD,
    title = "Injectable Lookup",
    brief = "The provider grew a lookup filter so callers can search the vault by tag. " +
        "The filter is pasted straight into the SQL.",
    owaspRefs = listOf("M8", "MASVS-PLATFORM-2", "M4", "MASTG-TEST-0x61"),
    hints = listOf(
        "Query /lookup?filter=<value> — the value lands inside single quotes in the SQL.",
        "Classic closure: x' OR '1'='1 makes the WHERE clause true for every row.",
        "The admin_recovery row holds the flag once the filter matches everything.",
    ),
    flag = "DS{components_provider_L3_e07b52}",
    learn = LearnContent(
        theory = "Concatenating caller input into SQL is injectable on any surface, " +
            "including content providers. A selection string built by string formatting " +
            "lets the caller close the quote and append their own predicate — reading " +
            "every row the query touches.\n\n" +
            "Parameterized queries (selectionArgs) are the fix, everywhere, including " +
            "provider selections.",
        mastgRefs = listOf("MASVS-PLATFORM-2", "MASVS-CODE-4", "MASTG-TEST-0x61"),
        vulnerableSnippet = "rawQuery(\"SELECT … WHERE tag = '${'$'}filter'\", null)",
        fixSnippet = "rawQuery(\"SELECT … WHERE tag = ?\", arrayOf(filter))",
        takeaway = "String-built SQL is attacker-written SQL.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        ActionChallengeScreen(
            secureMode = secureMode,
            actions = listOf(
                KitAction("Show the injection") { _, _ ->
                    "adb shell content query --uri content://$AUTHORITY/lookup " +
                        "--where \"tag='x' OR '1'='1\"  # or pass filter=x' OR '1'='1"
                },
            ),
        )
    }
}

class ProviderL4Challenge : TieredChallenge(
    category = "components",
    slug = "provider",
    level = Difficulty.INSANE,
    title = "File Traversal",
    brief = "The provider also serves note files by name. One of the app's private " +
        "files never got the memo about staying private.",
    owaspRefs = listOf("M8", "MASVS-PLATFORM-2", "M9", "MASTG-TEST-0x61"),
    hints = listOf(
        "First plant the target: press 'Plant escrow file' — it writes into the app's private files dir.",
        "openFile resolves files/<path> without canonicalizing — ../ climbs out of the provider root.",
        "content://com.droidsiege.vault/files/notes/../../secret_flag.txt — read it with content read.",
    ),
    flag = "DS{components_provider_L4_46d9f8}",
    learn = LearnContent(
        theory = "openFile hands raw file access to callers. Resolving a caller-supplied " +
            "relative path without canonicalizing it lets ../ climb out of the intended " +
            "directory into any app-private file.\n\n" +
            "Canonicalize the resolved path and verify it is still inside the allowlisted " +
            "root before opening — every time, not just for the first segment.",
        mastgRefs = listOf("MASVS-PLATFORM-2", "MASVS-STORAGE-1", "MASTG-TEST-0x61"),
        vulnerableSnippet = "val f = File(filesDir, uri.path) // ../ climbs out\n" +
            "ParcelFileDescriptor.open(f, MODE_READ_ONLY)",
        fixSnippet = "val resolved = File(base, relative).canonicalFile\n" +
            "if (!resolved.path.startsWith(base.canonicalPath)) throw SecurityException()",
        takeaway = "Canonicalize before you open, or the dots do the walking.",
    ),
) {
    @Composable
    override fun Screen(secureMode: Boolean) {
        ActionChallengeScreen(
            secureMode = secureMode,
            actions = listOf(
                KitAction("Plant escrow file") { ctx, _ ->
                    val f = ProviderFiles.plantSecret(ctx)
                    "Written (app-private): ${f.absolutePath}"
                },
                KitAction("Show the traversal") { _, _ ->
                    "adb shell content read --uri content://$AUTHORITY/files/notes/../../secret_flag.txt"
                },
            ),
        )
    }
}
