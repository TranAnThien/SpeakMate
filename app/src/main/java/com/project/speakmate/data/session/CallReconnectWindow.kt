package com.project.speakmate.data.session

import com.project.speakmate.utils.extension.default

class CallReconnectWindow {
    fun getRemainingMillis(disconnectedAtMillis: Long?, nowMillis: Long, windowMillis: Long): Long {
        val disconnectedAt = disconnectedAtMillis.default(nowMillis)
        return windowMillis - (nowMillis - disconnectedAt)
    }
}
