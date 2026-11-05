package com.project.speakmate.ui.profile.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun SettingsMenuRow(
    @DrawableRes iconRes: Int,
    title: String,
    subtitle: String,
    isDestructive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onClick).padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).background(if (isDestructive) DesignColor.ErrorSurface else DesignColor.PaleBlue, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center) {
            Icon(painterResource(iconRes), null, Modifier.size(24.dp), tint = if (isDestructive) DesignColor.Error else DesignColor.Primary)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = DesignTypography.Title, color = if (isDestructive) DesignColor.Error else DesignColor.Text)
            if (subtitle.isNotEmpty()) Text(subtitle, style = DesignTypography.Detail, color = DesignColor.Muted)
        }
        Icon(painterResource(R.drawable.ic_content_chevron), null, Modifier.size(20.dp, 24.dp), tint = DesignColor.Muted)
    }
}
