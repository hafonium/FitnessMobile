package com.example.homeworkout.ui.components.buttons

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.homeworkout.ui.theme.AppGradients
import com.example.homeworkout.ui.theme.HairlineGray
import com.example.homeworkout.ui.theme.InkBlack
import com.example.homeworkout.ui.theme.PillShape

/** Visual style for [AppButton] — the three pill treatments used across the storyboard. */
enum class AppButtonVariant { Primary, Dark, Outlined, Tonal, OnAccent }

/**
 * Shared pill CTA. Lives in ui/components so every feature package can reuse it instead of
 * styling its own Material3 Button. Defaults to the blue-gradient "Primary" pill used for almost
 * every call to action in the storyboard (Start, Continue, Save, Done, Close...).
 */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: AppButtonVariant = AppButtonVariant.Primary,
    subtitle: String? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
) {
    val shape = PillShape
    val contentColor = when (variant) {
        AppButtonVariant.Primary, AppButtonVariant.Dark -> Color.White
        AppButtonVariant.Outlined -> InkBlack
        AppButtonVariant.Tonal -> MaterialTheme.colorScheme.primary
        AppButtonVariant.OnAccent -> MaterialTheme.colorScheme.primary
    }
    val background = when (variant) {
        AppButtonVariant.Primary -> Modifier.background(AppGradients.PrimaryButton)
        AppButtonVariant.Dark -> Modifier.background(AppGradients.DarkButton)
        AppButtonVariant.Outlined -> Modifier
            .background(Color.White)
            .border(1.5.dp, HairlineGray, shape)
        AppButtonVariant.Tonal -> Modifier.background(MaterialTheme.colorScheme.primaryContainer)
        AppButtonVariant.OnAccent -> Modifier.background(Color.White)
    }

    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.5f)
            .clip(shape)
            .then(background)
            .defaultMinSize(minHeight = 52.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        if (subtitle != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = text,
                    color = contentColor,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = subtitle,
                    color = contentColor.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Text(
                text = text,
                color = contentColor,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}
