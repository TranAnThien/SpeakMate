package com.project.speakmate.ui.call

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.call.components.CallPageHeader
import com.project.speakmate.ui.call.components.CallSecondaryButton
import com.project.speakmate.ui.call.state.MicrophonePermissionPresentation
import com.project.speakmate.ui.call.state.MicrophonePermissionUiState
import com.project.speakmate.ui.components.PrimaryButton
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

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
        CallPageHeader(stringResource(when (state.presentation) {
            MicrophonePermissionPresentation.RATIONALE -> R.string.call_permission_rationale_header
            MicrophonePermissionPresentation.DENIED -> R.string.call_permission_denied_header
            MicrophonePermissionPresentation.REVOKED -> R.string.call_permission_revoked_header
        }), if (isRevoked) onExit else onBack, isClose = isRevoked)
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val contentHeight = maxHeight
            Box(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = contentHeight)
                .padding(horizontal = 24.dp).padding(top = 24.dp, bottom = if (state.presentation == MicrophonePermissionPresentation.RATIONALE) 36.dp else 44.dp),
                contentAlignment = Alignment.Center) {
                if (state.presentation == MicrophonePermissionPresentation.RATIONALE) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Image(painterResource(R.drawable.img_placement_mascot), null, Modifier.size(180.dp))
                        Text(stringResource(R.string.call_permission_rationale_title), style = DesignTypography.Heading,
                            color = DesignColor.Primary, textAlign = TextAlign.Center)
                        Text(stringResource(R.string.call_permission_rationale_description), style = DesignTypography.Body,
                            color = DesignColor.Text, textAlign = TextAlign.Center)
                        Text(stringResource(R.string.call_permission_rationale_note), style = DesignTypography.Body,
                            color = DesignColor.Muted, textAlign = TextAlign.Center)
                    }
                } else {
                    Column(Modifier.widthIn(max = 348.dp).fillMaxWidth().background(DesignColor.Surface, RoundedCornerShape(16.dp)).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(Modifier.size(64.dp).background(DesignColor.ErrorSurface, CircleShape), contentAlignment = Alignment.Center) {
                            Image(painterResource(R.drawable.ic_placement_microphone_off), null, Modifier.size(32.dp), colorFilter = ColorFilter.tint(DesignColor.Error))
                        }
                        if (!isRevoked) Text(stringResource(R.string.call_permission_required), style = DesignTypography.Badge, color = DesignColor.Error,
                            modifier = Modifier.background(DesignColor.ErrorSurface, RoundedCornerShape(12.dp)).padding(horizontal = 8.dp, vertical = 5.dp))
                        Text(stringResource(if (isRevoked) R.string.call_permission_revoked_title else R.string.call_permission_denied_title),
                            style = DesignTypography.Heading, color = DesignColor.Text, textAlign = TextAlign.Center)
                        Text(stringResource(if (isRevoked) R.string.call_permission_revoked_description else R.string.call_permission_denied_description),
                            style = DesignTypography.Body, color = DesignColor.Muted, textAlign = TextAlign.Center)
                        PrimaryButton(stringResource(R.string.call_open_settings), onOpenSettings)
                        CallSecondaryButton(stringResource(if (isRevoked) R.string.call_exit else R.string.call_recheck_permission),
                            if (isRevoked) onExit else onRecheckPermission)
                    }
                }
            }
        }
        if (state.presentation == MicrophonePermissionPresentation.RATIONALE) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PrimaryButton(stringResource(R.string.call_allow_microphone), onRequestPermission)
                CallSecondaryButton(stringResource(R.string.call_back), onBack)
            }
        }
    }
}
