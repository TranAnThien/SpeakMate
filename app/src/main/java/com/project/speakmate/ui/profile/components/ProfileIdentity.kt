package com.project.speakmate.ui.profile.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun ProfileIdentity(displayName: String, email: String, @DrawableRes avatarRes: Int?, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (avatarRes != null) {
            Image(painterResource(avatarRes), null, Modifier.size(80.dp).clip(CircleShape).border(4.dp, DesignColor.Surface, CircleShape),
                contentScale = ContentScale.Crop)
        } else {
            Box(Modifier.size(80.dp).background(DesignColor.IconBlue, CircleShape), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_navigation_profile), null, Modifier.size(32.dp), tint = DesignColor.Primary)
            }
        }
        Text(displayName, style = DesignTypography.Heading, color = DesignColor.Primary, textAlign = TextAlign.Center)
        Text(email, style = DesignTypography.Body, color = DesignColor.Muted, textAlign = TextAlign.Center)
    }
}
