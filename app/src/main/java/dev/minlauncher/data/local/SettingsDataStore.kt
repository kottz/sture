package dev.minlauncher.data.local

import android.content.Context
import android.view.Gravity
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dev.minlauncher.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {
    
    private object Keys {
        val HOME_APP_COUNT = intPreferencesKey("home_app_count")
        val HOME_ALIGNMENT = intPreferencesKey("home_alignment")
        val HOME_BOTTOM_ALIGNED = booleanPreferencesKey("home_bottom_aligned")
        val SHOW_DATE_TIME = stringPreferencesKey("show_date_time")
        val SHOW_SCREEN_TIME = booleanPreferencesKey("show_screen_time")
        val SHOW_STATUS_BAR = booleanPreferencesKey("show_status_bar")
        
        val THEME = stringPreferencesKey("theme")
        val TEXT_SCALE = floatPreferencesKey("text_scale")
        
        val SWIPE_LEFT_ENABLED = booleanPreferencesKey("swipe_left_enabled")
        val SWIPE_RIGHT_ENABLED = booleanPreferencesKey("swipe_right_enabled")
        val SWIPE_DOWN_ACTION = stringPreferencesKey("swipe_down_action")
        val DOUBLE_TAP_TO_LOCK = booleanPreferencesKey("double_tap_to_lock")
        
        val AUTO_SHOW_KEYBOARD = booleanPreferencesKey("auto_show_keyboard")
        val APP_LABEL_ALIGNMENT = intPreferencesKey("app_label_alignment")
        
        val DAILY_WALLPAPER = booleanPreferencesKey("daily_wallpaper")
        val DAILY_WALLPAPER_URL = stringPreferencesKey("daily_wallpaper_url")
        
        val CLOCK_APP_PACKAGE = stringPreferencesKey("clock_app_package")
        val CLOCK_APP_ACTIVITY = stringPreferencesKey("clock_app_activity")
        val CLOCK_APP_USER = stringPreferencesKey("clock_app_user")
        val CALENDAR_APP_PACKAGE = stringPreferencesKey("calendar_app_package")
        val CALENDAR_APP_ACTIVITY = stringPreferencesKey("calendar_app_activity")
        val CALENDAR_APP_USER = stringPreferencesKey("calendar_app_user")
        
        // State tracking
        val FIRST_LAUNCH = booleanPreferencesKey("first_launch")
        val LAST_RESTART = longPreferencesKey("last_restart")
    }
    
    val settings: Flow<LauncherSettings> = context.dataStore.data.map { prefs ->
        LauncherSettings(
            homeAppCount = prefs[Keys.HOME_APP_COUNT] ?: 4,
            homeAlignment = prefs[Keys.HOME_ALIGNMENT] ?: Gravity.START,
            homeBottomAligned = prefs[Keys.HOME_BOTTOM_ALIGNED] ?: false,
            showDateTime = prefs[Keys.SHOW_DATE_TIME]?.let { 
                DateTimeVisibility.valueOf(it) 
            } ?: DateTimeVisibility.ON,
            showScreenTime = prefs[Keys.SHOW_SCREEN_TIME] ?: false,
            showStatusBar = prefs[Keys.SHOW_STATUS_BAR] ?: false,
            theme = prefs[Keys.THEME]?.let { AppTheme.valueOf(it) } ?: AppTheme.DARK,
            textScale = TextScale.fromValue(prefs[Keys.TEXT_SCALE] ?: 1.0f),
            swipeLeftEnabled = prefs[Keys.SWIPE_LEFT_ENABLED] ?: true,
            swipeRightEnabled = prefs[Keys.SWIPE_RIGHT_ENABLED] ?: true,
            swipeDownAction = prefs[Keys.SWIPE_DOWN_ACTION]?.let { 
                SwipeDownAction.valueOf(it) 
            } ?: SwipeDownAction.NOTIFICATIONS,
            doubleTapToLock = prefs[Keys.DOUBLE_TAP_TO_LOCK] ?: false,
            autoShowKeyboard = prefs[Keys.AUTO_SHOW_KEYBOARD] ?: true,
            appLabelAlignment = prefs[Keys.APP_LABEL_ALIGNMENT] ?: Gravity.START,
            dailyWallpaper = prefs[Keys.DAILY_WALLPAPER] ?: false,
            dailyWallpaperUrl = prefs[Keys.DAILY_WALLPAPER_URL] ?: "",
            clockAppPackage = prefs[Keys.CLOCK_APP_PACKAGE] ?: "",
            clockAppActivity = prefs[Keys.CLOCK_APP_ACTIVITY] ?: "",
            clockAppUser = prefs[Keys.CLOCK_APP_USER] ?: "",
            calendarAppPackage = prefs[Keys.CALENDAR_APP_PACKAGE] ?: "",
            calendarAppActivity = prefs[Keys.CALENDAR_APP_ACTIVITY] ?: "",
            calendarAppUser = prefs[Keys.CALENDAR_APP_USER] ?: ""
        )
    }
    
    val isFirstLaunch: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.FIRST_LAUNCH] ?: true
    }
    
    suspend fun setFirstLaunchComplete() {
        context.dataStore.edit { it[Keys.FIRST_LAUNCH] = false }
    }
    
    suspend fun updateHomeAppCount(count: Int) {
        context.dataStore.edit { it[Keys.HOME_APP_COUNT] = count.coerceIn(0, 12) }
    }
    
    suspend fun updateHomeAlignment(gravity: Int) {
        context.dataStore.edit { it[Keys.HOME_ALIGNMENT] = gravity }
    }
    
    suspend fun updateHomeBottomAligned(aligned: Boolean) {
        context.dataStore.edit { it[Keys.HOME_BOTTOM_ALIGNED] = aligned }
    }
    
    suspend fun updateDateTimeVisibility(visibility: DateTimeVisibility) {
        context.dataStore.edit { it[Keys.SHOW_DATE_TIME] = visibility.name }
    }
    
    suspend fun updateShowScreenTime(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_SCREEN_TIME] = show }
    }
    
    suspend fun updateShowStatusBar(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_STATUS_BAR] = show }
    }
    
    suspend fun updateTheme(theme: AppTheme) {
        context.dataStore.edit { it[Keys.THEME] = theme.name }
    }
    
    suspend fun updateTextScale(scale: TextScale) {
        context.dataStore.edit { it[Keys.TEXT_SCALE] = scale.value }
    }
    
    suspend fun updateSwipeLeftEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SWIPE_LEFT_ENABLED] = enabled }
    }
    
    suspend fun updateSwipeRightEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SWIPE_RIGHT_ENABLED] = enabled }
    }
    
    suspend fun updateSwipeDownAction(action: SwipeDownAction) {
        context.dataStore.edit { it[Keys.SWIPE_DOWN_ACTION] = action.name }
    }
    
    suspend fun updateDoubleTapToLock(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DOUBLE_TAP_TO_LOCK] = enabled }
    }
    
    suspend fun updateAutoShowKeyboard(show: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_SHOW_KEYBOARD] = show }
    }
    
    suspend fun updateAppLabelAlignment(gravity: Int) {
        context.dataStore.edit { it[Keys.APP_LABEL_ALIGNMENT] = gravity }
    }
    
    suspend fun updateDailyWallpaper(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DAILY_WALLPAPER] = enabled }
    }
    
    suspend fun updateDailyWallpaperUrl(url: String) {
        context.dataStore.edit { it[Keys.DAILY_WALLPAPER_URL] = url }
    }
    
    suspend fun updateClockApp(packageName: String, activityName: String, userHandle: String) {
        context.dataStore.edit {
            it[Keys.CLOCK_APP_PACKAGE] = packageName
            it[Keys.CLOCK_APP_ACTIVITY] = activityName
            it[Keys.CLOCK_APP_USER] = userHandle
        }
    }
    
    suspend fun updateCalendarApp(packageName: String, activityName: String, userHandle: String) {
        context.dataStore.edit {
            it[Keys.CALENDAR_APP_PACKAGE] = packageName
            it[Keys.CALENDAR_APP_ACTIVITY] = activityName
            it[Keys.CALENDAR_APP_USER] = userHandle
        }
    }
}
