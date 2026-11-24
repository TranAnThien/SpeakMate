package com.project.speakmate.ui.components.assessment

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.project.speakmate.R
import com.project.speakmate.domain.model.PracticeSaveFailure

@Composable
fun practiceSaveFailureText(failure: PracticeSaveFailure?): String =
    stringResource(
        when (failure) {
            PracticeSaveFailure.ACCESS_DENIED -> R.string.completion_save_access_denied
            PracticeSaveFailure.CONFLICT -> R.string.completion_save_conflict
            PracticeSaveFailure.INVALID_COMPLETION -> R.string.completion_save_invalid
            PracticeSaveFailure.NETWORK,
            PracticeSaveFailure.UNKNOWN,
            null -> R.string.unit_result_save_failed
        }
    )
