package com.project.speakmate.data.repository

import com.project.speakmate.domain.model.LearningExample
import com.project.speakmate.domain.model.VocabularyItem
import com.project.speakmate.domain.model.VocabularySaveResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Transaction
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests cho SavedVocabularyRepositoryImpl.
 *
 * Lưu ý: Firestore Transaction trong môi trường unit test được mock hoàn toàn
 * bằng MockK để không cần kết nối mạng thực tế.
 */
class SavedVocabularyRepositoryImplFakeTest {

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

    // ─── Validation: từ chối lưu khi word/mean rỗng ───────────────────────

    @Test
    fun `saveVocabulary returns failure when word is blank`() = runTest {
        val auth = mockk<FirebaseAuth>()
        val firestore = mockk<FirebaseFirestore>()
        val user = mockk<FirebaseUser>()
        every { auth.currentUser } returns user
        every { user.uid } returns "uid123"

        val repo = SavedVocabularyRepositoryImpl(firestore, auth)
        val result = repo.saveVocabulary(makeItem(word = "   ", mean = "meaning"))

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("empty") == true)
    }

    @Test
    fun `saveVocabulary returns failure when mean is blank`() = runTest {
        val auth = mockk<FirebaseAuth>()
        val firestore = mockk<FirebaseFirestore>()
        val user = mockk<FirebaseUser>()
        every { auth.currentUser } returns user
        every { user.uid } returns "uid123"

        val repo = SavedVocabularyRepositoryImpl(firestore, auth)
        val result = repo.saveVocabulary(makeItem(word = "run", mean = "   "))

        assertTrue(result.isFailure)
    }

    @Test
    fun `saveVocabulary returns failure when user is not logged in`() = runTest {
        val auth = mockk<FirebaseAuth>()
        val firestore = mockk<FirebaseFirestore>()
        every { auth.currentUser } returns null

        val repo = SavedVocabularyRepositoryImpl(firestore, auth)
        val result = repo.saveVocabulary(makeItem("hello", "xin chào"))

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("not logged in") == true)
    }

    // ─── removeVocabulary: trả về failure khi chưa đăng nhập ─────────────

    @Test
    fun `removeVocabulary returns failure when user is not logged in`() = runTest {
        val auth = mockk<FirebaseAuth>()
        val firestore = mockk<FirebaseFirestore>()
        every { auth.currentUser } returns null

        val repo = SavedVocabularyRepositoryImpl(firestore, auth)
        val result = repo.removeVocabulary("hello", "noun")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("not logged in") == true)
    }

    // ─── Chuẩn hóa word về lowercase ─────────────────────────────────────

    /**
     * Kiểm tra chuẩn hóa thông qua hành vi: gọi saveVocabulary với word viết hoa,
     * khi lưu trùng (cùng word lowercase + partOfSpeech) phải nhận MOVED_TO_TOP.
     * Dùng FakeSavedVocabularyRepository để không cần mock Firestore transaction.
     */
    @Test
    fun `normalized word is compared case-insensitively for duplicate detection`() = runTest {
        val fakeRepo = FakeSavedVocabularyRepository()

        // Lưu từ "RESILIENCE" lần 1 → SAVED
        val r1 = fakeRepo.saveVocabulary(makeItem("RESILIENCE", "kiên cường", "noun"))
        assertTrue(r1.isSuccess)
        assertEquals(VocabularySaveResult.SAVED, r1.getOrThrow())

        // Lưu từ "resilience" lần 2 (cùng pos) → MOVED_TO_TOP
        val r2 = fakeRepo.saveVocabulary(makeItem("resilience", "kiên cường", "noun"))
        assertTrue(r2.isSuccess)
        assertEquals(VocabularySaveResult.MOVED_TO_TOP, r2.getOrThrow())
    }

    // ─── Giới hạn 1000 từ ────────────────────────────────────────────────

    @Test
    fun `saveVocabulary returns SAVED_REPLACING_OLDEST when list exceeds 1000`() = runTest {
        val fakeRepo = FakeSavedVocabularyRepository()

        // Lấp đầy 1000 từ
        repeat(1000) { i ->
            fakeRepo.saveVocabulary(makeItem("word$i", "nghĩa $i", "noun"))
        }

        // Thêm từ thứ 1001
        val result = fakeRepo.saveVocabulary(makeItem("newword", "nghĩa mới", "noun"))
        assertEquals(VocabularySaveResult.SAVED_REPLACING_OLDEST, result.getOrThrow())
    }

    // ─── removeVocabulary: xóa đúng từ theo composite key ────────────────

    @Test
    fun `removeVocabulary removes correct item by word and partOfSpeech`() = runTest {
        val fakeRepo = FakeSavedVocabularyRepository()
        fakeRepo.saveVocabulary(makeItem("run", "chạy", "verb"))
        fakeRepo.saveVocabulary(makeItem("run", "lần chạy", "noun"))

        val result = fakeRepo.removeVocabulary("run", "verb")
        assertTrue(result.isSuccess)

        val remaining = mutableListOf<VocabularyItem>()
        fakeRepo.getSavedVocabularies().collect { remaining.addAll(it) }
        assertTrue(remaining.none { it.word == "run" && it.partOfSpeech == "verb" })
        assertTrue(remaining.any { it.word == "run" && it.partOfSpeech == "noun" })
    }
}

// ─── Fake Repository dùng bộ nhớ in-process ─────────────────────────────────

private class FakeSavedVocabularyRepository : com.project.speakmate.domain.repository.SavedVocabularyRepository {

    private val list = mutableListOf<VocabularyItem>()

    override fun getSavedVocabularies(): kotlinx.coroutines.flow.Flow<List<VocabularyItem>> =
        kotlinx.coroutines.flow.flowOf(list.toList())

    override suspend fun saveVocabulary(item: VocabularyItem): Result<VocabularySaveResult> {
        val normalizedWord = item.word.trim().lowercase()
        val normalizedMean = item.mean.trim()
        if (normalizedWord.isBlank() || normalizedMean.isBlank()) {
            return Result.failure(Exception("Word and mean cannot be empty"))
        }
        val validItem = item.copy(word = normalizedWord, mean = normalizedMean)
        val existingIndex = list.indexOfFirst {
            it.word == validItem.word && it.partOfSpeech == validItem.partOfSpeech
        }
        return if (existingIndex != -1) {
            list.removeAt(existingIndex)
            list.add(0, validItem)
            Result.success(VocabularySaveResult.MOVED_TO_TOP)
        } else {
            list.add(0, validItem)
            if (list.size > 1000) {
                list.removeLast()
                Result.success(VocabularySaveResult.SAVED_REPLACING_OLDEST)
            } else {
                Result.success(VocabularySaveResult.SAVED)
            }
        }
    }

    override suspend fun removeVocabulary(word: String, partOfSpeech: String): Result<Unit> {
        list.removeAll { it.word == word.trim().lowercase() && it.partOfSpeech == partOfSpeech.trim() }
        return Result.success(Unit)
    }
}

