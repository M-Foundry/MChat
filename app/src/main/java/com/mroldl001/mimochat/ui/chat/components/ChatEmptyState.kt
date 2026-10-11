package com.mroldl001.mimochat.ui.chat.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mroldl001.mimochat.R

private const val EASTER_EGG = "MChat在这里，シタイだけ探した冒険TONGUE"
private const val EASTER_EGG_SECOND_LINE = "シタイだけ探した冒険TONGUE"

@Composable
internal fun ChatEmptyState(modifier: Modifier = Modifier) {
    val tips = listOf(
        stringResource(R.string.welcome_tip_1),
        stringResource(R.string.welcome_tip_2),
        stringResource(R.string.welcome_tip_3),
        stringResource(R.string.welcome_tip_4),
        EASTER_EGG
    )

    val randomText = remember(tips) { tips.random() }
    val prefix = stringResource(R.string.welcome_prefix)
    val secondLine = if (randomText == EASTER_EGG) EASTER_EGG_SECOND_LINE else randomText

    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = prefix,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Text(
            text = secondLine,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}