package com.project.speakmate.ui.training

import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.project.speakmate.ui.theme.SpeakMateTheme
import com.project.speakmate.ui.training.state.PreparationExampleUiState
import com.project.speakmate.ui.training.state.PreparationGrammarUiState
import com.project.speakmate.ui.training.state.PreparationModuleUiState
import com.project.speakmate.ui.training.state.PreparationOverviewUiState
import com.project.speakmate.ui.training.state.PreparationPatternUiState
import com.project.speakmate.ui.training.state.PreparationSection
import com.project.speakmate.ui.training.state.PreparationVocabularyUiState
import com.project.speakmate.ui.training.state.UnitPreparationUiState

@Preview(name = "Unit Preparation / Overview / Top / 96:616", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun PreparationOverviewScreenPreview() {
    SpeakMateTheme {
        UnitPreparationScreen(
            state = getPreparationPreviewState(PreparationSection.OVERVIEW),
            onBack = {},
            onSelectSection = {},
            onStartRoleplay = {},
            onPlayWord = {},
            onPlayPattern = {},
            onPlayExample = {}
        )
    }
}

@Preview(name = "Unit Preparation / Overview / Bottom / 97:662", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun PreparationOverviewBottomScreenPreview() {
    SpeakMateTheme {
        UnitPreparationScreen(
            state = getPreparationPreviewState(PreparationSection.OVERVIEW),
            onBack = {},
            onSelectSection = {},
            onStartRoleplay = {},
            onPlayWord = {},
            onPlayPattern = {},
            onPlayExample = {},
            scrollState = rememberScrollState(initial = with(LocalDensity.current) { 511.dp.roundToPx() })
        )
    }
}

@Preview(name = "Unit Preparation / Vocabulary / Top / 98:779", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun PreparationVocabularyScreenPreview() {
    SpeakMateTheme {
        UnitPreparationScreen(
            state = getPreparationPreviewState(PreparationSection.VOCABULARY),
            onBack = {},
            onSelectSection = {},
            onStartRoleplay = {},
            onPlayWord = {},
            onPlayPattern = {},
            onPlayExample = {}
        )
    }
}

@Preview(name = "Unit Preparation / Vocabulary / Bottom / 98:847", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun PreparationVocabularyBottomScreenPreview() {
    SpeakMateTheme {
        UnitPreparationScreen(
            state = getPreparationPreviewState(PreparationSection.VOCABULARY),
            onBack = {},
            onSelectSection = {},
            onStartRoleplay = {},
            onPlayWord = {},
            onPlayPattern = {},
            onPlayExample = {},
            scrollState = rememberScrollState(initial = with(LocalDensity.current) { 981.dp.roundToPx() })
        )
    }
}

@Preview(name = "Unit Preparation / Vocabulary / Middle / 120:2749", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun PreparationVocabularyMiddleScreenPreview() {
    SpeakMateTheme {
        UnitPreparationScreen(
            state = getPreparationPreviewState(PreparationSection.VOCABULARY),
            onBack = {},
            onSelectSection = {},
            onStartRoleplay = {},
            onPlayWord = {},
            onPlayPattern = {},
            onPlayExample = {},
            scrollState = rememberScrollState(initial = with(LocalDensity.current) { 478.5.dp.roundToPx() })
        )
    }
}

@Preview(name = "Unit Preparation / Grammar / Top / 98:899", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun PreparationGrammarScreenPreview() {
    SpeakMateTheme {
        UnitPreparationScreen(
            state = getPreparationPreviewState(PreparationSection.GRAMMAR),
            onBack = {},
            onSelectSection = {},
            onStartRoleplay = {},
            onPlayWord = {},
            onPlayPattern = {},
            onPlayExample = {}
        )
    }
}

@Preview(name = "Unit Preparation / Grammar / Bottom / 98:949", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun PreparationGrammarBottomScreenPreview() {
    SpeakMateTheme {
        UnitPreparationScreen(
            state = getPreparationPreviewState(PreparationSection.GRAMMAR),
            onBack = {},
            onSelectSection = {},
            onStartRoleplay = {},
            onPlayWord = {},
            onPlayPattern = {},
            onPlayExample = {},
            scrollState = rememberScrollState(initial = with(LocalDensity.current) { 485.dp.roundToPx() })
        )
    }
}

@Preview(name = "Unit Preparation / Sentence patterns / Top / 98:982", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun PreparationPatternsScreenPreview() {
    SpeakMateTheme {
        UnitPreparationScreen(
            state = getPreparationPreviewState(PreparationSection.SENTENCE_PATTERNS),
            onBack = {},
            onSelectSection = {},
            onStartRoleplay = {},
            onPlayWord = {},
            onPlayPattern = {},
            onPlayExample = {}
        )
    }
}

@Preview(name = "Unit Preparation / Sentence patterns / Bottom / 98:1039", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun PreparationPatternsBottomScreenPreview() {
    SpeakMateTheme {
        UnitPreparationScreen(
            state = getPreparationPreviewState(PreparationSection.SENTENCE_PATTERNS),
            onBack = {},
            onSelectSection = {},
            onStartRoleplay = {},
            onPlayWord = {},
            onPlayPattern = {},
            onPlayExample = {},
            scrollState = rememberScrollState(initial = with(LocalDensity.current) { 725.dp.roundToPx() })
        )
    }
}

@Preview(name = "Unit Preparation / Sentence patterns / Middle / 120:3013", widthDp = 396, heightDp = 835, showBackground = true)
@Composable
private fun PreparationPatternsMiddleScreenPreview() {
    SpeakMateTheme {
        UnitPreparationScreen(
            state = getPreparationPreviewState(PreparationSection.SENTENCE_PATTERNS),
            onBack = {},
            onSelectSection = {},
            onStartRoleplay = {},
            onPlayWord = {},
            onPlayPattern = {},
            onPlayExample = {},
            scrollState = rememberScrollState(initial = with(LocalDensity.current) { 350.5.dp.roundToPx() })
        )
    }
}

private fun getPreparationPreviewState(section: PreparationSection): UnitPreparationUiState = UnitPreparationUiState(
    section = section,
    contextLabel = if (section == PreparationSection.OVERVIEW) "A2 · GOAL 1 · UNIT 2 · ĐANG HỌC" else "UNIT 2 · CHUẨN BỊ BÀI HỌC",
    title = when (section) {
        PreparationSection.OVERVIEW -> "Điều chỉnh đơn gọi món"
        PreparationSection.VOCABULARY -> "Từ vựng cần biết"
        PreparationSection.GRAMMAR -> "Ngữ pháp ứng dụng"
        PreparationSection.SENTENCE_PATTERNS -> "Mẫu câu giao tiếp"
    },
    description = when (section) {
        PreparationSection.OVERVIEW -> "Học cách diễn đạt hữu ích trước khi trò chuyện với nhân viên quán cà phê."
        PreparationSection.VOCABULARY -> "5 từ hữu ích khi thay đổi đơn gọi món. Nhấn loa để nghe từ hoặc câu ví dụ."
        PreparationSection.GRAMMAR -> "Hai cấu trúc giúp bạn đề nghị thay đổi món một cách lịch sự."
        PreparationSection.SENTENCE_PATTERNS -> "Tìm hiểu ý nghĩa, cách sử dụng và nghe ví dụ trước khi thực hành."
    },
    overview = getPreparationOverviewPreviewState(),
    vocabulary = getPreparationVocabularyPreviewState(),
    grammar = getPreparationGrammarPreviewState(),
    sentencePatterns = getPreparationPatternsPreviewState()
)

private fun getPreparationOverviewPreviewState(): PreparationOverviewUiState = PreparationOverviewUiState(
    imageUrl = "https://images.unsplash.com/photo-1445116572660-236099ec97a0",
    communicativeFocus = "Yêu cầu thay đổi kích cỡ đồ uống hoặc nguyên liệu và xác nhận lại đơn sau khi điều chỉnh.",
    modules = listOf(
        PreparationModuleUiState(PreparationSection.VOCABULARY, "Từ vựng · 5 từ", "Size, extra, less, switch, dairy-free. Nghĩa, phiên âm, ví dụ và phát âm."),
        PreparationModuleUiState(PreparationSection.GRAMMAR, "Ngữ pháp · 2 cấu trúc", "Could I ...? và Would it be possible to ...? Cấu trúc, cách dùng và ví dụ."),
        PreparationModuleUiState(PreparationSection.SENTENCE_PATTERNS, "Mẫu câu · 3 mẫu", "Cách đề nghị điều chỉnh và xác nhận đơn. Ý nghĩa, cách dùng và ví dụ.")
    ),
    requiredActions = listOf("Yêu cầu thay đổi kích cỡ hoặc một thành phần của đồ uống.", "Xác nhận lại đơn sau khi nhân viên điều chỉnh.")
)

private fun getPreparationVocabularyPreviewState(): List<PreparationVocabularyUiState> = listOf(
    PreparationVocabularyUiState(
        id = "vocabulary_size",
        word = "size",
        pronunciationLabel = "/saɪz/ · danh từ",
        meaning = "kích cỡ",
        example = PreparationExampleUiState("What size would you like?", "Bạn muốn cỡ nào?")
    ),
    PreparationVocabularyUiState(
        id = "vocabulary_extra",
        word = "extra",
        pronunciationLabel = "/ˈek.strə/ · tính từ",
        meaning = "thêm; bổ sung",
        example = PreparationExampleUiState("Could I have extra ice, please?", "Cho tôi thêm đá được không?")
    ),
    PreparationVocabularyUiState(
        id = "vocabulary_less",
        word = "less",
        pronunciationLabel = "/les/ · từ hạn định",
        meaning = "ít hơn",
        example = PreparationExampleUiState("Could you make it with less sugar?", "Bạn có thể làm ít đường hơn không?")
    ),
    PreparationVocabularyUiState(
        id = "vocabulary_switch",
        word = "switch",
        pronunciationLabel = "/swɪtʃ/ · động từ",
        meaning = "đổi; chuyển sang",
        example = PreparationExampleUiState("Can I switch to oat milk?", "Tôi có thể đổi sang sữa yến mạch không?")
    ),
    PreparationVocabularyUiState(
        id = "vocabulary_dairy_free",
        word = "dairy-free",
        pronunciationLabel = "/ˌdeə.ri ˈfriː/ · tính từ",
        meaning = "không chứa sữa động vật",
        example = PreparationExampleUiState("Do you have a dairy-free option?", "Ở đây có lựa chọn không chứa sữa không?")
    )
)

private fun getPreparationGrammarPreviewState(): List<PreparationGrammarUiState> = listOf(
    PreparationGrammarUiState(
        id = "grammar_could_i",
        title = "1. Could I + verb ...?",
        explanation = "Dùng để đưa ra một yêu cầu lịch sự với nhân viên.",
        structure = "Could I + động từ nguyên mẫu + ...?",
        example = PreparationExampleUiState("Could I change my drink to a large?", "Tôi có thể đổi đồ uống sang cỡ lớn không?"),
        usageNote = "“Could I” thường lịch sự hơn “I want” trong tình huống gọi món."
    ),
    PreparationGrammarUiState(
        id = "grammar_would_it_be_possible",
        title = "2. Would it be possible to ...?",
        explanation = "Dùng khi muốn đưa ra yêu cầu nhẹ nhàng, trang trọng hơn.",
        structure = "Would it be possible to + verb ...?",
        example = PreparationExampleUiState("Would it be possible to use oat milk instead?", "Có thể dùng sữa yến mạch thay thế được không?"),
        usageNote = "Không cần dùng cấu trúc dài trong mọi câu. “Could I...?” thường là đủ."
    )
)

private fun getPreparationPatternsPreviewState(): List<PreparationPatternUiState> = listOf(
    PreparationPatternUiState(
        id = "pattern_change",
        label = "MẪU 1",
        pattern = "Could I change ... to ...?",
        meaning = "Tôi có thể đổi ... sang ... không?",
        usage = "Đề nghị đổi món hoặc kích cỡ.",
        example = PreparationExampleUiState("Could I change my latte to a large?", "Tôi có thể đổi latte sang cỡ lớn không?")
    ),
    PreparationPatternUiState(
        id = "pattern_less",
        label = "MẪU 2",
        pattern = "Could you make it with less ...?",
        meaning = "Bạn có thể giảm bớt ... không?",
        usage = "Yêu cầu giảm một nguyên liệu.",
        example = PreparationExampleUiState("Could you make it with less sugar?", "Bạn có thể làm ít đường hơn không?")
    ),
    PreparationPatternUiState(
        id = "pattern_confirm",
        label = "MẪU 3",
        pattern = "So, that’s ... , correct?",
        meaning = "Vậy là ..., đúng không?",
        usage = "Xác nhận lại thông tin sau thay đổi.",
        example = PreparationExampleUiState("So, that’s a large latte with less sugar, correct?", "Vậy là một latte cỡ lớn ít đường, đúng không?")
    )
)

