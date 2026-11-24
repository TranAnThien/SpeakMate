package com.project.speakmate.ui.placement

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.domain.model.ReassessmentStep
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.ui.components.button.PrimaryButton
import com.project.speakmate.ui.components.card.SpeakMateCard
import com.project.speakmate.ui.components.header.BackAppBar
import com.project.speakmate.ui.components.text.BodyText
import com.project.speakmate.ui.components.text.CaptionText
import com.project.speakmate.ui.components.text.DetailText
import com.project.speakmate.ui.components.text.HeadingText
import com.project.speakmate.ui.components.text.TitleText
import com.project.speakmate.ui.placement.components.PromotionStepsCard
import com.project.speakmate.ui.placement.state.PromotionIntroductionUiState
import com.project.speakmate.ui.placement.state.PromotionStepUiState
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun PromotionIntroductionScreen(
    state: PromotionIntroductionUiState,
    onBack: () -> Unit,
    onStartAssessment: () -> Unit,
    onReturnToPath: () -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState()
) {
    Column(modifier.fillMaxSize().background(DesignColor.Background)) {
        BackAppBar(stringResource(R.string.promotion_header), onBack)
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (state.currentLevel != SpeakingLevel.NOT_ASSESSED)
                CaptionText(
                    text = stringResource(R.string.assessment_completed_path, state.currentLevel.name),
                    style = DesignTypography.Label
                )
            HeadingText(
                text =
                    stringResource(
                        R.string.promotion_runtime_introduction_title,
                        state.currentLevel.name,
                        state.nextLevel.name
                    )
            )
            SpeakMateCard(
                modifier =
                    Modifier.fillMaxWidth()
                        .border(width = 1.dp, color = DesignColor.Border, shape = RoundedCornerShape(16.dp)),
                contentPadding = 16.dp,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TitleText(text = stringResource(R.string.assessment_each_call_heading))
                BodyText(
                    text =
                        stringResource(
                            R.string.promotion_runtime_introduction_description,
                            state.currentLevel.name,
                            state.nextLevel.name
                        ),
                    color = DesignColor.Muted
                )
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(DesignColor.SurfaceContainer, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TitleText(
                    text = stringResource(R.string.promotion_runtime_condition_title, state.nextLevel.name)
                )
                BodyText(
                    text =
                        stringResource(
                            R.string.promotion_runtime_condition_description,
                            state.currentLevel.name,
                            state.nextLevel.name
                        ),
                    color = DesignColor.Muted
                )
                DetailText(text = stringResource(R.string.placement_result_disclaimer))
            }
            PromotionStepsCard(
                steps =
                    ReassessmentStep.entries.mapIndexed { index, step ->
                        PromotionStepUiState(
                            id = step.name,
                            number = index + 1,
                            text =
                                stringResource(
                                    when (step) {
                                        ReassessmentStep.CURRENT_CONFIRMATION ->
                                            R.string.promotion_runtime_current_confirmation
                                        ReassessmentStep.NEXT_PROBE -> R.string.promotion_runtime_next_probe
                                        ReassessmentStep.NEXT_CONFIRMATION ->
                                            R.string.promotion_runtime_next_confirmation
                                    },
                                    if (step == ReassessmentStep.CURRENT_CONFIRMATION) state.currentLevel.name
                                    else state.nextLevel.name
                                )
                        )
                    }
            )
        }
        Column(
            Modifier
                .fillMaxWidth()
                .background(DesignColor.Surface)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PrimaryButton(stringResource(R.string.placement_start), onStartAssessment)
            TextButton(onClick = onReturnToPath, modifier = Modifier.fillMaxWidth()) {
                DetailText(
                    text = stringResource(R.string.goal_assessment_return_to_path),
                    style = DesignTypography.Button
                )
            }
        }
    }
}
