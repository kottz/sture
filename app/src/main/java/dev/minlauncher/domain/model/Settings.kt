package dev.minlauncher.domain.model

import android.view.Gravity

/**
 * All launcher settings in one place.
 */
data class LauncherSettings(
    // Home screen
    val homeAppCount: Int = 4,
    val homeAlignment: Int = Gravity.START,
    val homeBottomAligned: Boolean = false,
    val showDateTime: DateTimeVisibility = DateTimeVisibility.ON,
    val showScreenTime: Boolean = false,
    val showStatusBar: Boolean = false,
    
    // Appearance
    val theme: AppTheme = AppTheme.DARK,
    val textScale: TextScale = TextScale.NORMAL,
    
    // Gestures
    val swipeLeftEnabled: Boolean = true,
    val swipeRightEnabled: Boolean = true,
    val swipeDownAction: SwipeDownAction = SwipeDownAction.NOTIFICATIONS,
    val doubleTapToLock: Boolean = false,
    
    // Drawer
    val autoShowKeyboard: Boolean = true,
    val appLabelAlignment: Int = Gravity.START,
    
    // Wallpaper
    val dailyWallpaper: Boolean = false,
    val dailyWallpaperUrl: String = "",
    
    // Clock/Calendar apps
    val clockAppPackage: String = "",
    val clockAppActivity: String = "",
    val clockAppUser: String = "",
    val calendarAppPackage: String = "",
    val calendarAppActivity: String = "",
    val calendarAppUser: String = ""
)

enum class DateTimeVisibility {
    OFF,
    ON,
    DATE_ONLY
}

enum class AppTheme {
    LIGHT,
    DARK,
    SYSTEM
}

enum class TextScale(val value: Float) {
    TINY(0.7f),
    SMALL(0.85f),
    NORMAL(1.0f),
    LARGE(1.15f),
    XLARGE(1.3f);
    
    companion object {
        fun fromValue(value: Float): TextScale {
            return entries.minByOrNull { kotlin.math.abs(it.value - value) } ?: NORMAL
        }
    }
}

enum class SwipeDownAction {
    NOTIFICATIONS,
    SEARCH
}
