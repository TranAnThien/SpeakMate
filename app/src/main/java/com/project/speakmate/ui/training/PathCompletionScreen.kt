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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.components.header.BackAppBar
import com.project.speakmate.ui.components.lottie.AnimatedMascot
import com.project.speakmate.ui.components.lottie.MascotAnimation
import com.project.speakmate.ui.components.state.StatusContent
import com.project.speakmate.ui.components.text.DetailText
import com.project.speakmate.ui.components.text.TitleText
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography
import com.project.speakmate.ui.training.components.GoalAssessmentFooter
import com.project.speakmate.ui.training.state.PathCompletionUiState
import com.project.speakmate.utils.extension.defaultEmpty

@Composable
fun PathCompletionScreen(
    state: PathCompletionUiState,
    onBack: () -> Unit,
    onStartPromotion: () -> Unit,
    onReturnToPath: () -> Unit,
    onReviewPath: () -> Unit,
    onFreePractice: () -> Unit,
    modifier: Modifier = Modifier,
    isPromotionEnabled: Boolean = true
) {
    val levelLabel = state.level.name
    val nextLevelLabel = state.nextLevel?.name.defaultEmpty()
    Column(modifier.fillMaxSize().background(DesignColor.Background)) {
        BackAppBar(stringResource(R.string.path_completion_header), onBack)
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            StatusContent(
                title = stringResource(R.string.path_completion_title, levelLabel),
                titleStyle = DesignTypography.Heading,
                titleColor = DesignColor.Text,
                titleTextAlign = TextAlign.Center,
                description = stringResource(R.string.path_completion_runtime_description, state.level.name),
                descriptionStyle = DesignTypography.Body,
                descriptionColor = DesignColor.Muted,
                descriptionTextAlign = TextAlign.Center,
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                illustration = {
                    AnimatedMascot(animation = MascotAnimation.PATH_COMPLETE, modifier = Modifier.size(200.dp))
                    Spacer(Modifier.height(12.dp))
                },
                titleContent = { Spacer(Modifier.height(12.dp)) },
                actions = {
                    Spacer(Modifier.height(20.dp))
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(DesignColor.SecondaryButton, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TitleText(
                            text =
                                stringResource(
                                    if (state.isHighestLevel) R.string.path_completion_b2_notice_title
                                    else R.string.path_completion_a2_notice_title
                                )
                        )
                        DetailText(
                            text =
                                if (state.isHighestLevel) stringResource(R.string.path_completion_b2_notice)
                                else
                                    stringResource(
                                        R.string.path_completion_runtime_notice,
                                        state.nextLevel?.name.defaultEmpty()
                                    ),
                            color = DesignColor.Primary
                        )
                    }
                }
            )
        }
        GoalAssessmentFooter(
            primaryText =
                stringResource(
                    if (state.isHighestLevel) R.string.path_completion_review else R.string.path_completion_promote,
                    if (state.isHighestLevel) levelLabel else nextLevelLabel
                ),
            secondaryText =
                stringResource(
                    if (state.isHighestLevel) R.string.home_free_practice else R.string.goal_assessment_return_to_path
                ),
            onPrimaryAction = if (state.isHighestLevel) onReviewPath else onStartPromotion,
            onSecondaryAction = if (state.isHighestLevel) onFreePractice else onReturnToPath,
            isPrimaryEnabled = state.isHighestLevel || isPromotionEnabled
        )
    }
}
