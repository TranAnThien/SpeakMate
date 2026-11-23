package com.project.speakmate.ui.profile.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
    Row(modifier.fillMaxWidth().heightIn(min = 64.dp).padding(start = 24.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.navigation_profile), style = DesignTypography.PageTitle, color = DesignColor.Primary,
            modifier = Modifier.weight(1f).padding(vertical = 18.dp))
        IconButton(onClick = onOpenSettings, modifier = Modifier.size(48.dp)) {
            Icon(painterResource(R.drawable.ic_profile_settings), stringResource(R.string.settings_header), Modifier.size(24.dp), tint = DesignColor.Text)
        }
    }
}
