package com.project.speakmate.ui.profile.components

import androidx.compose.foundation.layout.PaddingValues
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
import com.project.speakmate.ui.components.header.ContentPageHeader
import com.project.speakmate.ui.theme.DesignColor

@Composable
fun ProfileHeader(onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().heightIn(min = 64.dp), verticalAlignment = Alignment.CenterVertically) {
        ContentPageHeader(
            title = stringResource(R.string.navigation_profile),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 24.dp, top = 18.dp, bottom = 18.dp)
        )
        IconButton(onClick = onOpenSettings, modifier = Modifier.padding(end = 16.dp).size(48.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_profile_settings),
                contentDescription = stringResource(R.string.settings_header),
                modifier = Modifier.size(24.dp),
                tint = DesignColor.Text
            )
        }
    }
}
