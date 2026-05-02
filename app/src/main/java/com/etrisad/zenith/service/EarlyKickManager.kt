package com.etrisad.zenith.service

import java.util.Calendar

/**
 * Manages the "Early Kick" feature state.
 * Ensures that the early kick (2 minutes before limit) happens only once per app session/day.
 */
class EarlyKickManager {
    private val kickedApps = mutableSetOf<String>()
    private var lastResetDay = -1

    /**
     * Checks if an app should be early kicked.
     * @param packageName The package name of the app.
     * @param remainingTimeMillis The remaining time for the app in milliseconds.
     * @param isEnabled Whether the early kick feature is enabled in settings.
     * @return True if the app should be kicked now.
     */
    fun shouldKick(packageName: String, remainingTimeMillis: Long, isEnabled: Boolean): Boolean {
        if (!isEnabled) return false
        
        checkDayChange()

        // Kick if remaining time is less than 2 minutes (120,000ms)
        // but only if it hasn't been kicked yet today/in this session.
        if (remainingTimeMillis in 1..120000L && !kickedApps.contains(packageName)) {
            kickedApps.add(packageName)
            return true
        }
        
        return false
    }

    private fun checkDayChange() {
        val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        if (lastResetDay != today) {
            kickedApps.clear()
            lastResetDay = today
        }
    }

    fun reset() {
        kickedApps.clear()
    }
}
