package com.project.speakmate.ui.profile

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
            onOpenVocabulary = {}
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
            onOpenVocabulary = {}
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
            listState = rememberLazyListState(initialFirstVisibleItemScrollOffset = with(LocalDensity.current) { 373.dp.roundToPx() })
        )
    }
}

@Composable
private fun rememberProfilePreviewState(isLongName: Boolean): ProfileUiState = ProfileUiState(
    displayName = if (isLongName) "Đặng Trần Anh Quân Nguyễn Hoàng Minh" else "Đặng Trần Anh Quân",
    email = "quandang@example.com",
    avatarUrl = null,
    levelLabel = "A2",
    assessmentDescription = "Đây là kết quả ước lượng phục vụ học tập.\nĐánh giá gần nhất: 07/10/2026",
    skills = listOf(
        ProfileSkillUiState("range", "Range (Từ vựng)",
            "Sử dụng từ vựng đa dạng, linh hoạt", true),
        ProfileSkillUiState("accuracy", "Accuracy (Chính xác)",
            "Ngữ pháp và phát âm đúng chuẩn", false),
        ProfileSkillUiState("fluency", "Fluency (Trôi chảy)",
            "Khả năng duy trì lời nói tương đối trôi chảy", true),
        ProfileSkillUiState("interaction", "Interaction (Tương tác)",
            "Khả năng phản hồi và dẫn dắt", true),
        ProfileSkillUiState("coherence", "Coherence (Gắn kết)",
            "Tính liên kết giữa các câu", false)
    )
)
