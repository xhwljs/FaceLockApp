package com.insightface.recognizer

import android.app.Application
import com.insightface.recognizer.data.FaceManager
import com.insightface.recognizer.ui.theme.ThemeManager
import com.insightface.recognizer.update.AppUpdateManager

/**
 * Process-wide singletons:
 *  - [faceManager] owns the InspireFace SDK + FeatureHub
 *  - [updateManager] checks GitHub Releases on startup and downloads/installs APK updates
 *  - [themeManager] persists the active light theme
 */
class App : Application() {

    lateinit var faceManager: FaceManager
        private set
    lateinit var updateManager: AppUpdateManager
        private set
    lateinit var themeManager: ThemeManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        themeManager = ThemeManager(this)
        faceManager = FaceManager(this)
        updateManager = AppUpdateManager(this)
        // Launch the SDK + check for updates immediately on cold start.
        faceManager.launch()
        updateManager.checkOnStartup()
    }

    companion object {
        @Volatile private var instance: App? = null
        fun get(): App = instance!!
    }
}
