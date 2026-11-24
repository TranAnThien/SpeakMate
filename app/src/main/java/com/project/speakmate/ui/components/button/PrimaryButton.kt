package com.project.speakmate.ui.components.button

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.components.text.DetailText
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    minHeight: Dp = 48.dp,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    val loadingDescription = stringResource(R.string.accessibility_loading)
    val colors = MaterialTheme.colorScheme
    Button(
        onClick = onClick,
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = minHeight.coerceAtLeast(48.dp))
                .semantics {
                if (isLoading) {
                    contentDescription = text
                    stateDescription = loadingDescription
                    progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
                }
            },
        enabled = enabled && !isLoading,
        shape = CircleShape,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
        colors =
            ButtonDefaults.buttonColors(
                disabledContainerColor = if (isLoading) colors.primary else colors.onSurface.copy(alpha = 0.12f),
                disabledContentColor = if (isLoading) colors.onPrimary else colors.onSurface.copy(alpha = 0.38f)
            )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp).clearAndSetSemantics {},
                color = colors.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            DetailText(
                text = text,
                style = DesignTypography.Button,
                color = LocalContentColor.current,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
