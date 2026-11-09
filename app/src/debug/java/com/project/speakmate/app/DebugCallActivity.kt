package com.project.speakmate.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.project.speakmate.domain.model.CallActivityType
import com.project.speakmate.ui.call.CallRoute
import com.project.speakmate.ui.theme.SpeakMateTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DebugCallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val scenario = DebugCallScenario.getScenario(this)
        setContent {
            SpeakMateTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    CallRoute(scenario, CallActivityType.FREE_PRACTICE, onExit = ::finish, modifier = Modifier.padding(padding))
                }
            }
        }
    }
}
