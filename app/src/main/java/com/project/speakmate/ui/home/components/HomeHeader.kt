package com.project.speakmate.ui.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.project.speakmate.R
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun HomeHeader(isLoading: Boolean, avatarUrl: String?, onViewProfile: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(R.string.app_name), style = DesignTypography.Brand, color = DesignColor.Primary)
        Box(Modifier.size(48.dp).background(if (isLoading) DesignColor.Skeleton else if (!avatarUrl.isNullOrEmpty()) DesignColor.Surface else DesignColor.IconBlue, CircleShape)
            .clip(CircleShape)
            .clickable(enabled = !isLoading, onClick = onViewProfile), contentAlignment = Alignment.Center) {
            if (!isLoading) {
                if (!avatarUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = stringResource(R.string.navigation_profile),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(painterResource(R.drawable.ic_navigation_profile), stringResource(R.string.navigation_profile), Modifier.size(24.dp),
                        colorFilter = ColorFilter.tint(DesignColor.Primary))
                }
            }
        }
    }
}
