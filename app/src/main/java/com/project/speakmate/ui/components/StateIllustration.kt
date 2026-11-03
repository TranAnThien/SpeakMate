package com.project.speakmate.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.ui.theme.DesignColor

@Composable
fun StateIllustration(@DrawableRes iconRes: Int, modifier: Modifier = Modifier) {
    Box(modifier.size(116.dp).background(DesignColor.IllustrationSurface, CircleShape), contentAlignment = Alignment.Center) {
        Image(painterResource(iconRes), null, Modifier.size(48.dp), colorFilter = ColorFilter.tint(DesignColor.Placeholder))
        Box(Modifier.align(Alignment.TopStart).offset(6.dp, 92.dp).size(10.dp).background(DesignColor.IconBlue, CircleShape))
        Box(Modifier.align(Alignment.TopStart).offset(99.dp, 14.dp).size(8.dp).background(DesignColor.Border, CircleShape))
    }
}
