package com.project.speakmate.ui.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun HomeFreePracticeCard(onSelectScenario: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(DesignColor.Surface, RoundedCornerShape(16.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.home_free_practice), style = DesignTypography.Title, color = DesignColor.Text)
                Text(stringResource(R.string.home_free_practice_description), style = DesignTypography.Detail, color = DesignColor.Muted)
            }
            Box(Modifier.size(40.dp).background(DesignColor.PaleBlue, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Image(painterResource(R.drawable.ic_navigation_practice), null, Modifier.size(24.dp), colorFilter = ColorFilter.tint(DesignColor.Primary))
            }
        }
        Text(stringResource(R.string.home_free_practice_notice), style = DesignTypography.Detail, color = DesignColor.Primary,
            modifier = Modifier.fillMaxWidth().background(DesignColor.PaleBlue, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 8.dp))
        HomeTextAction(stringResource(R.string.home_select_scenario), onSelectScenario)
    }
}
