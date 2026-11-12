package com.project.speakmate.domain.model

data class SpeakingEvaluation(
    val range: CriterionJudgement,
    val accuracy: CriterionJudgement,
    val fluency: CriterionJudgement,
    val interaction: CriterionJudgement,
    val coherence: CriterionJudgement,
    val requiredActions: Map<String, RequiredActionJudgement>
)
