package dev.minlauncher.util

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi

class LockAccessibilityService : AccessibilityService() {
    
    companion object {
        const val LOCK_DESCRIPTION = "minlauncher_lock_trigger"
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
    
    @RequiresApi(Build.VERSION_CODES.P)
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        
        try {
            val source = event.source ?: return
            if (source.className == "android.widget.FrameLayout" &&
                source.contentDescription == LOCK_DESCRIPTION
            ) {
                performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }
    
    override fun onInterrupt() {}
}
