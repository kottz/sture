package dev.minlauncher.util

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

/**
 * Touch listener for swipe, tap, and long press gestures.
 */
open class GestureTouchListener(context: Context) : View.OnTouchListener {
    
    private val gestureDetector = GestureDetector(context, GestureListener())
    
    override fun onTouch(view: View, event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event)
    }
    
    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        
        private val swipeThreshold = 100
        private val swipeVelocityThreshold = 100
        
        override fun onDown(e: MotionEvent): Boolean = true
        
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            onClick()
            return true
        }
        
        override fun onDoubleTap(e: MotionEvent): Boolean {
            onDoubleClick()
            return true
        }
        
        override fun onLongPress(e: MotionEvent) {
            onLongClick()
        }
        
        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (e1 == null) return false
            
            val diffX = e2.x - e1.x
            val diffY = e2.y - e1.y
            
            if (abs(diffX) > abs(diffY)) {
                if (abs(diffX) > swipeThreshold && abs(velocityX) > swipeVelocityThreshold) {
                    if (diffX > 0) onSwipeRight() else onSwipeLeft()
                    return true
                }
            } else {
                if (abs(diffY) > swipeThreshold && abs(velocityY) > swipeVelocityThreshold) {
                    if (diffY > 0) onSwipeDown() else onSwipeUp()
                    return true
                }
            }
            return false
        }
    }
    
    open fun onClick() {}
    open fun onDoubleClick() {}
    open fun onLongClick() {}
    open fun onSwipeLeft() {}
    open fun onSwipeRight() {}
    open fun onSwipeUp() {}
    open fun onSwipeDown() {}
}

/**
 * Touch listener for view-specific gestures that also handles pressed state.
 */
open class ViewGestureTouchListener(
    context: Context,
    private val view: View
) : View.OnTouchListener {
    
    private val gestureDetector = GestureDetector(context, GestureListener())
    
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> view.isPressed = true
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> view.isPressed = false
        }
        return gestureDetector.onTouchEvent(event)
    }
    
    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        
        private val swipeThreshold = 100
        private val swipeVelocityThreshold = 100
        
        override fun onDown(e: MotionEvent): Boolean = true
        
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            onClick(view)
            return true
        }
        
        override fun onLongPress(e: MotionEvent) {
            onLongClick(view)
        }
        
        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (e1 == null) return false
            
            val diffX = e2.x - e1.x
            val diffY = e2.y - e1.y
            
            if (abs(diffX) > abs(diffY)) {
                if (abs(diffX) > swipeThreshold && abs(velocityX) > swipeVelocityThreshold) {
                    if (diffX > 0) onSwipeRight() else onSwipeLeft()
                    return true
                }
            } else {
                if (abs(diffY) > swipeThreshold && abs(velocityY) > swipeVelocityThreshold) {
                    if (diffY > 0) onSwipeDown() else onSwipeUp()
                    return true
                }
            }
            return false
        }
    }
    
    open fun onClick(view: View) {}
    open fun onLongClick(view: View) {}
    open fun onSwipeLeft() {}
    open fun onSwipeRight() {}
    open fun onSwipeUp() {}
    open fun onSwipeDown() {}
}
