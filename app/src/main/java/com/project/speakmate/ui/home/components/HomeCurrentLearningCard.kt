package com.project.speakmate.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun HomeCurrentLearningCard(goalTitle: String, unitTitle: String, unitDescription: String) {
    Column(Modifier.fillMaxWidth().background(DesignColor.Surface, RoundedCornerShape(16.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.home_current_learning), style = DesignTypography.Label, color = DesignColor.Muted)
        Text(goalTitle, style = DesignTypography.Title, color = DesignColor.Text)
        Text(stringResource(R.string.home_current_unit), style = DesignTypography.Label, color = DesignColor.Muted)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(unitTitle, style = DesignTypography.Title, color = DesignColor.Text, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.training_current), style = DesignTypography.Badge, color = DesignColor.Primary,
                modifier = Modifier.background(DesignColor.IconBlue, RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 7.dp))
        }
        Text(unitDescription, style = DesignTypography.Detail, color = DesignColor.Muted)
    }
}
