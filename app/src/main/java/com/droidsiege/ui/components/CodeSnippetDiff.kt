package com.droidsiege.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AssistChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidsiege.R

private const val MASTG_SEARCH_URL = "https://mas.owasp.org/?s="

@Composable
fun CodeSnippetDiff(
    vulnerableSnippet: String,
    fixSnippet: String,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        SnippetBlock(
            title = stringResource(R.string.learn_vulnerable),
            text = vulnerableSnippet,
            container = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
            labelColor = MaterialTheme.colorScheme.onErrorContainer,
        )
        SnippetBlock(
            title = stringResource(R.string.learn_hardened),
            text = fixSnippet,
            container = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun SnippetBlock(
    title: String,
    text: String,
    container: Color,
    labelColor: Color,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = container,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = labelColor,
            )
            HorizontalDivider(color = labelColor.copy(alpha = 0.3f))
            SelectionContainer {
                Text(
                    text = text,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = labelColor,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MastgRefRow(
    refs: List<String>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        refs.forEach { ref ->
            AssistChip(
                onClick = { openMastgSearch(context, ref) },
                label = { Text(ref) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OwaspRefChips(
    refs: List<String>,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        refs.forEach { ref ->
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                Text(
                    text = ref,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}

private fun openMastgSearch(
    context: Context,
    ref: String,
) {
    val intent =
        Intent(Intent.ACTION_VIEW, Uri.parse(MASTG_SEARCH_URL + ref))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // no browser installed — nothing sensible to do on a challenge device
    }
}
