package com.project.speakmate.ui.training

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography
import com.project.speakmate.ui.training.components.GoalAssessmentFooter
import com.project.speakmate.ui.training.components.PreparationHeader
import com.project.speakmate.ui.training.state.PathCompletionUiState

@Composable
fun PathCompletionScreen(
    state: PathCompletionUiState,
    onBack: () -> Unit,
    onStartPromotion: () -> Unit,
    onReturnToPath: () -> Unit,
    onReviewPath: () -> Unit,
    onFreePractice: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize().background(DesignColor.Background)) {
        PreparationHeader(stringResource(R.string.path_completion_header), onBack)
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(80.dp).background(DesignColor.ResultSuccessSurface, RoundedCornerShape(36.dp)), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_placement_passed), null, Modifier.size(32.dp), tint = DesignColor.Success)
                }
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.path_completion_title, state.levelLabel), style = DesignTypography.Heading,
                    color = DesignColor.Text, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text(state.description, style = DesignTypography.Body, color = DesignColor.Muted, textAlign = TextAlign.Center)
                Spacer(Modifier.height(20.dp))
                Column(Modifier.fillMaxWidth().background(DesignColor.SecondaryButton, RoundedCornerShape(16.dp)).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(state.noticeTitle, style = DesignTypography.Title, color = DesignColor.Text)
                    Text(state.notice, style = DesignTypography.Detail, color = DesignColor.Primary)
                }
            }
        }
        GoalAssessmentFooter(
            stringResource(if (state.isHighestLevel) R.string.path_completion_review else R.string.path_completion_promote,
                if (state.isHighestLevel) state.levelLabel else state.nextLevelLabel),
            stringResource(if (state.isHighestLevel) R.string.home_free_practice else R.string.goal_assessment_return_to_path),
            if (state.isHighestLevel) onReviewPath else onStartPromotion,
            if (state.isHighestLevel) onFreePractice else onReturnToPath
        )
    }
}
