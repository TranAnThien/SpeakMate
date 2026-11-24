package com.project.speakmate.ui.components.assessment

import androidx.annotation.StringRes
import com.project.speakmate.R
import com.project.speakmate.domain.model.InsufficientEvidenceReason

@StringRes
fun insufficientEvidenceDescriptionRes(
    reason: InsufficientEvidenceReason?,
    @StringRes fallbackRes: Int,
    isSavedUnassessed: Boolean = false
): Int =
    when (reason) {
        InsufficientEvidenceReason.NO_USER_SPEECH ->
            if (isSavedUnassessed) fallbackRes else R.string.assessment_no_speech
        InsufficientEvidenceReason.NOT_ENOUGH_SPEECH ->
            if (isSavedUnassessed) fallbackRes else R.string.assessment_not_enough_speech
        InsufficientEvidenceReason.AUDIO_UNAVAILABLE ->
            if (isSavedUnassessed) R.string.assessment_audio_unavailable_reason else R.string.assessment_audio_missing
        InsufficientEvidenceReason.TRANSCRIPT_UNAVAILABLE ->
            if (isSavedUnassessed) fallbackRes else R.string.assessment_transcript_missing
        InsufficientEvidenceReason.CALL_FAILED ->
            if (isSavedUnassessed) fallbackRes else R.string.assessment_call_failed
        InsufficientEvidenceReason.EVALUATION_FAILED,
        InsufficientEvidenceReason.EVALUATION_INVALID ->
            if (isSavedUnassessed) R.string.assessment_saved_unassessed_reason else fallbackRes
        null -> fallbackRes
    }
