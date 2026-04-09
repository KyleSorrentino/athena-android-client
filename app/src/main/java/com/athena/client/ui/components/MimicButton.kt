package com.athena.client.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp

@Composable
fun MimicButton(
    isListening: Boolean,
    isProcessing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isListening) 1.1f else 1f,
        animationSpec = tween(150),
        label = "mimicScale"
    )

    val containerColor by animateColorAsState(
        targetValue = when {
            isListening -> MaterialTheme.colorScheme.tertiary
            isProcessing -> MaterialTheme.colorScheme.surfaceVariant
            else -> MaterialTheme.colorScheme.tertiaryContainer
        },
        animationSpec = tween(150),
        label = "mimicContainerColor"
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            isListening -> MaterialTheme.colorScheme.onTertiary
            isProcessing -> MaterialTheme.colorScheme.onSurfaceVariant
            else -> MaterialTheme.colorScheme.onTertiaryContainer
        },
        animationSpec = tween(150),
        label = "mimicContentColor"
    )

    SmallFloatingActionButton(
        onClick = onClick,
        modifier = modifier
            .size(56.dp)
            .scale(scale)
            .alpha(if (isProcessing && !isListening) 0.5f else 1f),
        containerColor = containerColor,
        contentColor = contentColor,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 4.dp
        )
    ) {
        Icon(
            imageVector = Icons.Filled.ContentCopy,
            contentDescription = if (isListening) "Stop listening" else "Mimic voice",
            modifier = Modifier.size(24.dp)
        )
    }
}
