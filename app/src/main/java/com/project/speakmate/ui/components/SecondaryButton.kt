package com.project.speakmate.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    background: Color = Color.Transparent,
    enabled: Boolean = true
) {
    val colors = MaterialTheme.colorScheme
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp),
        enabled = enabled,
        shape = CircleShape,
        border = BorderStroke(1.dp, if (enabled) colors.outline else colors.onSurface.copy(alpha = 0.12f)),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = background)
    ) {
        Text(text, style = DesignTypography.Button, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}
