package com.project.speakmate.utils

object AppConstant {
    const val GEMINI_API_KEY = "GEMINI_API_KEY"
    const val CALL_INPUT_SAMPLE_RATE = 16_000
    const val CALL_OUTPUT_SAMPLE_RATE = 24_000
    const val PCM16_BYTES_PER_SAMPLE = 2
    const val BITS_PER_BYTE = 8
    const val MILLIS_PER_SECOND = 1_000
    const val SECONDS_PER_MINUTE = 60L
    const val STATE_SUBSCRIPTION_TIMEOUT_MILLIS = 5_000L
    const val TOO_MANY_REQUESTS = 429
    const val GOAL_ID_ARGUMENT = "goalId"
    const val IS_REPLAY_ARGUMENT = "isReplay"
    const val GOAL_DETAIL_ROUTE = "training_goal/{goalId}"
    const val GOAL_ASSESSMENT_ROUTE = "goal_assessment/{goalId}"
    const val GOAL_ASSESSMENT_CALL_ROUTE = "goal_assessment_call/{goalId}"
    const val GOAL_ASSESSMENT_RESULT_ROUTE = "goal_assessment_result/{goalId}/{attemptId}/{isReplay}"
    const val PATH_COMPLETION_ROUTE = "training_path_completed"
    const val UNIT_ID_ARGUMENT = "unitId"
    const val ATTEMPT_ID_ARGUMENT = "attemptId"
    const val TRAINING_PATH_ROUTE = "training_path"
    const val UNIT_PREPARATION_ROUTE = "training_unit/{unitId}"
    const val UNIT_CALL_ROUTE = "training_unit_call/{unitId}"
    const val UNIT_RESULT_ROUTE = "training_unit_result/{unitId}/{attemptId}"
}
