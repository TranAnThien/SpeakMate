package com.project.speakmate.ui.call.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun CallEndConfirmation(onConfirmEndCall: () -> Unit, onContinueCall: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxSize(), color = DesignColor.Scrim) {
        Box(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 24.dp), contentAlignment = Alignment.Center) {
            Column(Modifier.widthIn(max = 348.dp).fillMaxWidth().offset(y = (-16).dp).verticalScroll(rememberScrollState())
                .background(DesignColor.Surface, RoundedCornerShape(24.dp)).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(48.dp).background(DesignColor.ErrorSurface, CircleShape), contentAlignment = Alignment.Center) {
                    Image(painterResource(R.drawable.ic_call_end), null, Modifier.size(24.dp), colorFilter = ColorFilter.tint(DesignColor.Error))
                }
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.call_end_confirmation_title), style = DesignTypography.Heading, color = DesignColor.Text, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.call_end_confirmation_description), style = DesignTypography.Body, color = DesignColor.Muted, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(16.dp)).background(DesignColor.CallDanger)
                    .clickable(onClick = onConfirmEndCall).padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.call_confirm_end), style = DesignTypography.Button, color = DesignColor.Surface, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(12.dp))
                CallSecondaryButton(stringResource(R.string.call_continue), onContinueCall)
            }
        }
    }
}
