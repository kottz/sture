package dev.minlauncher.util

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import dev.minlauncher.domain.model.ScreenTimeStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

@RequiresApi(Build.VERSION_CODES.Q)
class ScreenTimeHelper(private val context: Context) {
    
    private val usageStatsManager by lazy {
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    }
    
    fun hasPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            "android:get_usage_stats",
            android.os.Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }
    
    suspend fun getTodayScreenTime(): ScreenTimeStats = withContext(Dispatchers.IO) {
        if (!hasPermission()) {
            return@withContext ScreenTimeStats(0, System.currentTimeMillis())
        }
        
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()
        
        val totalTime = calculateForegroundTime(startTime, endTime)
        ScreenTimeStats(totalTime, endTime)
    }
    
    private fun calculateForegroundTime(startTime: Long, endTime: Long): Long {
        val events = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()
        
        val foregroundStartTimes = mutableMapOf<String, Long>()
        var totalTime = 0L
        
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            
            // Skip our own package
            if (event.packageName == context.packageName) continue
            
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    foregroundStartTimes[event.packageName] = event.timeStamp
                }
                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    foregroundStartTimes[event.packageName]?.let { start ->
                        totalTime += event.timeStamp - start
                        foregroundStartTimes.remove(event.packageName)
                    }
                }
            }
        }
        
        // Add time for apps still in foreground
        foregroundStartTimes.values.forEach { start ->
            totalTime += endTime - start
        }
        
        return totalTime
    }
}
