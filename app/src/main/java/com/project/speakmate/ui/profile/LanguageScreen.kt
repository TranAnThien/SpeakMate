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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.domain.model.AppLanguage
import com.project.speakmate.ui.components.button.SecondaryButton
import com.project.speakmate.ui.components.header.BackAppBar
import com.project.speakmate.ui.components.state.SpeakMateLoadingIndicator
import com.project.speakmate.ui.components.text.BodyText
import com.project.speakmate.ui.components.text.CaptionText
import com.project.speakmate.ui.components.text.DetailText
import com.project.speakmate.ui.theme.DesignColor

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
        BackAppBar(title = stringResource(R.string.language_header), onBack = onBack, isBackEnabled = !isSaving)
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AppLanguage.entries.forEach { language ->
                    val selected = !isLoading && selectedLanguage == language
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(DesignColor.Surface, RoundedCornerShape(12.dp))
                                .selectable(
                                    selected = selected,
                                    enabled = !isLoading && !isSaving && !hasLoadError,
                                    role = Role.RadioButton,
                                    onClick = { onSelectLanguage(language) }
                                )
                                .heightIn(min = 56.dp)
                                .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = null,
                            enabled = !isLoading && !isSaving && !hasLoadError
                        )
                        BodyText(
                            text =
                                stringResource(
                                    if (language == AppLanguage.VIETNAMESE)
                                        R.string.settings_language_vietnamese
                                    else R.string.settings_language_english
                                )
                        )
                    }
                }
            }
            CaptionText(text = stringResource(R.string.language_content_notice))
            if (isLoading || isSaving) {
                SpeakMateLoadingIndicator()
                DetailText(
                    text = stringResource(if (isSaving) R.string.language_saving else R.string.language_loading)
                )
            }
            if (isSaveFailed || hasLoadError)
                BodyText(
                    text =
                        stringResource(
                            if (hasLoadError) R.string.language_load_failed else R.string.language_save_failed
                        ),
                    color = DesignColor.Error
                )
            if (hasLoadError) SecondaryButton(stringResource(R.string.content_retry), onRetry)
        }
    }
}
