package com.project.speakmate.ui.training.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import com.project.speakmate.ui.components.SpeakMateLoadingIndicator
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography
import com.project.speakmate.ui.training.state.UnitResultPresentation

@Composable
fun UnitResultOutcome(
    presentation: UnitResultPresentation,
    explanation: String,
    modifier: Modifier = Modifier
) {
    val icon: Int? = when (presentation) {
        UnitResultPresentation.PASS -> R.drawable.ic_training_result_check
        UnitResultPresentation.PARTIAL -> R.drawable.ic_training_result_partial
        UnitResultPresentation.FAIL_REQUIRED_ACTION, UnitResultPresentation.FAIL_CRITERIA -> R.drawable.ic_call_close
        UnitResultPresentation.INSUFFICIENT_EVIDENCE -> R.drawable.ic_placement_microphone_off
        UnitResultPresentation.PROCESSING -> null
    }
    val foreground = when (presentation) {
        UnitResultPresentation.PASS -> DesignColor.Success
        UnitResultPresentation.PARTIAL, UnitResultPresentation.INSUFFICIENT_EVIDENCE -> DesignColor.ResultWarning
        UnitResultPresentation.FAIL_REQUIRED_ACTION, UnitResultPresentation.FAIL_CRITERIA -> DesignColor.Error
        UnitResultPresentation.PROCESSING -> DesignColor.Primary
    }
    val background = when (presentation) {
        UnitResultPresentation.PASS -> DesignColor.ResultSuccessSurface
        UnitResultPresentation.PARTIAL, UnitResultPresentation.INSUFFICIENT_EVIDENCE -> DesignColor.ResultWarningSurface
        UnitResultPresentation.FAIL_REQUIRED_ACTION, UnitResultPresentation.FAIL_CRITERIA -> DesignColor.ErrorSurface
        UnitResultPresentation.PROCESSING -> DesignColor.IconBlue
    }
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(56.dp).background(background, CircleShape), contentAlignment = Alignment.Center) {
            if (presentation == UnitResultPresentation.PROCESSING) {
                SpeakMateLoadingIndicator(size = 32.dp)
            } else {
                Icon(painterResource(requireNotNull(icon)), null, Modifier.size(32.dp), tint = foreground)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(stringResource(when (presentation) {
            UnitResultPresentation.PASS -> R.string.placement_passed
            UnitResultPresentation.PARTIAL -> R.string.unit_result_partial
            UnitResultPresentation.FAIL_REQUIRED_ACTION -> R.string.unit_result_missing_action
            UnitResultPresentation.FAIL_CRITERIA -> R.string.placement_not_passed
            UnitResultPresentation.INSUFFICIENT_EVIDENCE -> R.string.unit_result_insufficient
            UnitResultPresentation.PROCESSING -> R.string.unit_result_processing
        }), style = DesignTypography.Heading, color = DesignColor.Text, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(explanation, style = DesignTypography.Body, color = DesignColor.Muted, textAlign = TextAlign.Center)
    }
}
