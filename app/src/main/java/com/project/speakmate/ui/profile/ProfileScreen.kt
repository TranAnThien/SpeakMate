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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.components.AppBottomBar
import com.project.speakmate.ui.components.AppDestination
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
    onOpenTrainingPath: () -> Unit,
    onOpenPractice: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState()
) {
    Column(modifier.fillMaxSize().background(DesignColor.Background)) {
        ProfileHeader(onOpenSettings)
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState,
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item(key = "profile-summary", contentType = ProfileUiState::class) {
                Column(Modifier.padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    ProfileIdentity(state.displayName, state.email, state.avatarRes)
                    Column(Modifier.fillMaxWidth().background(DesignColor.Surface, RoundedCornerShape(16.dp)).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.profile_estimated_level, state.levelLabel), style = DesignTypography.Title, color = DesignColor.Text)
                        Text(state.assessmentDescription, style = DesignTypography.Detail, color = DesignColor.Muted)
                    }
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(DesignColor.SecondaryButton)
                        .clickable(onClick = onOpenVocabulary).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.profile_saved_vocabulary), style = DesignTypography.Title, color = DesignColor.Text)
                        Text(stringResource(R.string.profile_saved_vocabulary_description), style = DesignTypography.Detail, color = DesignColor.Primary)
                    }
                }
            }
            item(key = "skills-heading", contentType = String::class) {
                Text(stringResource(R.string.profile_skills_heading), style = DesignTypography.Title, color = DesignColor.Text)
            }
            itemsIndexed(state.skills, key = { index, skill -> "skill:$index:${skill.id}" },
                contentType = { _, _ -> ProfileSkillUiState::class }) { _, skill ->
                ProfileSkillCard(skill)
            }
        }
        AppBottomBar(onOpenTrainingPath, onOpenPractice, onOpenHistory, onOpenProfile,
            selectedDestination = AppDestination.PROFILE, bottomPadding = 4.dp)
    }
}
