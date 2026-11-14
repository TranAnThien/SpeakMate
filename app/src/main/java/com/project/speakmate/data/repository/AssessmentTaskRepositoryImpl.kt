package com.project.speakmate.data.repository

import com.project.speakmate.data.remote.firebase.FirestoreAssessmentTaskDataSource
import com.project.speakmate.domain.model.AssessmentTask
import com.project.speakmate.domain.repository.AssessmentTaskRepository
import javax.inject.Inject

class AssessmentTaskRepositoryImpl @Inject constructor(
    private val remoteDataSource: FirestoreAssessmentTaskDataSource
) : AssessmentTaskRepository {
    override suspend fun getAssessmentTasks(): List<AssessmentTask> = remoteDataSource.getAssessmentTasks().mapNotNull { it.toDomain() }
}
