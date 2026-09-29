package com.droidsiege.challenges.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.droidsiege.R
import com.droidsiege.core.Challenge
import com.droidsiege.core.ChallengeId
import com.droidsiege.core.Difficulty
import com.droidsiege.core.LearnContent

class HelloFlagChallenge : Challenge {
    override val id = ChallengeId(category = "demo", slug = "helloflag", level = Difficulty.EASY)

    override val title: String = "Hello, Flag!"

    override val brief: String =
        "This onboarding challenge proves the whole pipeline works. The flag is rendered as " +
            "plain text inside the challenge screen — read it and submit it below."

    override val owaspRefs: List<String> = listOf("M9", "MASVS-STORAGE-1")

    override val hints: List<String> =
        listOf(
            "The flag is printed as plain text inside the challenge screen below.",
            "Flags always look like DS{category_slug_L<level>_<token>}.",
            "Copy it exactly — validation is a case-sensitive exact match.",
        )

    override val flag: String = "DS{demo_helloflag_L1_f17a2b}"

    override val learn: LearnContent =
        LearnContent(
            theory =
                "Anything an app renders to the screen is readable without real exploitation: " +
                    "a screenshot, a uiautomator dump, or a view-hierarchy capture is enough. The same " +
                    "holds for secrets written to logcat, the clipboard, or unprotected preferences.\n\n" +
                    "The categories that follow hide secrets in more realistic places — preferences, " +
                    "files, logs, backups — and grade how hard they are to reach. This demo keeps the " +
                    "secret on-screen so you can exercise the full loop: find, submit, score, and flip " +
                    "the secure toggle to see the hardened path.",
            mastgRefs = listOf("MASVS-STORAGE-1", "MASTG-TEST-0x52"),
            vulnerableSnippet =
                "@Composable\nfun FlagCard() {\n" +
                    "    Text(text = \"Your flag: DS{demo_helloflag_L1_f17a2b}\")\n" +
                    "}",
            fixSnippet =
                "@Composable\nfun FlagCard(secureMode: Boolean) {\n" +
                    "    if (secureMode) {\n" +
                    "        MaskedFlag()\n" +
                    "    } else {\n" +
                    "        Text(text = \"Your flag: \$flag\")\n" +
                    "    }\n" +
                    "}",
            takeaway =
                "A secret shown by the UI is not a secret — gate sensitive rendering behind " +
                    "explicit guards instead of shipping it in plaintext.",
        )

    @Composable
    override fun Screen(secureMode: Boolean) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.demo_screen_caption),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = if (secureMode) stringResource(R.string.demo_masked) else flag,
                    modifier = Modifier.padding(16.dp),
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
