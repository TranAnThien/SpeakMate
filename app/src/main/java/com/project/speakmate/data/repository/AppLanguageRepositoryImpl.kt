package com.project.speakmate.data.repository

import com.project.speakmate.data.local.AppLanguageLocalDataSource
import com.project.speakmate.domain.model.AppLanguage
import com.project.speakmate.domain.repository.AppLanguageRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
class AppLanguageRepositoryImpl @Inject constructor(
    private val localDataSource: AppLanguageLocalDataSource
) : AppLanguageRepository {
    private val language = MutableStateFlow<AppLanguage?>(null)
    private val mutex = Mutex()

    override fun observeLanguage(): Flow<AppLanguage> = flow {
        mutex.withLock {
            if (language.value == null) language.value = localDataSource.getLanguage()
        }
        emitAll(language.filterNotNull())
    }

    override suspend fun saveLanguage(language: AppLanguage): Result<Unit> {
        return try {
            mutex.withLock {
                currentCoroutineContext().ensureActive()
                withContext(NonCancellable) {
                    localDataSource.saveLanguage(language)
                    this@AppLanguageRepositoryImpl.language.value = language
                }
                currentCoroutineContext().ensureActive()
            }
            Result.success(Unit)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }
}
