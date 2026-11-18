package com.project.speakmate.domain.rules

import com.project.speakmate.domain.model.CallSessionState
import com.project.speakmate.utils.extension.default

object CallTimingRules {
    fun getDurationMillis(session: CallSessionState, nowMillis: Long): Long? {
        val startedAt = session.startedAtMillis ?: return null
        return (session.endedAtMillis.default(nowMillis) - startedAt).coerceAtLeast(0L)
    }
}
