package com.project.speakmate.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.project.speakmate.data.remote.firebase.FirebaseAuthDataSource
import com.project.speakmate.domain.model.LearningExample
import com.project.speakmate.domain.model.VocabularyItem
import com.project.speakmate.domain.model.VocabularySaveResult
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test



class SavedVocabularyRepositoryImplTest {

    private fun makeItem(
        word: String,
        mean: String = "nghĩa",
        pos: String = "noun"
    ) = VocabularyItem(
        word = word,
        mean = mean,
        phonetic = "",
        partOfSpeech = pos,
        example = LearningExample("", "")
    )



    @Test
    fun saveVocabularyReturnsFailureWhenWordIsBlank() = runTest {
        val auth = mockk<FirebaseAuth>()
        val firestore = mockk<FirebaseFirestore>()
        val user = mockk<FirebaseUser>()
        every { auth.currentUser } returns user
        every { user.uid } returns "uid123"

        val repo = SavedVocabularyRepositoryImpl(firestore, FirebaseAuthDataSource(auth, mockk()))
        val result = repo.saveVocabulary(makeItem(word = "   ", mean = "meaning"))

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("empty") == true)
    }

    @Test
    fun saveVocabularyReturnsFailureWhenMeanIsBlank() = runTest {
        val auth = mockk<FirebaseAuth>()
        val firestore = mockk<FirebaseFirestore>()
        val user = mockk<FirebaseUser>()
        every { auth.currentUser } returns user
        every { user.uid } returns "uid123"

        val repo = SavedVocabularyRepositoryImpl(firestore, FirebaseAuthDataSource(auth, mockk()))
        val result = repo.saveVocabulary(makeItem(word = "run", mean = "   "))

        assertTrue(result.isFailure)
    }

    @Test
    fun saveVocabularyReturnsFailureWhenUserIsNotLoggedIn() = runTest {
        val auth = mockk<FirebaseAuth>()
        val firestore = mockk<FirebaseFirestore>()
        every { auth.currentUser } returns null

        val repo = SavedVocabularyRepositoryImpl(firestore, FirebaseAuthDataSource(auth, mockk()))
        val result = repo.saveVocabulary(makeItem("hello", "xin chào"))

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("not logged in") == true)
    }



    @Test
    fun removeVocabularyReturnsFailureWhenUserIsNotLoggedIn() = runTest {
        val auth = mockk<FirebaseAuth>()
        val firestore = mockk<FirebaseFirestore>()
        every { auth.currentUser } returns null

        val repo = SavedVocabularyRepositoryImpl(firestore, FirebaseAuthDataSource(auth, mockk()))
        val result = repo.removeVocabulary("hello", "noun")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("not logged in") == true)
    }





    @Test
    fun normalizedWordIsComparedCaseInsensitivelyForDuplicateDetection() = runTest {
        val fakeRepo = FakeSavedVocabularyRepository()


        val r1 = fakeRepo.saveVocabulary(makeItem("RESILIENCE", "kiên cường", "noun"))
        assertTrue(r1.isSuccess)
        assertEquals(VocabularySaveResult.SAVED, r1.getOrThrow())


        val r2 = fakeRepo.saveVocabulary(makeItem("resilience", "kiên cường", "noun"))
        assertTrue(r2.isSuccess)
        assertEquals(VocabularySaveResult.MOVED_TO_TOP, r2.getOrThrow())
    }



    @Test
    fun saveVocabularyReturnsSavedReplacingOldestWhenListExceeds1000() = runTest {
        val fakeRepo = FakeSavedVocabularyRepository()


        repeat(1000) { i ->
            fakeRepo.saveVocabulary(makeItem("word$i", "nghĩa $i", "noun"))
        }


        val result = fakeRepo.saveVocabulary(makeItem("newword", "nghĩa mới", "noun"))
        assertEquals(VocabularySaveResult.SAVED_REPLACING_OLDEST, result.getOrThrow())
    }



    @Test
    fun removeVocabularyRemovesCorrectItemByWordAndPartOfSpeech() = runTest {
        val fakeRepo = FakeSavedVocabularyRepository()
        fakeRepo.saveVocabulary(makeItem("run", "chạy", "verb"))
        fakeRepo.saveVocabulary(makeItem("run", "lần chạy", "noun"))

        val result = fakeRepo.removeVocabulary("run", "verb")
        assertTrue(result.isSuccess)

        val remaining = mutableListOf<VocabularyItem>()
        fakeRepo.observeSavedVocabularies().collect { remaining.addAll(it) }
        assertTrue(remaining.none { it.word == "run" && it.partOfSpeech == "verb" })
        assertTrue(remaining.any { it.word == "run" && it.partOfSpeech == "noun" })
    }
}
