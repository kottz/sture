package dev.minlauncher.data.repository

import android.content.Context
import android.content.pm.LauncherApps
import android.os.UserHandle
import android.os.UserManager
import dev.minlauncher.BuildConfig
import dev.minlauncher.data.local.*
import dev.minlauncher.data.model.*
import dev.minlauncher.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.text.Collator

class AppRepository(private val context: Context) {
    
    private val database = LauncherDatabase.getInstance(context)
    private val appMetadataDao = database.appMetadataDao()
    private val homeAppDao = database.homeAppDao()
    private val gestureAppDao = database.gestureAppDao()
    
    private val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
    private val collator = Collator.getInstance()
    
    /**
     * Get all installed apps with metadata applied.
     */
    suspend fun getInstalledApps(includeHidden: Boolean = false): List<App> = withContext(Dispatchers.IO) {
        val metadata = appMetadataDao.getAll().associateBy { it.uniqueId }
        val apps = mutableListOf<App>()
        
        for (profile in userManager.userProfiles) {
            for (activityInfo in launcherApps.getActivityList(null, profile)) {
                val packageName = activityInfo.applicationInfo.packageName
                
                // Skip ourselves
                if (packageName == BuildConfig.APPLICATION_ID) continue
                
                val uniqueId = "$packageName|$profile"
                val meta = metadata[uniqueId]
                
                // Skip hidden unless requested
                if (meta?.isHidden == true && !includeHidden) continue
                
                val installTime = activityInfo.firstInstallTime
                val isNew = System.currentTimeMillis() - installTime < 3_600_000 // 1 hour
                
                apps.add(
                    App(
                        packageName = packageName,
                        activityName = activityInfo.componentName.className,
                        userHandle = profile,
                        label = activityInfo.label.toString(),
                        customLabel = meta?.customLabel,
                        isHidden = meta?.isHidden ?: false,
                        isNew = isNew
                    )
                )
            }
        }
        
        apps.sortedWith { a, b ->
            collator.compare(a.displayLabel.lowercase(), b.displayLabel.lowercase())
        }
    }
    
    /**
     * Get only hidden apps.
     */
    fun getHiddenAppsFlow(): Flow<List<App>> {
        return appMetadataDao.getHiddenAppsFlow()
            .map { hiddenMeta ->
                val hiddenIds = hiddenMeta.map { it.uniqueId }.toSet()
                getInstalledApps(includeHidden = true).filter { it.uniqueId in hiddenIds }
            }
    }
    
    /**
     * Set app hidden state.
     */
    suspend fun setAppHidden(app: App, hidden: Boolean) {
        val existing = appMetadataDao.getById(app.uniqueId)
        appMetadataDao.upsert(
            AppMetadataEntity(
                uniqueId = app.uniqueId,
                customLabel = existing?.customLabel,
                isHidden = hidden
            )
        )
    }
    
    /**
     * Set app custom label.
     */
    suspend fun setAppCustomLabel(app: App, label: String?) {
        val existing = appMetadataDao.getById(app.uniqueId)
        appMetadataDao.upsert(
            AppMetadataEntity(
                uniqueId = app.uniqueId,
                customLabel = label?.takeIf { it.isNotBlank() },
                isHidden = existing?.isHidden ?: false
            )
        )
    }
    
    // Home apps
    
    fun getHomeAppsFlow(): Flow<List<HomeApp>> {
        return homeAppDao.getAllFlow().map { entities ->
            entities.map { entity ->
                HomeApp(
                    position = entity.position,
                    packageName = entity.packageName,
                    activityName = entity.activityName,
                    userHandle = entity.userHandle,
                    customLabel = entity.customLabel
                )
            }
        }
    }
    
    suspend fun setHomeApp(position: Int, app: App?, customLabel: String? = null) {
        homeAppDao.upsert(
            HomeAppEntity(
                position = position,
                packageName = app?.packageName,
                activityName = app?.activityName,
                userHandle = app?.userHandle?.toString(),
                customLabel = customLabel ?: app?.displayLabel
            )
        )
    }
    
    suspend fun clearHomeApp(position: Int) {
        homeAppDao.deleteByPosition(position)
    }
    
    suspend fun trimHomeApps(maxCount: Int) {
        homeAppDao.deleteAbovePosition(maxCount - 1)
    }
    
    // Gesture apps
    
    fun getGestureAppFlow(gesture: Gesture): Flow<GestureApp?> {
        return gestureAppDao.getByGestureFlow(gesture.name).map { entity ->
            entity?.let {
                GestureApp(
                    gesture = gesture,
                    packageName = it.packageName,
                    activityName = it.activityName,
                    userHandle = it.userHandle,
                    label = it.label,
                    isEnabled = it.isEnabled
                )
            }
        }
    }
    
    suspend fun setGestureApp(gesture: Gesture, app: App?, enabled: Boolean = true) {
        gestureAppDao.upsert(
            GestureAppEntity(
                gesture = gesture.name,
                packageName = app?.packageName,
                activityName = app?.activityName,
                userHandle = app?.userHandle?.toString(),
                label = app?.displayLabel,
                isEnabled = enabled
            )
        )
    }
    
    suspend fun setGestureEnabled(gesture: Gesture, enabled: Boolean) {
        val existing = gestureAppDao.getByGesture(gesture.name)
        if (existing != null) {
            gestureAppDao.upsert(existing.copy(isEnabled = enabled))
        } else {
            gestureAppDao.upsert(
                GestureAppEntity(
                    gesture = gesture.name,
                    packageName = null,
                    activityName = null,
                    userHandle = null,
                    label = null,
                    isEnabled = enabled
                )
            )
        }
    }
    
    /**
     * Check if a package is installed.
     */
    fun isPackageInstalled(packageName: String, userHandle: UserHandle): Boolean {
        return launcherApps.getActivityList(packageName, userHandle).isNotEmpty()
    }
    
    /**
     * Get user handle from string representation.
     */
    fun getUserHandle(userString: String?): UserHandle {
        if (userString.isNullOrBlank()) return android.os.Process.myUserHandle()
        return userManager.userProfiles.find { it.toString() == userString }
            ?: android.os.Process.myUserHandle()
    }
}
