package com.project.speakmate.utils.extension

import com.project.speakmate.utils.AppConstant
import java.util.Locale

fun Long.toDurationLabel(): String = String.format(
    Locale.ROOT, AppConstant.DURATION_FORMAT,
    this / AppConstant.SECONDS_PER_MINUTE, this % AppConstant.SECONDS_PER_MINUTE
)
