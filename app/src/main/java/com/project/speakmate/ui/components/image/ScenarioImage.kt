package com.project.speakmate.ui.components.image

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.project.speakmate.R
import com.project.speakmate.ui.components.text.CaptionText
import com.project.speakmate.ui.theme.DesignColor

@Composable
fun ScenarioImage(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    @DrawableRes imageRes: Int? = null,
    isCompact: Boolean = false
) {
    var hasFailed by remember(imageUrl) { mutableStateOf(false) }
    Box(
        modifier.clip(RoundedCornerShape(16.dp)).background(DesignColor.ExampleSurface),
        contentAlignment = Alignment.Center
    ) {
        when {
            !imageUrl.isNullOrBlank() && !hasFailed ->
                AsyncImage(
                    imageUrl,
                    null,
                    Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop,
                    onError = { hasFailed = true }
                )
            imageRes != null ->
                Image(
                    painter = painterResource(imageRes),
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop
                )
            else ->
                Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(R.drawable.ic_load_error),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = DesignColor.Muted
                    )
                    if (!isCompact) CaptionText(text = stringResource(R.string.preparation_image_unavailable))
                }
        }
    }
}
