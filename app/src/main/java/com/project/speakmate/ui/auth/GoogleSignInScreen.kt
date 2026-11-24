package com.project.speakmate.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.auth.state.GoogleSignInStatus
import com.project.speakmate.ui.auth.state.GoogleSignInUiState
import com.project.speakmate.ui.components.button.PrimaryButton
import com.project.speakmate.ui.components.button.SecondaryButton
import com.project.speakmate.ui.components.card.SpeakMateCard
import com.project.speakmate.ui.components.dialog.LegalDocumentDialog
import com.project.speakmate.ui.components.header.ContentPageHeader
import com.project.speakmate.ui.components.state.SpeakMateLoadingIndicator
import com.project.speakmate.ui.components.text.BodyText
import com.project.speakmate.ui.components.text.CaptionText
import com.project.speakmate.ui.components.text.DetailText
import com.project.speakmate.ui.components.text.HeadingText
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun GoogleSignInScreen(
    state: GoogleSignInUiState,
    onSignIn: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onRetryProfile: () -> Unit,
    onSignOut: () -> Unit,
    onDismissLegalInformation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val legalDocument = state.legalDocument
    if (legalDocument != null) {
        LegalDocumentDialog(legalDocument, onDismissLegalInformation)
    }
    if (state.isCheckingProfile || state.status == GoogleSignInStatus.PROFILE_ERROR) {
        AuthProfileContent(
            isLoading = state.isCheckingProfile,
            onRetry = onRetryProfile,
            onSignOut = onSignOut,
            modifier = modifier
        )
        return
    }
    Column(modifier.fillMaxSize().background(DesignColor.Background)) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val contentHeight = maxHeight
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .heightIn(min = contentHeight)
                        .padding(horizontal = 32.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.img_auth_logo),
                    contentDescription = null,
                    modifier = Modifier.size(160.dp).clip(RoundedCornerShape(24.dp))
                )
                Spacer(Modifier.height(24.dp))
                HeadingText(text = stringResource(R.string.app_name), style = DesignTypography.SectionTitle)
                Spacer(Modifier.height(16.dp))
                DetailText(text = stringResource(R.string.auth_tagline), textAlign = TextAlign.Center)
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (state.status) {
                GoogleSignInStatus.ERROR,
                GoogleSignInStatus.OFFLINE,
                GoogleSignInStatus.CANCELLED -> {
                    DetailText(
                        text =
                            stringResource(
                                when (state.status) {
                                    GoogleSignInStatus.OFFLINE -> R.string.auth_offline
                                    GoogleSignInStatus.CANCELLED -> R.string.auth_cancelled
                                    else -> R.string.auth_sign_in_error
                                }
                            ),
                        modifier = Modifier.fillMaxWidth(),
                        style = DesignTypography.Notice,
                        color =
                            if (state.status == GoogleSignInStatus.CANCELLED) DesignColor.Muted else DesignColor.Error,
                        textAlign = TextAlign.Center
                    )
                }
                else -> Unit
            }
            GoogleSignInButton(state.status, onSignIn)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onOpenTerms, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                    DetailText(
                        text = stringResource(R.string.auth_terms),
                        style = DesignTypography.Button,
                        textAlign = TextAlign.Center
                    )
                }
                TextButton(onClick = onOpenPrivacyPolicy, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                    DetailText(
                        text = stringResource(R.string.auth_privacy_policy),
                        style = DesignTypography.Button,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun GoogleSignInButton(status: GoogleSignInStatus, onSignIn: () -> Unit) {
    OutlinedButton(
        onClick = onSignIn,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        enabled = status != GoogleSignInStatus.LOADING && status != GoogleSignInStatus.SIGNING_OUT,
        shape = CircleShape,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
    ) {
        if (status == GoogleSignInStatus.LOADING || status == GoogleSignInStatus.SIGNING_OUT)
            SpeakMateLoadingIndicator(size = 24.dp, strokeWidth = 2.dp)
        else
            Image(
                painter = painterResource(R.drawable.ic_auth_google),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        DetailText(
            text =
                stringResource(
                    when (status) {
                        GoogleSignInStatus.LOADING -> R.string.auth_signing_in
                        GoogleSignInStatus.SIGNING_OUT -> R.string.auth_signing_out
                        GoogleSignInStatus.ERROR,
                        GoogleSignInStatus.OFFLINE,
                        GoogleSignInStatus.CANCELLED -> R.string.content_retry
                        else -> R.string.auth_continue_with_google
                    }
                ),
            modifier = Modifier.padding(start = 12.dp),
            color = DesignColor.Primary,
            style = DesignTypography.Button,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AuthProfileContent(
    isLoading: Boolean,
    onRetry: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize().background(DesignColor.Background)) {
        ContentPageHeader(stringResource(R.string.auth_profile_header))
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            SpeakMateCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = 24.dp,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CaptionText(
                    text =
                        stringResource(
                            if (isLoading) R.string.auth_profile_loading_status
                            else R.string.auth_profile_error_status
                        ),
                    style = DesignTypography.Badge,
                    color = DesignColor.Primary
                )
                HeadingText(
                    text =
                        stringResource(
                            if (isLoading) R.string.auth_profile_loading_title
                            else R.string.auth_profile_error_title
                        ),
                    style = DesignTypography.SectionTitle
                )
                BodyText(
                    text =
                        stringResource(
                            if (isLoading) R.string.auth_profile_loading_description
                            else R.string.auth_profile_error_description
                        )
                )
                if (isLoading) SpeakMateLoadingIndicator()
            }
        }
        if (!isLoading) {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PrimaryButton(stringResource(R.string.content_retry), onRetry)
                SecondaryButton(stringResource(R.string.settings_sign_out), onSignOut)
            }
        }
    }
}
