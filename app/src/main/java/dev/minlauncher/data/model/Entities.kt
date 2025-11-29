package dev.minlauncher.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores custom labels and hidden state for apps.
 */
@Entity(tableName = "app_metadata")
data class AppMetadataEntity(
    @PrimaryKey
    val uniqueId: String, // packageName|userHandle
    val customLabel: String? = null,
    val isHidden: Boolean = false
)

/**
 * Stores home screen app configuration.
 * Supports any number of home apps dynamically.
 */
@Entity(tableName = "home_apps")
data class HomeAppEntity(
    @PrimaryKey
    val position: Int,
    val packageName: String?,
    val activityName: String?,
    val userHandle: String?,
    val customLabel: String?
)

/**
 * Stores gesture app bindings.
 */
@Entity(tableName = "gesture_apps")
data class GestureAppEntity(
    @PrimaryKey
    val gesture: String, // SWIPE_LEFT, SWIPE_RIGHT
    val packageName: String?,
    val activityName: String?,
    val userHandle: String?,
    val label: String?,
    val isEnabled: Boolean = true
)
