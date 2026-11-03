package com.project.speakmate.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (isMascot) {
            Image(painterResource(iconRes), null, Modifier.size(116.dp))
        } else {
            Box(Modifier.size(64.dp).background(DesignColor.SecondaryButton, CircleShape), contentAlignment = Alignment.Center) {
                Icon(painterResource(iconRes), null, Modifier.size(32.dp), tint = DesignColor.Primary)
            }
        }
        Text(title, style = DesignTypography.Heading, color = DesignColor.Text, textAlign = TextAlign.Center)
        Text(description, style = DesignTypography.Body, color = DesignColor.Muted, textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp))
        PrimaryButton(primaryText, onPrimaryAction, Modifier.padding(horizontal = 8.dp))
        if (secondaryText.isNotEmpty()) {
            SecondaryButton(secondaryText, onSecondaryAction, Modifier.padding(horizontal = 8.dp))
        }
    }
}
