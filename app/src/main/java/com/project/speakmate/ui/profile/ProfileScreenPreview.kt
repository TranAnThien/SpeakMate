package com.project.speakmate.ui.profile

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.project.speakmate.R
import com.project.speakmate.ui.profile.state.ProfileSkillUiState
import com.project.speakmate.ui.profile.state.ProfileUiState
import com.project.speakmate.ui.theme.SpeakMateTheme

@Preview(name = "Profile / Default / Audited / 17:810", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun ProfileDefaultScreenPreview() {
    SpeakMateTheme {
        ProfileScreen(
            state = rememberProfilePreviewState(isLongName = false),
            onOpenSettings = {},
            onOpenVocabulary = {},
            onOpenTrainingPath = {},
            onOpenPractice = {},
            onOpenHistory = {},
            onOpenProfile = {}
        )
    }
}

@Preview(name = "Profile / Scroll / Bottom / 131:6666", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun ProfileBottomScreenPreview() {
    SpeakMateTheme {
        ProfileScreen(
            state = rememberProfilePreviewState(isLongName = false),
            onOpenSettings = {},
            onOpenVocabulary = {},
            onOpenTrainingPath = {},
            onOpenPractice = {},
            onOpenHistory = {},
            onOpenProfile = {},
            listState = rememberLazyListState(initialFirstVisibleItemScrollOffset = with(LocalDensity.current) { 341.dp.roundToPx() })
        )
    }
}

@Preview(name = "Profile / Long Name / Missing Photo / Default / Audited / 33:2974", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun ProfileLongNameScreenPreview() {
    SpeakMateTheme {
        ProfileScreen(
            state = rememberProfilePreviewState(isLongName = true),
            onOpenSettings = {},
            onOpenVocabulary = {},
            onOpenTrainingPath = {},
            onOpenPractice = {},
            onOpenHistory = {},
            onOpenProfile = {}
        )
    }
}

@Preview(name = "Profile / Long Name / Missing Photo / Scroll / Bottom / 131:6799", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun ProfileLongNameBottomScreenPreview() {
    SpeakMateTheme {
        ProfileScreen(
            state = rememberProfilePreviewState(isLongName = true),
            onOpenSettings = {},
            onOpenVocabulary = {},
            onOpenTrainingPath = {},
            onOpenPractice = {},
            onOpenHistory = {},
            onOpenProfile = {},
            listState = rememberLazyListState(initialFirstVisibleItemScrollOffset = with(LocalDensity.current) { 373.dp.roundToPx() })
        )
    }
}

@Composable
private fun rememberProfilePreviewState(isLongName: Boolean): ProfileUiState = ProfileUiState(
    displayName = stringResource(if (isLongName) R.string.profile_preview_long_name else R.string.profile_preview_display_name),
    email = stringResource(R.string.profile_preview_email),
    avatarRes = if (isLongName) null else R.drawable.img_profile_avatar,
    levelLabel = "A2",
    assessmentDescription = stringResource(R.string.profile_preview_assessment_description),
    skills = listOf(
        ProfileSkillUiState("range", stringResource(R.string.profile_preview_skill_0_title),
            stringResource(R.string.profile_preview_skill_0_description), true),
        ProfileSkillUiState("accuracy", stringResource(R.string.profile_preview_skill_1_title),
            stringResource(R.string.profile_preview_skill_1_description), false),
        ProfileSkillUiState("fluency", stringResource(R.string.profile_preview_skill_2_title),
            stringResource(R.string.profile_preview_skill_2_description), true),
        ProfileSkillUiState("interaction", stringResource(R.string.profile_preview_skill_3_title),
            stringResource(R.string.profile_preview_skill_3_description), true),
        ProfileSkillUiState("coherence", stringResource(R.string.profile_preview_skill_4_title),
            stringResource(R.string.profile_preview_skill_4_description), false)
    )
)
