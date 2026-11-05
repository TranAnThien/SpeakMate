package com.project.speakmate.ui.profile.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun ProfileHeader(onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(48.dp).padding(horizontal = 16.dp)) {
        Text(stringResource(R.string.navigation_profile), style = DesignTypography.PageTitle, color = DesignColor.Primary,
            modifier = Modifier.padding(start = 8.dp, top = 7.dp))
        Box(Modifier.align(Alignment.TopEnd).size(48.dp).clickable(onClick = onOpenSettings), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_profile_settings), null, Modifier.size(24.dp), tint = DesignColor.Text)
        }
    }
}
