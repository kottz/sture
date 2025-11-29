package dev.minlauncher.ui

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.minlauncher.MinLauncherApp
import dev.minlauncher.data.local.SettingsDataStore
import dev.minlauncher.data.repository.AppRepository
import dev.minlauncher.domain.model.*
import dev.minlauncher.util.ScreenTimeHelper
import dev.minlauncher.util.isDefaultLauncher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val app = application as MinLauncherApp
    private val repository = AppRepository(application)
    val settingsDataStore: SettingsDataStore = app.settingsDataStore
    
    private val screenTimeHelper = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ScreenTimeHelper(application)
    } else null
    
    // Settings flow
    val settings: StateFlow<LauncherSettings> = settingsDataStore.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, LauncherSettings())
    
    val isFirstLaunch: Flow<Boolean> = settingsDataStore.isFirstLaunch
    
    // Home apps
    val homeApps: StateFlow<List<HomeApp>> = repository.getHomeAppsFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    
    // Gesture apps
    val swipeLeftApp: StateFlow<GestureApp?> = repository.getGestureAppFlow(Gesture.SWIPE_LEFT)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    
    val swipeRightApp: StateFlow<GestureApp?> = repository.getGestureAppFlow(Gesture.SWIPE_RIGHT)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    
    // Screen time
    private val _screenTime = MutableStateFlow<ScreenTimeStats?>(null)
    val screenTime: StateFlow<ScreenTimeStats?> = _screenTime.asStateFlow()
    
    // App list (cached)
    private val _allApps = MutableStateFlow<List<App>>(emptyList())
    val allApps: StateFlow<List<App>> = _allApps.asStateFlow()
    
    // Hidden apps
    val hiddenApps: Flow<List<App>> = repository.getHiddenAppsFlow()
    
    // Launcher state
    private val _isDefaultLauncher = MutableStateFlow(false)
    val isDefaultLauncher: StateFlow<Boolean> = _isDefaultLauncher.asStateFlow()
    
    init {
        refreshAppList()
    }
    
    fun refreshAppList() {
        viewModelScope.launch {
            _allApps.value = repository.getInstalledApps()
        }
    }
    
    fun checkDefaultLauncher() {
        _isDefaultLauncher.value = getApplication<Application>().isDefaultLauncher()
    }
    
    fun refreshScreenTime() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && screenTimeHelper?.hasPermission() == true) {
            viewModelScope.launch {
                _screenTime.value = screenTimeHelper.getTodayScreenTime()
            }
        }
    }
    
    fun setFirstLaunchComplete() {
        viewModelScope.launch {
            settingsDataStore.setFirstLaunchComplete()
        }
    }
    
    // Home app management
    
    fun setHomeApp(position: Int, app: App) {
        viewModelScope.launch {
            repository.setHomeApp(position, app)
        }
    }
    
    fun setHomeAppLabel(position: Int, label: String) {
        viewModelScope.launch {
            val currentApps = homeApps.value
            val homeApp = currentApps.find { it.position == position }
            if (homeApp != null) {
                repository.setHomeApp(
                    position = position,
                    app = null, // Keep existing app
                    customLabel = label
                )
                // Re-upsert with the label
                repository.setHomeApp(
                    position,
                    app = _allApps.value.find { 
                        it.packageName == homeApp.packageName && 
                        it.userHandle.toString() == homeApp.userHandle 
                    },
                    customLabel = label
                )
            }
        }
    }
    
    fun clearHomeApp(position: Int) {
        viewModelScope.launch {
            repository.clearHomeApp(position)
        }
    }
    
    // Gesture app management
    
    fun setGestureApp(gesture: Gesture, app: App) {
        viewModelScope.launch {
            repository.setGestureApp(gesture, app)
        }
    }
    
    fun toggleGestureEnabled(gesture: Gesture, enabled: Boolean) {
        viewModelScope.launch {
            repository.setGestureEnabled(gesture, enabled)
        }
    }
    
    // App management
    
    fun setAppHidden(app: App, hidden: Boolean) {
        viewModelScope.launch {
            repository.setAppHidden(app, hidden)
            refreshAppList()
        }
    }
    
    fun setAppLabel(app: App, label: String?) {
        viewModelScope.launch {
            repository.setAppCustomLabel(app, label)
            refreshAppList()
        }
    }
    
    // Settings updates
    
    fun updateHomeAppCount(count: Int) {
        viewModelScope.launch {
            settingsDataStore.updateHomeAppCount(count)
            repository.trimHomeApps(count)
        }
    }
    
    fun updateHomeAlignment(gravity: Int) {
        viewModelScope.launch {
            settingsDataStore.updateHomeAlignment(gravity)
        }
    }
    
    fun updateHomeBottomAligned(aligned: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateHomeBottomAligned(aligned)
        }
    }
    
    fun updateDateTimeVisibility(visibility: DateTimeVisibility) {
        viewModelScope.launch {
            settingsDataStore.updateDateTimeVisibility(visibility)
        }
    }
    
    fun updateShowScreenTime(show: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateShowScreenTime(show)
            if (show) refreshScreenTime()
        }
    }
    
    fun updateShowStatusBar(show: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateShowStatusBar(show)
        }
    }
    
    fun updateTheme(theme: AppTheme) {
        viewModelScope.launch {
            settingsDataStore.updateTheme(theme)
        }
    }
    
    fun updateTextScale(scale: TextScale) {
        viewModelScope.launch {
            settingsDataStore.updateTextScale(scale)
        }
    }
    
    fun updateSwipeDownAction(action: SwipeDownAction) {
        viewModelScope.launch {
            settingsDataStore.updateSwipeDownAction(action)
        }
    }
    
    fun updateDoubleTapToLock(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateDoubleTapToLock(enabled)
        }
    }
    
    fun updateAutoShowKeyboard(show: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateAutoShowKeyboard(show)
        }
    }
    
    fun updateAppLabelAlignment(gravity: Int) {
        viewModelScope.launch {
            settingsDataStore.updateAppLabelAlignment(gravity)
        }
    }
    
    fun updateClockApp(app: App) {
        viewModelScope.launch {
            settingsDataStore.updateClockApp(
                app.packageName,
                app.activityName ?: "",
                app.userHandle.toString()
            )
        }
    }
    
    fun updateCalendarApp(app: App) {
        viewModelScope.launch {
            settingsDataStore.updateCalendarApp(
                app.packageName,
                app.activityName ?: "",
                app.userHandle.toString()
            )
        }
    }
    
    // Utilities
    
    fun getUserHandle(userString: String?) = repository.getUserHandle(userString)
    
    fun isPackageInstalled(packageName: String, userString: String?): Boolean {
        return repository.isPackageInstalled(packageName, getUserHandle(userString))
    }
}
