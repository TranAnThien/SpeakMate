package com.project.speakmate.data.repository

import com.project.speakmate.data.dto.ScenarioDto
import com.project.speakmate.domain.model.Scenario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScenarioDtoTest {
    private val scenario = ScenarioDto("cafe-a2", "Order coffee", "A conversation in a cafe", "Play the barista")

    @Test
    fun mapsConfiguredFieldsWithoutChangingStoredValues() {
        assertEquals(Scenario("cafe-a2", "Order coffee", "A conversation in a cafe", "Play the barista"), scenario.toDomain())
    }

    @Test
    fun defaultsDoNotCreateAUsableScenario() {
        assertNull(ScenarioDto().toDomain())
    }

    @Test
    fun blankIdDoesNotCreateAUsableScenario() {
        assertNull(scenario.copy(scenarioId = " ").toDomain())
    }

    @Test
    fun blankNameDoesNotCreateAUsableScenario() {
        assertNull(scenario.copy(name = " ").toDomain())
    }

    @Test
    fun blankDescriptionDoesNotCreateAUsableScenario() {
        assertNull(scenario.copy(description = " ").toDomain())
    }

    @Test
    fun blankPromptDoesNotCreateAUsableScenario() {
        assertNull(scenario.copy(promptTemplate = "\t").toDomain())
    }

    @Test
    fun missingScenarioIdFieldFallsBackToDocumentId() {
        assertEquals("doc-cafe", scenario.copy(scenarioId = "").withDocumentId("doc-cafe").toDomain()?.scenarioId)
    }

    @Test
    fun storedScenarioIdFieldWinsOverDocumentId() {
        assertEquals("cafe-a2", scenario.withDocumentId("doc-cafe").toDomain()?.scenarioId)
    }
}
