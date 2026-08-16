package my.id.rmalan.cache.sweep.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import my.id.rmalan.cache.sweep.model.CleaningState
import my.id.rmalan.cache.sweep.ui.components.NeoBadge
import my.id.rmalan.cache.sweep.ui.components.NeoCard
import my.id.rmalan.cache.sweep.ui.components.NeoProgressBar

@Composable
fun CleaningProgressScreen(
    state: CleaningState,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Animated Icon
                val icon = when (state) {
                    is CleaningState.Validating -> Icons.Outlined.Shield
                    is CleaningState.SnapshotBefore, is CleaningState.SnapshotAfter -> Icons.Outlined.Storage
                    is CleaningState.WaitingForStats -> Icons.Outlined.HourglassEmpty
                    else -> Icons.Outlined.CleaningServices
                }

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .border(
                            width = 2.5.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Cleaning cache…",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.semantics { heading() }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Requesting Android to reclaim temporary application files.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Progress Bar and State Text
                NeoCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    when (state) {
                        is CleaningState.Clearing -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (state.total > 0) {
                                    NeoProgressBar(
                                        progress = state.progressFraction,
                                        height = 12.dp
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Cleaning ${state.current} of ${state.total}",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        NeoBadge(
                                            text = "${(state.progressFraction * 100).toInt()}%",
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                } else {
                                    NeoProgressBar(
                                        progress = 0.5f,
                                        height = 12.dp
                                    )
                                }

                                val appLabel = state.currentAppName ?: state.currentPackage
                                if (!appLabel.isNullOrBlank()) {
                                    Text(
                                        text = appLabel,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        is CleaningState.Validating -> {
                            IndeterminateProgressBlock(label = "Validating capabilities…")
                        }

                        is CleaningState.SnapshotBefore -> {
                            IndeterminateProgressBlock(label = "Capturing initial storage snapshot…")
                        }

                        is CleaningState.WaitingForStats -> {
                            IndeterminateProgressBlock(label = "Waiting for storage statistics to settle…")
                        }

                        is CleaningState.SnapshotAfter -> {
                            IndeterminateProgressBlock(label = "Calculating final reclamation metrics…")
                        }

                        else -> {
                            IndeterminateProgressBlock(label = "Preparing cleanup…")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                NeoBadge(
                    text = "DO NOT CLOSE CACHESWEEP",
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    borderWidth = 1.5.dp
                )
            }
        }
    }
}

@Composable
private fun IndeterminateProgressBlock(
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(36.dp),
            strokeWidth = 3.dp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
