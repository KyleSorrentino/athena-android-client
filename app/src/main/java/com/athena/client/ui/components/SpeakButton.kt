package com.athena.client.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp

@Composable
fun SpeakButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    SmallFloatingActionButton(
        onClick = { if (enabled) onClick() },
        modifier = modifier
            .size(56.dp)
            .alpha(if (enabled) 1f else 0.5f),
        containerColor = if (enabled) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        contentColor = if (enabled) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 4.dp
        )
    ) {
        Icon(
            imageVector = Icons.Filled.RecordVoiceOver,
            contentDescription = "Speak text",
            modifier = Modifier.size(24.dp)
        )
    }
}
