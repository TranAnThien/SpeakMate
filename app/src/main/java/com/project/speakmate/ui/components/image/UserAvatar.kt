package com.project.speakmate.ui.components.image

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import com.project.speakmate.R
import com.project.speakmate.ui.theme.DesignColor

@Composable
fun UserAvatar(
    photoUrl: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    placeholderSize: Dp = size,
    placeholderTint: ColorFilter? = null,
    emptyBackground: Color = DesignColor.Surface
) {
    val placeholder = painterResource(R.drawable.ic_navigation_profile)
    Box(
        modifier =
            modifier
                .size(size)
                .clip(CircleShape)
                .background(if (photoUrl.isNullOrEmpty()) emptyBackground else DesignColor.Surface),
        contentAlignment = Alignment.Center
    ) {
        if (photoUrl.isNullOrEmpty()) {
            Image(
                painter = placeholder,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = placeholderTint,
                modifier = Modifier.size(placeholderSize)
            )
        } else {
            AsyncImage(
                model = photoUrl,
                placeholder = placeholder,
                error = placeholder,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
