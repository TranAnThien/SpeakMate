package com.project.speakmate.domain.model

data class UnitPreparation(
    val vocabulary: List<VocabularyItem>,
    val grammarPoints: List<GrammarPoint>,
    val sentencePatterns: List<SentencePattern>
)