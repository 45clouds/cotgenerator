package com.jon.common.service

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener

abstract class ThreadManager(protected val prefs: SharedPreferences) : OnSharedPreferenceChangeListener {

    abstract fun start()

    abstract fun shutdown()

    abstract fun isRunning(): Boolean

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String) {
        /* If any preferences are changed, kill the thread and instantly reload with the new settings */
        if (isRunning()) {
            shutdown()
            start()
        }
    }
}
