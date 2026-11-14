package com.project.speakmate.data.repository

import com.project.speakmate.data.session.CallSessionManager
import com.project.speakmate.domain.model.Scenario
import com.project.speakmate.domain.model.CallActivityType
import com.project.speakmate.domain.repository.CallRepository
import javax.inject.Inject

class CallRepositoryImpl @Inject constructor(private val session: CallSessionManager) : CallRepository {
    companion object {
        private const val FREE_PRACTICE_INSTRUCTION = "Activity: Free Practice."
        private const val PLACEMENT_INSTRUCTION = "Activity: Initial Placement."
        private const val REASSESSMENT_INSTRUCTION = "Activity: Promotion Reassessment."
        private const val GOAL_ASSESSMENT_INSTRUCTION = "Activity: Goal Assessment."
        private const val TRAINING_INSTRUCTION = "Activity: Guided Roleplay Training Unit."
        private const val GOAL_LABEL = "Communicative goal: "
        private const val ASSESSMENT_RESTRICTION = "Maintain the scenario role and natural conversation only. Do not correct the learner, provide model answers, suggest how to answer, hint at incomplete required actions, or scaffold their assessed performance."
    }

    override val sessionState get() = session.sessionState
    override val audioLevel get() = session.audioLevel

    override fun startCall(scenario: Scenario, activityType: CallActivityType, communicativeGoal: String) {
        val instruction = when (activityType) {
            CallActivityType.FREE_PRACTICE -> FREE_PRACTICE_INSTRUCTION
            CallActivityType.INITIAL_PLACEMENT -> PLACEMENT_INSTRUCTION
            CallActivityType.REASSESSMENT -> REASSESSMENT_INSTRUCTION
            CallActivityType.GOAL_ASSESSMENT -> GOAL_ASSESSMENT_INSTRUCTION
            CallActivityType.TRAINING_UNIT -> TRAINING_INSTRUCTION
        }
        val prompt = buildString {
            appendLine(instruction)
            append(scenario.promptTemplate)
            if (activityType != CallActivityType.FREE_PRACTICE) {
                appendLine()
                append(GOAL_LABEL).append(communicativeGoal)
                if (activityType != CallActivityType.TRAINING_UNIT) appendLine().append(ASSESSMENT_RESTRICTION)
            }
        }
        session.startCall(scenario.copy(promptTemplate = prompt), activityType, communicativeGoal)
    }
    override fun toggleMute() = session.toggleMute()
    override fun setForeground(isForeground: Boolean) = session.setForeground(isForeground)
    override fun resumeCall() = session.resumeCall()
    override fun endCall() = session.endCall()
    override fun playPronunciation(text: String) = session.playPronunciation(text)
    override fun stopPronunciation() = session.stopPronunciation()
    override fun close() = session.close()
}
