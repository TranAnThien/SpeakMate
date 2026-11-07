package com.project.speakmate.app

import android.content.Context
import com.project.speakmate.R
import com.project.speakmate.domain.model.Scenario

object DebugCallScenario {
    private const val SCENARIO_ID = "debug_coffee_shop"
    private const val DESCRIPTION = "A customer is ordering a drink at a coffee shop."
    private const val PROMPT =
        "You are the barista and the learner is the customer. Help them order a drink, choose a size, and confirm the order. " +
            "Speak only in English. Stay in character, keep replies brief, and ask one natural follow-up at a time. " +
            "Remember the customer's choices and refer to them in later turns."

    fun getScenario(context: Context): Scenario = Scenario(
        scenarioId = SCENARIO_ID,
        name = context.getString(R.string.call_debug_scenario_name),
        description = DESCRIPTION,
        promptTemplate = PROMPT
    )
}
