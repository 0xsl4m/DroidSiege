package com.droidsiege.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.droidsiege.core.Difficulty

private val EasyContainer = Color(0xFF2E7D32)
private val MediumContainer = Color(0xFFF9A825)
private val HardContainer = Color(0xFFEF6C00)
private val InsaneContainer = Color(0xFFC62828)

private fun Difficulty.containerColor(): Color =
    when (this) {
        Difficulty.EASY -> EasyContainer
        Difficulty.MEDIUM -> MediumContainer
        Difficulty.HARD -> HardContainer
        Difficulty.INSANE -> InsaneContainer
    }

@Composable
fun LevelChip(
    difficulty: Difficulty,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = difficulty.containerColor(),
        contentColor = Color.White,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier,
    ) {
        Text(
            text = difficulty.label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}
