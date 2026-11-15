package com.project.speakmate.domain.model

sealed interface ScenarioSelection {
    data class Original(val scenario: Scenario) : ScenarioSelection

    data class Variant(val variant: ScenarioVariant) : ScenarioSelection
}
