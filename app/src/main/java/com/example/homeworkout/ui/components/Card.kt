package com.example.homeworkout.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.example.homeworkout.ui.theme.CardShape
import com.example.homeworkout.ui.theme.CardWhite

/**
 * Shared surface card. Lives in ui/components so every feature package can reuse it. White,
 * generously rounded and lightly elevated to match the storyboard's card style.
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    shape: Shape = CardShape,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(content = content)
    }
}
