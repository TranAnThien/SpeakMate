package com.project.speakmate.ui.placement

import com.project.speakmate.domain.repository.AssessmentTaskRepository
import com.project.speakmate.domain.rules.ReassessmentTestFixtures

class FakeAssessmentTaskRepository : AssessmentTaskRepository {
    var tasks = ReassessmentTestFixtures.getTasks()
    var failure: Exception? = null

    override suspend fun getAssessmentTasks() = tasks.also {
        failure?.let { exception -> throw exception }
    }
}
