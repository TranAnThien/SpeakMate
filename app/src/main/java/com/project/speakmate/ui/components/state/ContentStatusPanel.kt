package com.project.speakmate.ui.components.state

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.project.speakmate.ui.components.button.PrimaryButton
import com.project.speakmate.ui.components.button.SecondaryButton
import com.project.speakmate.ui.components.image.CircleIcon
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun ContentStatusPanel(
    @DrawableRes iconRes: Int,
    title: String,
    description: String,
    primaryText: String,
    secondaryText: String,
    onPrimaryAction: () -> Unit,
    onSecondaryAction: () -> Unit,
    modifier: Modifier = Modifier,
    isMascot: Boolean = false
) {
    StatusContent(
        title = title,
        titleStyle = DesignTypography.Heading,
        titleColor = DesignColor.Text,
        titleTextAlign = TextAlign.Center,
        description = description,
        descriptionStyle = DesignTypography.Body,
        descriptionColor = DesignColor.Muted,
        descriptionTextAlign = TextAlign.Center,
        descriptionModifier = Modifier.padding(horizontal = 8.dp),
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        illustration = {
            if (isMascot) {
                Image(painter = painterResource(iconRes), contentDescription = null, modifier = Modifier.size(116.dp))
            } else {
                CircleIcon(iconRes = iconRes)
            }
        },
        actions = {
            PrimaryButton(text = primaryText, onClick = onPrimaryAction, modifier = Modifier.padding(horizontal = 8.dp))
            if (secondaryText.isNotEmpty()) {
                SecondaryButton(
                    text = secondaryText,
                    onClick = onSecondaryAction,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    )
}
