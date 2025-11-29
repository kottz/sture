package dev.minlauncher

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.work.Configuration
import dev.minlauncher.data.local.SettingsDataStore
import dev.minlauncher.domain.model.AppTheme
import dev.minlauncher.util.isEinkDisplay
import dev.minlauncher.util.toNightMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MinLauncherApp : Application(), Configuration.Provider {
    
    lateinit var settingsDataStore: SettingsDataStore
        private set
    
    override fun onCreate() {
        super.onCreate()
        
        settingsDataStore = SettingsDataStore(this)
        
        // Apply theme on startup
        val theme = runBlocking {
            if (isEinkDisplay()) {
                AppTheme.LIGHT
            } else {
                settingsDataStore.settings.first().theme
            }
        }
        AppCompatDelegate.setDefaultNightMode(theme.toNightMode())
    }
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
