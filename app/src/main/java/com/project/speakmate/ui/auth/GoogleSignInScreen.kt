package com.project.speakmate.ui.auth

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.auth.state.GoogleSignInStatus
import com.project.speakmate.ui.auth.state.GoogleSignInUiState
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography
import com.project.speakmate.ui.theme.SpeakMateTheme

@Composable
fun GoogleSignInScreen(
    state: GoogleSignInUiState,
    onSignIn: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier.fillMaxSize().background(DesignColor.Background)) {
        val contentHeight = maxHeight
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .heightIn(min = contentHeight).padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.fillMaxWidth().heightIn(min = (contentHeight - 67.dp).coerceAtLeast(0.dp)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painterResource(R.drawable.img_auth_logo), contentDescription = null,
                        modifier = Modifier.size(160.dp).clip(RoundedCornerShape(24.dp))
                    )
                    Spacer(Modifier.height(24.dp))
                    Text(stringResource(R.string.auth_tagline), style = DesignTypography.Tagline,
                        color = DesignColor.Muted, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(64.dp))
                    GoogleSignInButton(state.status, onSignIn)
                    if (state.status == GoogleSignInStatus.ERROR) {
                        Spacer(Modifier.height(16.dp))
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 50.dp)
                                .background(DesignColor.ErrorSurface, RoundedCornerShape(12.dp))
                                .border(1.dp, DesignColor.ErrorBorder, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(painterResource(R.drawable.ic_auth_error), null, Modifier.size(20.dp, 24.dp))
                            Text(stringResource(R.string.auth_sign_in_error), style = DesignTypography.Notice,
                                color = DesignColor.Error)
                        }
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.auth_terms), style = DesignTypography.Caption,
                    color = DesignColor.Muted, textAlign = TextAlign.Center,
                    modifier = Modifier.clickable(onClick = onOpenTerms).padding(horizontal = 6.dp, vertical = 17.dp))
                Text(stringResource(R.string.auth_privacy_policy), style = DesignTypography.Caption,
                    color = DesignColor.Muted, textAlign = TextAlign.Center,
                    modifier = Modifier.clickable(onClick = onOpenPrivacyPolicy).padding(horizontal = 6.dp, vertical = 17.dp))
            }
        }
    }
}

@Composable
private fun GoogleSignInButton(status: GoogleSignInStatus, onSignIn: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "loading_icon_rotation"
    )

    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .clip(RoundedCornerShape(16.dp)).background(DesignColor.Surface)
            .border(1.dp, DesignColor.Border, RoundedCornerShape(16.dp))
            .clickable(enabled = status != GoogleSignInStatus.LOADING, onClick = onSignIn)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val iconModifier = if (status == GoogleSignInStatus.LOADING) {
            Modifier.size(24.dp).rotate(rotation)
        } else {
            Modifier.size(24.dp)
        }

        Image(
            painter = painterResource(if (status == GoogleSignInStatus.LOADING) R.drawable.ic_auth_progress else R.drawable.ic_auth_google),
            contentDescription = null,
            modifier = iconModifier
        )
        Text(
            text = stringResource(if (status == GoogleSignInStatus.LOADING) R.string.auth_signing_in else R.string.auth_continue_with_google),
            color = DesignColor.Primary,
            style = DesignTypography.Button
        )
    }
}

@Preview(name = "Google sign-in / Normal / 16:2", widthDp = 396, heightDp = 819, showBackground = true)
@Composable
private fun GoogleSignInScreenPreview() {
    SpeakMateTheme {
        GoogleSignInScreen(GoogleSignInUiState(), {}, {}, {})
    }
}

@Preview(name = "Google sign-in / Loading / 33:2942", widthDp = 396, heightDp = 819, showBackground = true)
@Composable
private fun GoogleSignInLoadingScreenPreview() {
    SpeakMateTheme {
        GoogleSignInScreen(GoogleSignInUiState(GoogleSignInStatus.LOADING), {}, {}, {})
    }
}

@Preview(name = "Google sign-in / Error / 17:24", widthDp = 396, heightDp = 819, showBackground = true)
@Composable
private fun GoogleSignInErrorScreenPreview() {
    SpeakMateTheme {
        GoogleSignInScreen(GoogleSignInUiState(GoogleSignInStatus.ERROR), {}, {}, {})
    }
}
