package com.project.speakmate.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.border
import com.project.speakmate.ui.components.SpeakMateLoadingIndicator
import com.project.speakmate.ui.components.PrimaryButton
import com.project.speakmate.ui.components.SecondaryButton
import java.text.DateFormat
import java.util.Date
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.ui.components.UserProfileLoadError
import com.project.speakmate.ui.profile.components.ProfileHeader
import com.project.speakmate.ui.profile.components.ProfileIdentity
import com.project.speakmate.ui.profile.components.ProfileSkillCard
import com.project.speakmate.ui.profile.state.ProfileSkillUiState
import com.project.speakmate.ui.profile.state.ProfileUiState
import com.project.speakmate.ui.theme.DesignColor
import com.project.speakmate.ui.theme.DesignTypography

@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onOpenSettings: () -> Unit,
    onOpenVocabulary: () -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    onRetryProfile: () -> Unit = {},
    onStartPlacement: () -> Unit = {}
) {
    val levelLabel = when (state.level) {
        SpeakingLevel.NOT_ASSESSED -> stringResource(R.string.profile_unassessed_label)
        SpeakingLevel.A1 -> stringResource(R.string.placement_a1_label)
        SpeakingLevel.A2 -> stringResource(R.string.placement_a2_label)
        SpeakingLevel.B1 -> stringResource(R.string.placement_b1_label)
        SpeakingLevel.B2 -> stringResource(R.string.placement_b2_label)
        null -> state.levelLabel
    }
    val description = when (state.level) {
        null -> state.assessmentDescription
        SpeakingLevel.NOT_ASSESSED -> stringResource(R.string.profile_unassessed_description)
        else -> stringResource(R.string.profile_assessed_description)
    }
    val locale = LocalConfiguration.current.locales[0]
    val assessedDate = remember(state.lastAssessedAtMillis, locale) {
        state.lastAssessedAtMillis?.let { DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(Date(it)) }
    }
    Column(modifier.fillMaxSize().background(DesignColor.Background)) {
        ProfileHeader(onOpenSettings)
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState,
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.isLoading) {
                item(key = "profile-loading", contentType = "profile-loading") { SpeakMateLoadingIndicator() }
                if (!state.hasLoaded) return@LazyColumn
            }
            if (state.isProfileLoadFailed) {
                item(key = "profile-load-error", contentType = "profile-load-error") {
                    if (state.hasLoaded) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.profile_stale_description), style = DesignTypography.Detail, color = DesignColor.Muted)
                            SecondaryButton(stringResource(R.string.content_retry), onRetryProfile)
                        }
                    } else UserProfileLoadError(onRetryProfile)
                }
                if (!state.hasLoaded) return@LazyColumn
            }
            item(key = "profile-summary", contentType = ProfileUiState::class) {
                Column(Modifier.padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    ProfileIdentity(
                        displayName = state.displayName,
                        email = state.email,
                        avatarUrl = state.avatarUrl
                    )
                    Column(Modifier.fillMaxWidth()
                        .background(DesignColor.Surface, RoundedCornerShape(16.dp))
                        .border(1.dp, DesignColor.Border, RoundedCornerShape(16.dp)).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.profile_estimated_level, levelLabel), style = DesignTypography.Title, color = DesignColor.Text)
                        Text(description, style = DesignTypography.Detail, color = DesignColor.Muted)
                        if (assessedDate != null && state.level != SpeakingLevel.NOT_ASSESSED) {
                            Text(stringResource(R.string.profile_assessed_date, assessedDate), style = DesignTypography.Detail, color = DesignColor.Muted)
                        }
                        if (state.level == SpeakingLevel.NOT_ASSESSED) {
                            PrimaryButton(stringResource(R.string.profile_start_placement), onStartPlacement)
                        }
                    }
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(DesignColor.SecondaryButton)
                        .clickable(onClick = onOpenVocabulary).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(if (state.savedVocabularyCount != null) stringResource(R.string.profile_saved_vocabulary_count, state.savedVocabularyCount)
                            else stringResource(R.string.profile_saved_vocabulary), style = DesignTypography.Title, color = DesignColor.Text)
                        Text(stringResource(R.string.profile_saved_vocabulary_description), style = DesignTypography.Detail, color = DesignColor.Primary)
                    }
                }
            }
            if (state.skills.isNotEmpty()) item(key = "skills-heading", contentType = String::class) {
                Text(stringResource(R.string.profile_skills_heading), style = DesignTypography.Title, color = DesignColor.Text)
            }
            itemsIndexed(state.skills.distinctBy { it.id }, key = { _, skill -> "skill:${skill.id}" },
                contentType = { _, _ -> ProfileSkillUiState::class }) { _, skill ->
                ProfileSkillCard(skill)
            }
        }

    }
}
