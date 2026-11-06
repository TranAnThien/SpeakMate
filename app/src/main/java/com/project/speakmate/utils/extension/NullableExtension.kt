package com.project.speakmate.utils.extension

fun <T> T?.default(fallback: T): T = this ?: fallback
