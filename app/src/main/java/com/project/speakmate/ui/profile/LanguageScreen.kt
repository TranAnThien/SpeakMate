package com.project.speakmate.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.domain.model.AppLanguage
import com.project.speakmate.ui.components.BackAppBar
import com.project.speakmate.ui.components.SecondaryButton
import com.project.speakmate.ui.components.SpeakMateLoadingIndicator
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun LanguageScreen(
    selectedLanguage: AppLanguage,
    isLoading: Boolean,
    isSaving: Boolean,
    isSaveFailed: Boolean,
    hasLoadError: Boolean,
    onBack: () -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize().background(DesignColor.Background)) {
        BackAppBar(stringResource(R.string.language_header), onBack, isBackEnabled = !isSaving)
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
            .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AppLanguage.entries.forEach { language ->
                    val selected = !isLoading && selectedLanguage == language
                    Row(Modifier.fillMaxWidth().background(DesignColor.Surface, RoundedCornerShape(12.dp))
                        .selectable(selected, enabled = !isLoading && !isSaving && !hasLoadError,
                            role = Role.RadioButton, onClick = { onSelectLanguage(language) })
                        .heightIn(min = 56.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        RadioButton(selected, onClick = null, enabled = !isLoading && !isSaving && !hasLoadError)
                        Text(stringResource(if (language == AppLanguage.VIETNAMESE) R.string.settings_language_vietnamese else R.string.settings_language_english),
                            style = DesignTypography.Body, color = DesignColor.Text)
                    }
                }
            }
            Text(stringResource(R.string.language_content_notice), style = DesignTypography.Caption, color = DesignColor.Muted)
            if (isLoading || isSaving) {
                SpeakMateLoadingIndicator()
                Text(stringResource(if (isSaving) R.string.language_saving else R.string.language_loading),
                    style = DesignTypography.Detail, color = DesignColor.Muted)
            }
            if (isSaveFailed || hasLoadError) Text(stringResource(if (hasLoadError) R.string.language_load_failed else R.string.language_save_failed),
                style = DesignTypography.Body, color = DesignColor.Error)
            if (hasLoadError) SecondaryButton(stringResource(R.string.content_retry), onRetry)
        }
    }
}
