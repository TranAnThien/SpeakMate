package com.project.speakmate.ui.training.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.ui.theme.DesignColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.project.speakmate.R
import com.project.speakmate.ui.components.BackAppBar
import com.project.speakmate.ui.components.ContentPageHeader

@Composable
fun TrainingHeader(onShowLevelInformation: () -> Unit, shouldShowInformation: Boolean, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        ContentPageHeader(stringResource(R.string.navigation_training), Modifier.weight(1f))
        if (shouldShowInformation) {
            IconButton(onClick = onShowLevelInformation, modifier = Modifier.padding(end = 16.dp).size(48.dp)) {
                Icon(painterResource(R.drawable.ic_placement_info), stringResource(R.string.training_level_information_action),
                    Modifier.size(24.dp), tint = DesignColor.Text)
            }
        }
    }
}

@Composable
fun GoalDetailHeader(onBack: () -> Unit, modifier: Modifier = Modifier) {
    BackAppBar(stringResource(R.string.training_goal_detail_title), onBack, modifier)
}
