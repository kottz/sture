package dev.minlauncher.util

import android.annotation.SuppressLint
import android.app.Activity
import android.app.SearchManager
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.UserHandle
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.MediaStore
import android.provider.Settings
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatDelegate
import dev.minlauncher.BuildConfig
import dev.minlauncher.R
import dev.minlauncher.domain.model.AppTheme
import dev.minlauncher.ui.FakeHomeActivity

// Context extensions

fun Context.showToast(message: String, long: Boolean = false) {
    Toast.makeText(this, message, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
}

fun Context.showToast(resId: Int, long: Boolean = false) {
    showToast(getString(resId), long)
}

val Context.isDarkTheme: Boolean
    get() = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

fun Context.isDefaultLauncher(): Boolean {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
    return resolveInfo?.activityInfo?.packageName == BuildConfig.APPLICATION_ID
}

fun Context.getDefaultLauncherPackage(): String {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    val resolveInfo = packageManager.resolveActivity(intent, 0)
    return resolveInfo?.activityInfo?.packageName ?: "android"
}

@ColorInt
fun Context.getColorFromAttr(@AttrRes attrColor: Int): Int {
    val typedValue = TypedValue()
    theme.resolveAttribute(attrColor, typedValue, true)
    return typedValue.data
}

fun Context.dpToPx(dp: Int): Int {
    return (dp * resources.displayMetrics.density).toInt()
}

// View extensions

fun View.hideKeyboard() {
    clearFocus()
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
}

fun View.showKeyboard() {
    if (requestFocus()) {
        postDelayed({
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
        }, 100)
    }
}

// App launching

fun Context.launchApp(packageName: String, activityName: String?, userHandle: UserHandle) {
    val launcher = getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    val activities = launcher.getActivityList(packageName, userHandle)
    
    if (activities.isEmpty()) {
        showToast(R.string.app_not_found)
        return
    }
    
    val component = if (activityName.isNullOrBlank()) {
        ComponentName(packageName, activities.last().name)
    } else {
        ComponentName(packageName, activityName)
    }
    
    try {
        launcher.startMainActivity(component, userHandle, null, null)
    } catch (e: SecurityException) {
        // Try with current user as fallback
        try {
            launcher.startMainActivity(component, android.os.Process.myUserHandle(), null, null)
        } catch (e: Exception) {
            showToast(R.string.unable_to_launch_app)
        }
    } catch (e: Exception) {
        showToast(R.string.unable_to_launch_app)
    }
}

fun Context.openAppInfo(packageName: String, userHandle: UserHandle) {
    val launcher = getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    val intent = packageManager.getLaunchIntentForPackage(packageName)
    intent?.component?.let { component ->
        launcher.startAppDetailsActivity(component, userHandle, null, null)
    } ?: showToast(R.string.unable_to_launch_app)
}

fun Context.uninstallApp(packageName: String) {
    val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName"))
    startActivity(intent)
}

fun Context.isSystemApp(packageName: String): Boolean {
    return try {
        val appInfo = packageManager.getApplicationInfo(packageName, 0)
        appInfo.flags and (android.content.pm.ApplicationInfo.FLAG_SYSTEM or 
            android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
    } catch (e: Exception) {
        false
    }
}

// System actions

fun Context.openAlarmApp() {
    try {
        startActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS))
    } catch (e: Exception) {
        showToast(R.string.app_not_found)
    }
}

fun Context.openCalendarApp() {
    try {
        val uri = CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build()
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (e: Exception) {
        try {
            startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR))
        } catch (e: Exception) {
            showToast(R.string.app_not_found)
        }
    }
}

fun Context.openCameraApp() {
    try {
        startActivity(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
    } catch (e: Exception) {
        showToast(R.string.app_not_found)
    }
}

fun Context.openDialerApp() {
    try {
        startActivity(Intent(Intent.ACTION_DIAL))
    } catch (e: Exception) {
        showToast(R.string.app_not_found)
    }
}

fun Context.openWebSearch(query: String = "") {
    try {
        val intent = Intent(Intent.ACTION_WEB_SEARCH)
        intent.putExtra(SearchManager.QUERY, query)
        startActivity(intent)
    } catch (e: Exception) {
        showToast(R.string.app_not_found)
    }
}

@SuppressLint("WrongConstant")
fun Context.expandNotificationDrawer() {
    try {
        val service = getSystemService("statusbar")
        val clazz = Class.forName("android.app.StatusBarManager")
        val method = clazz.getMethod("expandNotificationsPanel")
        method.invoke(service)
    } catch (e: Exception) {
        // Ignore
    }
}

fun Context.openUrl(url: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: Exception) {
        showToast(R.string.app_not_found)
    }
}

// Launcher reset

fun Context.resetDefaultLauncher() {
    try {
        val componentName = ComponentName(this, FakeHomeActivity::class.java)
        packageManager.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
        startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME))
        packageManager.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun Activity.showLauncherSelector(requestCode: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = getSystemService(Context.ROLE_SERVICE) as RoleManager
        if (roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
            @Suppress("DEPRECATION")
            startActivityForResult(roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME), requestCode)
            return
        }
    }
    resetDefaultLauncher()
}

fun Context.openDefaultAppsSettings() {
    startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
}

// Theme

fun AppTheme.toNightMode(): Int = when (this) {
    AppTheme.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
    AppTheme.DARK -> AppCompatDelegate.MODE_NIGHT_YES
    AppTheme.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
}

// Display

fun Context.isTablet(): Boolean {
    val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val metrics = resources.displayMetrics
    val widthInches = metrics.widthPixels / metrics.xdpi
    val heightInches = metrics.heightPixels / metrics.ydpi
    val diagonalInches = kotlin.math.sqrt((widthInches * widthInches + heightInches * heightInches).toDouble())
    return diagonalInches >= 7.0
}

fun Context.isEinkDisplay(): Boolean {
    return try {
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        @Suppress("DEPRECATION")
        wm.defaultDisplay.refreshRate <= 10f
    } catch (e: Exception) {
        false
    }
}
