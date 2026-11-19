package com.project.speakmate.utils.extension

import com.project.speakmate.utils.AppConstant
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Date.toDateLabel(): String = SimpleDateFormat(AppConstant.DISPLAY_DATE_PATTERN, Locale.getDefault()).format(this)

fun Date.toTimeLabel(): String = SimpleDateFormat(AppConstant.DISPLAY_TIME_PATTERN, Locale.getDefault()).format(this)

fun Date.toDateTimeLabel(): String = SimpleDateFormat(AppConstant.DISPLAY_DATE_TIME_PATTERN, Locale.getDefault()).format(this)
