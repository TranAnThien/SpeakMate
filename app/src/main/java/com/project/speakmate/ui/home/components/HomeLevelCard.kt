package com.project.speakmate.ui.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun HomeLevelCard(level: String, isUnassessed: Boolean) {
    Column(Modifier.fillMaxWidth().background(DesignColor.Surface, RoundedCornerShape(16.dp)).padding(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.placement_estimated_level), style = DesignTypography.Label, color = DesignColor.Muted)
                Text(if (isUnassessed) stringResource(R.string.home_unassessed) else level,
                    style = DesignTypography.Brand, color = DesignColor.Primary)
            }
            Box(Modifier.size(40.dp).background(DesignColor.PaleBlue, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Image(painterResource(if (isUnassessed) R.drawable.ic_placement_microphone else R.drawable.ic_home_speaking),
                    null, Modifier.size(24.dp))
            }
        }
        if (isUnassessed) {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.home_assessment_summary), style = DesignTypography.Detail, color = DesignColor.Muted)
        }
    }
}
