package com.project.speakmate.data.session

import com.project.speakmate.domain.model.AssessmentOutcome
import com.project.speakmate.domain.model.AssessmentResult
import com.project.speakmate.domain.model.CallActivityContext
import com.project.speakmate.domain.model.CallOutcome
import com.project.speakmate.domain.model.CallSessionState
import com.project.speakmate.domain.model.CallState
import com.project.speakmate.domain.model.CompletedPractice
import com.project.speakmate.domain.model.CriterionJudgement
import com.project.speakmate.domain.model.RequiredAction
import com.project.speakmate.domain.model.RequiredActionJudgement
import com.project.speakmate.domain.model.Scenario
import com.project.speakmate.domain.model.SenderType
import com.project.speakmate.domain.model.SpeakingCriteriaResult
import com.project.speakmate.domain.model.SpeakingEvaluation
import com.project.speakmate.domain.model.SpeakingLevel
import com.project.speakmate.domain.model.TrainingActivity
import com.project.speakmate.domain.model.TrainingActivityScenario
import com.project.speakmate.domain.model.TranscriptMessage

object PracticeCompletionTestFixtures {
    const val USER_ID = "user-1"
    const val AUDIO_ID = "audio"
    const val START_MILLIS = 1_000L
    const val END_MILLIS = 8_000L
    const val EPOCH_START_MILLIS = 1_700_000_000_000L
    const val ACTION_ID = "action"
    const val FEEDBACK = "Keep the conversation natural."

    val scenario = Scenario("scenario-1", "Coffee", "Order coffee", "Act as a barista")
    val training = TrainingActivityScenario(
        scenario, TrainingActivity.UnitActivity(0, 0), SpeakingLevel.B1, "Order coffee",
        listOf(RequiredAction(ACTION_ID, "Order a drink")), SpeakingLevel.B1.name, AssessmentResult.PASS.name
    )

    fun practice(isFreePractice: Boolean = false): CompletedPractice = CompletedPractice(
        if (isFreePractice) CallActivityContext.FreePractice(scenario) else CallActivityContext.Training(training),
        CallSessionState(
            state = CallState.ENDED,
            startedAtMillis = START_MILLIS,
            endedAtMillis = END_MILLIS,
            startedAtEpochMillis = EPOCH_START_MILLIS,
            outcome = CallOutcome.COMPLETED,
            hasUserSpeech = true,
            transcript = listOf(TranscriptMessage("message-1", SenderType.USER, "A large coffee please", 0, true, false)),
            assessmentAudioId = if (isFreePractice) null else AUDIO_ID,
            assessmentAudioDurationMillis = END_MILLIS - START_MILLIS
        )
    )

    fun evaluated(
        criteria: SpeakingCriteriaResult = SpeakingCriteriaResult(true, true, true, true, true),
        isActionCompleted: Boolean = true
    ): AssessmentOutcome.Evaluated {
        val judgement = CriterionJudgement(true, "A large coffee please", FEEDBACK, null, null)
        return AssessmentOutcome.Evaluated(
            AssessmentResult.PASS,
            criteria,
            mapOf(ACTION_ID to isActionCompleted),
            SpeakingEvaluation(judgement, judgement, judgement, judgement, judgement, mapOf(ACTION_ID to RequiredActionJudgement(isActionCompleted, "A large coffee please")))
        )
    }
}
