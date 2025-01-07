package com.tis.timestampcamerafree.lifecycle

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

class AppLifecycleListener(private val lifecycleListener: LifecycleListener) : LifecycleObserver {

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onMoveToForeground() { // app moved to foreground
        lifecycleListener.onMoveToForeground()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onMoveToBackground() { // app moved to background
        lifecycleListener.onMoveToBackground()
    }


    interface LifecycleListener {
        fun onMoveToForeground()
        fun onMoveToBackground()
    }

}