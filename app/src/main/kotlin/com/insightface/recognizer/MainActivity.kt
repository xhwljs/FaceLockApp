package com.insightface.recognizer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.insightface.recognizer.ui.nav.AppNav
import com.insightface.recognizer.ui.theme.InsightFaceApp
import com.insightface.recognizer.ui.theme.ProvideThemeManager
import com.insightface.recognizer.ui.components.UpdateDialog

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = App.get()
        setContent {
            ProvideThemeManager(app.themeManager) {
                InsightFaceApp {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        AppNav()
                    }
                    // Startup update check result → dialog.
                    val updateState by app.updateManager.state.collectAsState()
                    UpdateDialog(state = updateState)
                }
            }
        }
    }
}
