package com.project.speakmate.ui.home.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.components.SpeakMateLoadingIndicator
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun HomeLoadingContent(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(top = 12.dp, bottom = 16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SpeakMateLoadingIndicator(size = 16.dp, strokeWidth = 2.dp)
            Text(
                text = stringResource(R.string.home_loading),
                style = DesignTypography.Navigation,
                color = DesignColor.Muted,
                modifier = Modifier.weight(1f),
                maxLines = 2
            )
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
