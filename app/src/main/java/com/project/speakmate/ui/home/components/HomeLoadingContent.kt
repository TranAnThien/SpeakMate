package com.project.speakmate.ui.home.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun HomeLoadingContent(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "loading_icon_rotation"
    )

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 16.dp)) {
        Row(Modifier.height(24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Image(
                painter = painterResource(R.drawable.ic_placement_progress),
                contentDescription = null,
                modifier = Modifier.size(16.dp).rotate(rotation)
            )
            Text(stringResource(R.string.home_loading), style = DesignTypography.Navigation, color = DesignColor.Muted)
        }
        Row(Modifier.fillMaxWidth().height(82.dp).background(DesignColor.Surface, RoundedCornerShape(16.dp)).padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                HomeSkeletonLine(155.dp, 10.dp)
                Spacer(Modifier.height(14.dp))
                HomeSkeletonLine(72.dp, 20.dp)
            }
            HomeSkeletonLine(40.dp, 40.dp)
        }
        Spacer(Modifier.height(16.dp))
        Column(Modifier.fillMaxWidth().height(168.dp).background(DesignColor.Surface, RoundedCornerShape(16.dp)).padding(16.dp)) {
            HomeSkeletonLine(100.dp, 10.dp)
            Spacer(Modifier.height(14.dp))
            HomeSkeletonLine(220.dp, 16.dp)
            Spacer(Modifier.height(16.dp))
            HomeSkeletonLine(92.dp, 10.dp)
            Spacer(Modifier.height(14.dp))
            HomeSkeletonLine(172.dp, 12.dp)
            Spacer(Modifier.height(24.dp))
            HomeSkeletonLine(200.dp, 8.dp)
            Spacer(Modifier.height(8.dp))
            HomeSkeletonLine(140.dp, 8.dp)
        }
        Spacer(Modifier.height(16.dp))
        Column(Modifier.fillMaxWidth().height(220.dp).background(DesignColor.Surface, RoundedCornerShape(16.dp)).padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    HomeSkeletonLine(112.dp, 16.dp)
                    Spacer(Modifier.height(12.dp))
                    HomeSkeletonLine(224.dp, 10.dp)
                    Spacer(Modifier.height(6.dp))
                    HomeSkeletonLine(140.dp, 10.dp)
                }
                HomeSkeletonLine(40.dp, 40.dp)
            }
            Spacer(Modifier.height(22.dp))
            Column(Modifier.fillMaxWidth().height(56.dp).background(DesignColor.NeutralSurface, RoundedCornerShape(12.dp)).padding(12.dp, 8.dp)) {
                HomeSkeletonLine(204.dp, 8.dp)
                Spacer(Modifier.height(10.dp))
                HomeSkeletonLine(119.dp, 8.dp)
            }
            Spacer(Modifier.height(24.dp))
            HomeSkeletonLine(124.dp, 10.dp)
        }
    }
}

@Composable
private fun HomeSkeletonLine(width: Dp, height: Dp) {
    Box(Modifier.width(width).height(height).background(DesignColor.Skeleton, RoundedCornerShape(6.dp)))
}