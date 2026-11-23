package com.project.speakmate.app

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.project.speakmate.domain.model.CallActivityType
import com.project.speakmate.ui.call.CallRoute
import com.project.speakmate.ui.theme.SpeakMateTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DebugCallActivity : ComponentActivity() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppLocaleProvider().wrap(base))
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        hideSpeakMateSystemBars(window)
        val scenario = DebugCallScenario.getScenario(this)
        setContent {
                SpeakMateTheme {
                    SpeakMateWindowContent {
                        CallRoute(scenario, CallActivityType.FREE_PRACTICE, onExit = ::finish)
                    }
                }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSpeakMateSystemBars(window)
    }
}
