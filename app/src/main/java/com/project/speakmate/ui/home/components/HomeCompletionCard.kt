package com.project.speakmate.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.components.PrimaryButton
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun HomeCompletionCard(
    level: String,
    nextLevel: String,
    isHighestLevel: Boolean,
    onStartReassessment: () -> Unit,
    onReviewPath: () -> Unit
) {
    Column(Modifier.fillMaxWidth().background(DesignColor.Surface, RoundedCornerShape(16.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.home_path_completed, level), style = DesignTypography.Title, color = DesignColor.Text)
        Text(if (isHighestLevel) stringResource(R.string.home_highest_level_description)
        else stringResource(R.string.home_path_completed_description, nextLevel),
            style = DesignTypography.Detail, color = DesignColor.Muted)
        PrimaryButton(if (isHighestLevel) stringResource(R.string.home_review_content)
        else stringResource(R.string.home_reassess, nextLevel), if (isHighestLevel) onReviewPath else onStartReassessment)
    }
}
