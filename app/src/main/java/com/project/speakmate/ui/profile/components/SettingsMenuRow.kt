package com.project.speakmate.ui.profile.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.components.card.SpeakMateCard
import com.project.speakmate.ui.components.text.DetailText
import com.project.speakmate.ui.components.text.TitleText
import com.project.speakmate.ui.theme.DesignColor

@Composable
fun SettingsMenuRow(
    @DrawableRes iconRes: Int,
    title: String,
    subtitle: String,
    isDestructive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    SpeakMateCard(
        modifier = modifier.heightIn(min = 56.dp),
        onClick = onClick,
        enabled = enabled
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = if (isDestructive) DesignColor.Error else DesignColor.Muted
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TitleText(text = title, color = if (isDestructive) DesignColor.Error else DesignColor.Text)
                if (subtitle.isNotEmpty()) DetailText(text = subtitle)
            }
            Icon(
                painter = painterResource(R.drawable.ic_content_chevron),
                contentDescription = null,
                modifier = Modifier.size(20.dp, 24.dp),
                tint = DesignColor.Muted
            )
        }
    }
}
