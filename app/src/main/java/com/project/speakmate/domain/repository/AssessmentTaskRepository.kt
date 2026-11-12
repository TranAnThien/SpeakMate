package com.project.speakmate.domain.repository

import com.project.speakmate.domain.model.AssessmentTask

interface AssessmentTaskRepository {
    suspend fun getAssessmentTasks(): List<AssessmentTask>
}
