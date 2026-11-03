package com.project.speakmate.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun AppBottomBar(
    onOpenTrainingPath: () -> Unit,
    onOpenPractice: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
    selectedDestination: AppDestination = AppDestination.TRAINING,
    bottomPadding: Dp = 12.dp
) {
    Row(modifier.fillMaxWidth().background(DesignColor.Surface).padding(horizontal = 24.dp)
        .padding(top = 8.dp, bottom = bottomPadding)) {
        AppNavigationItem(R.drawable.ic_navigation_path, stringResource(R.string.navigation_training),
            if (selectedDestination == AppDestination.TRAINING) DesignColor.Primary else DesignColor.Muted, onOpenTrainingPath, Modifier.weight(1f))
        AppNavigationItem(R.drawable.ic_navigation_practice, stringResource(R.string.navigation_practice),
            if (selectedDestination == AppDestination.PRACTICE) DesignColor.Primary else DesignColor.Muted, onOpenPractice, Modifier.weight(1f))
        AppNavigationItem(R.drawable.ic_navigation_history, stringResource(R.string.navigation_history),
            if (selectedDestination == AppDestination.HISTORY) DesignColor.Primary else DesignColor.Muted, onOpenHistory, Modifier.weight(1f))
        AppNavigationItem(R.drawable.ic_navigation_profile, stringResource(R.string.navigation_profile),
            if (selectedDestination == AppDestination.PROFILE) DesignColor.Primary else DesignColor.Muted, onOpenProfile, Modifier.weight(1f))
    }
}

@Composable
private fun AppNavigationItem(
    @DrawableRes iconRes: Int,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.heightIn(min = 50.dp).clickable(onClick = onClick).padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Image(painterResource(iconRes), null, Modifier.size(24.dp), colorFilter = ColorFilter.tint(color))
        Text(label, style = DesignTypography.Navigation, color = color)
    }
}
