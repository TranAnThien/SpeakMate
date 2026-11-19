package com.project.speakmate.ui.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.project.speakmate.R
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun HomeProfileCard(displayName: String, email: String, avatarUrl: String?, onViewProfile: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(DesignColor.Surface, RoundedCornerShape(16.dp))
        .clickable(onClick = onViewProfile).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top) {

        if (!avatarUrl.isNullOrEmpty()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(48.dp).clip(CircleShape).background(DesignColor.Surface)
            )
        } else {
            Box(Modifier.size(48.dp).background(DesignColor.IconBlue, CircleShape), contentAlignment = Alignment.Center) {
                Image(painterResource(R.drawable.ic_navigation_profile), null, Modifier.size(24.dp), colorFilter = ColorFilter.tint(DesignColor.Primary))
            }
        }

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(displayName, style = DesignTypography.Title, color = DesignColor.Text)
            Text(email, style = DesignTypography.Detail, color = DesignColor.Muted)
        }
    }
}
