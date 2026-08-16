package my.id.rmalan.cache.sweep.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import my.id.rmalan.cache.sweep.ui.theme.NeoBlack

/**
 * Reusable Neobrutalist Card container with a 2dp solid outline and hard unblurred drop-shadow.
 * In dark mode, shadows are eliminated for clean, flat, high-contrast Cyber-Brutalist styling.
 */
@Composable
fun NeoCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    shadowColor: Color = NeoBlack,
    borderWidth: Dp = 2.dp,
    shadowOffset: Dp = 4.dp,
    cornerRadius: Dp = 12.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val effectiveShadowOffset = if (isDark) 0.dp else shadowOffset

    Box(
        modifier = if (effectiveShadowOffset > 0.dp) {
            modifier.padding(end = effectiveShadowOffset, bottom = effectiveShadowOffset)
        } else {
            modifier
        }
    ) {
        // Hard unblurred shadow layer (light mode only)
        if (effectiveShadowOffset > 0.dp) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = effectiveShadowOffset, y = effectiveShadowOffset)
                    .background(
                        color = shadowColor,
                        shape = RoundedCornerShape(cornerRadius)
                    )
            )
        }
        // Foreground surface layer
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = borderWidth,
                    color = borderColor,
                    shape = RoundedCornerShape(cornerRadius)
                ),
            shape = RoundedCornerShape(cornerRadius),
            color = containerColor
        ) {
            Column(
                modifier = Modifier.padding(contentPadding)
            ) {
                content()
            }
        }
    }
}

/**
 * Tactile Neobrutalist Button with solid outline and hard drop-shadow.
 * In dark mode, shadows are eliminated for clean, flat, high-contrast Cyber-Brutalist styling.
 */
@Composable
fun NeoButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    shadowColor: Color = NeoBlack,
    borderWidth: Dp = 2.dp,
    shadowOffset: Dp = 4.dp,
    cornerRadius: Dp = 12.dp,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val actualContainerColor = if (enabled) containerColor else containerColor.copy(alpha = 0.5f)
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val effectiveShadowOffset = if (isDark || !enabled) 0.dp else shadowOffset

    Box(
        modifier = if (effectiveShadowOffset > 0.dp) {
            modifier.padding(end = effectiveShadowOffset, bottom = effectiveShadowOffset)
        } else {
            modifier
        }
    ) {
        if (effectiveShadowOffset > 0.dp) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = effectiveShadowOffset, y = effectiveShadowOffset)
                    .background(
                        color = shadowColor,
                        shape = RoundedCornerShape(cornerRadius)
                    )
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .border(
                    width = borderWidth,
                    color = if (enabled) borderColor else borderColor.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(cornerRadius)
                )
                .clickable(
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick
                ),
            shape = RoundedCornerShape(cornerRadius),
            color = actualContainerColor
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProvideTextStyle(
                    value = MaterialTheme.typography.labelLarge.copy(
                        color = contentColor,
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    content()
                }
            }
        }
    }
}

/**
 * Neobrutalist status / metric badge pill with crisp 1.5dp border.
 */
@Composable
fun NeoBadge(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    borderWidth: Dp = 1.5.dp,
    cornerRadius: Dp = 6.dp,
    textStyle: TextStyle = MaterialTheme.typography.labelSmall
) {
    Surface(
        modifier = modifier.border(
            width = borderWidth,
            color = borderColor,
            shape = RoundedCornerShape(cornerRadius)
        ),
        shape = RoundedCornerShape(cornerRadius),
        color = containerColor
    ) {
        Text(
            text = text,
            style = textStyle,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

/**
 * Chunky Neobrutalist Progress Bar with solid border frame and flat block fill.
 */
@Composable
fun NeoProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    borderWidth: Dp = 2.dp,
    height: Dp = 14.dp,
    cornerRadius: Dp = 6.dp
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .border(
                width = borderWidth,
                color = borderColor,
                shape = shape
            )
            .clip(shape)
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(clampedProgress)
                .fillMaxHeight()
                .background(color)
        )
    }
}
