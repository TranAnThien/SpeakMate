package com.project.speakmate.ui.call

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import com.project.speakmate.ui.call.state.MicrophonePermissionPresentation
import com.project.speakmate.ui.call.state.MicrophonePermissionUiState
import com.project.speakmate.ui.components.button.PrimaryButton
import com.project.speakmate.ui.components.button.SecondaryButton
import com.project.speakmate.ui.components.card.SpeakMateCard
import com.project.speakmate.ui.components.header.BackAppBar
import com.project.speakmate.ui.components.image.CircleIcon
import com.project.speakmate.ui.components.lottie.AnimatedMascot
import com.project.speakmate.ui.components.lottie.MascotAnimation
import com.project.speakmate.ui.components.state.StatusContent
import com.project.speakmate.ui.components.text.BodyText
import com.project.speakmate.ui.components.text.CaptionText
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography
import com.project.speakmate.utils.AppConstant

@Composable
fun MicrophonePermissionScreen(
    state: MicrophonePermissionUiState,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onRecheckPermission: () -> Unit,
    onBack: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRevoked = state.presentation == MicrophonePermissionPresentation.REVOKED
    Column(modifier.fillMaxSize().background(DesignColor.Background)) {
        BackAppBar(
            title =
                stringResource(
                    when (state.presentation) {
                        MicrophonePermissionPresentation.RATIONALE -> R.string.call_permission_rationale_header
                        MicrophonePermissionPresentation.DENIED -> R.string.call_permission_denied_header
                        MicrophonePermissionPresentation.REVOKED -> R.string.call_permission_revoked_header
                    }
                ),
            onBack = if (isRevoked) onExit else onBack,
            isClose = isRevoked
        )
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val contentHeight = maxHeight
            Box(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = contentHeight)
                    .padding(horizontal = 24.dp)
                    .padding(
                        top = 24.dp,
                        bottom =
                            if (state.presentation == MicrophonePermissionPresentation.RATIONALE) 36.dp else 44.dp
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (state.presentation == MicrophonePermissionPresentation.RATIONALE) {
                    StatusContent(
                        title = stringResource(R.string.call_permission_rationale_title),
                        titleStyle = DesignTypography.Heading,
                        titleColor = DesignColor.Primary,
                        titleTextAlign = TextAlign.Center,
                        description = stringResource(R.string.call_permission_rationale_description),
                        descriptionStyle = DesignTypography.Body,
                        descriptionColor = DesignColor.Text,
                        descriptionTextAlign = TextAlign.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        illustration = {
                            AnimatedMascot(
                                animation = MascotAnimation.CALL_MASCOT,
                                modifier = Modifier.size(180.dp),
                                marker = AppConstant.CALL_MASCOT_IDLE_MARKER
                            )
                        },
                        actions = {
                            BodyText(
                                text = stringResource(R.string.call_permission_rationale_note),
                                color = DesignColor.Muted,
                                textAlign = TextAlign.Center
                            )
                        }
                    )
                } else {
                    SpeakMateCard(
                        modifier = Modifier.widthIn(max = 348.dp).fillMaxWidth(),
                        contentPadding = 24.dp
                    ) {
                        StatusContent(
                            title =
                                stringResource(
                                    if (isRevoked) R.string.call_permission_revoked_title
                                    else R.string.call_permission_denied_title
                                ),
                            titleStyle = DesignTypography.Heading,
                            titleColor = DesignColor.Text,
                            titleTextAlign = TextAlign.Center,
                            description =
                                stringResource(
                                    if (isRevoked) R.string.call_permission_revoked_description
                                    else R.string.call_permission_denied_description
                                ),
                            descriptionStyle = DesignTypography.Body,
                            descriptionColor = DesignColor.Muted,
                            descriptionTextAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            illustration = {
                                CircleIcon(
                                    iconRes = R.drawable.ic_placement_microphone_off,
                                    backgroundColor = DesignColor.ErrorSurface,
                                    tint = DesignColor.Error
                                )
                                if (!isRevoked)
                                    CaptionText(
                                        text = stringResource(R.string.call_permission_required),
                                        style = DesignTypography.Badge,
                                        color = DesignColor.Error,
                                        modifier =
                                            Modifier.background(
                                                DesignColor.ErrorSurface,
                                                RoundedCornerShape(12.dp)
                                            )
                                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                    )
                            },
                            actions = {
                                PrimaryButton(stringResource(R.string.call_open_settings), onOpenSettings)
                                SecondaryButton(
                                    stringResource(
                                        if (isRevoked) R.string.call_exit
                                        else R.string.call_recheck_permission
                                    ),
                                    if (isRevoked) onExit else onRecheckPermission
                                )
                            }
                        )
                    }
                }
            }
        }
        if (state.presentation == MicrophonePermissionPresentation.RATIONALE) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PrimaryButton(stringResource(R.string.call_allow_microphone), onRequestPermission)
                SecondaryButton(stringResource(R.string.call_back), onBack)
            }
        }
    }
}
