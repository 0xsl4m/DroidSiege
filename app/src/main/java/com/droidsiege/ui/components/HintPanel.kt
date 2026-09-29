package com.droidsiege.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.droidsiege.R
import com.droidsiege.core.Challenge
import com.droidsiege.engine.HintPenalty

@Composable
fun HintPanel(
    challenge: Challenge,
    hintsUsed: Int,
    awardNow: Int,
    onReveal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.hint_penalty_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        challenge.hints.forEachIndexed { index, hint ->
            when {
                index < hintsUsed -> RevealedHintCard(index = index, hint = hint)
                index == hintsUsed ->
                    LockedNextHintCard(
                        index = index,
                        cost = awardNow - penaltyAwardAfterNext(challenge, hintsUsed),
                        onReveal = onReveal,
                    )
                else -> LockedFarHintRow(index = index)
            }
        }
        Text(
            text = stringResource(R.string.hint_award_preview, awardNow),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun RevealedHintCard(
    index: Int,
    hint: String,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(12.dp),
        ) {
            Text(
                text = stringResource(R.string.hint_number, index + 1),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(text = hint, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun LockedNextHintCard(
    index: Int,
    cost: Int,
    onReveal: () -> Unit,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(12.dp),
        ) {
            Text(
                text = stringResource(R.string.hint_locked, index + 1),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.hint_cost_warning, cost),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onReveal) {
                Text(stringResource(R.string.hint_reveal, index + 1, cost))
            }
        }
    }
}

@Composable
private fun LockedFarHintRow(index: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = stringResource(R.string.hint_locked, index + 1),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

private fun penaltyAwardAfterNext(
    challenge: Challenge,
    hintsUsed: Int,
): Int =
    HintPenalty.pointsAwarded(
        levelPoints = challenge.id.level.points,
        hintsUsed = (hintsUsed + 1).coerceAtMost(challenge.hints.size),
    )
