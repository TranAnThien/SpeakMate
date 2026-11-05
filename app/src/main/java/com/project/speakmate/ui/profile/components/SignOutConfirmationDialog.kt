package com.project.speakmate.ui.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.components.SecondaryButton
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun SignOutConfirmationDialog(onConfirmSignOut: () -> Unit, onCancelSignOut: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxSize(), color = DesignColor.Scrim) {
        Box(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 24.dp), contentAlignment = Alignment.Center) {
            Surface(Modifier.widthIn(max = 364.dp).fillMaxWidth().offset(y = (-16).dp),
                shape = RoundedCornerShape(24.dp), color = DesignColor.Surface) {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(Modifier.size(64.dp).background(DesignColor.ErrorSurface, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(painterResource(R.drawable.ic_profile_sign_out), null, Modifier.size(32.dp), tint = DesignColor.Error)
                    }
                    Text(stringResource(R.string.settings_sign_out_confirmation_title), style = DesignTypography.Heading,
                        color = DesignColor.Text, textAlign = TextAlign.Center)
                    Text(stringResource(R.string.settings_sign_out_confirmation_description), style = DesignTypography.Body,
                        color = DesignColor.Muted, textAlign = TextAlign.Center)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SecondaryButton(stringResource(R.string.settings_cancel_sign_out), onCancelSignOut, Modifier.weight(1f))
                        Box(Modifier.weight(1f).heightIn(min = 48.dp).clip(RoundedCornerShape(16.dp)).background(DesignColor.Error)
                            .clickable(onClick = onConfirmSignOut).padding(horizontal = 16.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.settings_sign_out), style = DesignTypography.Button, color = DesignColor.Surface, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}
