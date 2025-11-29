package dev.minlauncher.domain.model

import android.os.UserHandle

/**
 * Represents an installed application.
 */
data class App(
    val packageName: String,
    val activityName: String?,
    val userHandle: UserHandle,
    val label: String,
    val customLabel: String? = null,
    val isHidden: Boolean = false,
    val isNew: Boolean = false
) {
    val displayLabel: String
        get() = customLabel?.takeIf { it.isNotBlank() } ?: label
    
    val uniqueId: String
        get() = "$packageName|$userHandle"
}

/**
 * Represents a home screen app slot.
 */
data class HomeApp(
    val position: Int,
    val packageName: String?,
    val activityName: String?,
    val userHandle: String?,
    val customLabel: String?
) {
    val isEmpty: Boolean
        get() = packageName.isNullOrBlank()
}

/**
 * Represents a gesture-bound app (swipe left/right).
 */
data class GestureApp(
    val gesture: Gesture,
    val packageName: String?,
    val activityName: String?,
    val userHandle: String?,
    val label: String?,
    val isEnabled: Boolean = true
)

enum class Gesture {
    SWIPE_LEFT,
    SWIPE_RIGHT,
    DOUBLE_TAP
}

/**
 * Represents screen time stats for today.
 */
data class ScreenTimeStats(
    val totalMillis: Long,
    val lastUpdated: Long
) {
    val formatted: String
        get() {
            val minutes = totalMillis / 60_000
            val hours = minutes / 60
            val remainingMinutes = minutes % 60
            return when {
                hours > 0 -> "${hours}h ${remainingMinutes}m"
                minutes > 0 -> "${minutes}m"
                else -> "<1m"
            }
        }
}
