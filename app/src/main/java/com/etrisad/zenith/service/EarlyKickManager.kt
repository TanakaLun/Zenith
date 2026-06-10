package com.etrisad.zenith.service

import java.util.Calendar

class EarlyKickManager {
    private val kickedApps = mutableSetOf<String>()
    private var lastResetDay = -1
    private var lastResetCheckTime = 0L

    fun shouldKick(packageName: String, remainingTimeMillis: Long, isEnabled: Boolean): Boolean {
        if (!isEnabled) return false
        
        checkDayChange()

        if (remainingTimeMillis in 1..300000L && !kickedApps.contains(packageName)) {
            kickedApps.add(packageName)
            return true
        }
        
        return false
    }

    private fun checkDayChange() {
        val now = System.currentTimeMillis()
        if (now - lastResetCheckTime < 60000) return
        lastResetCheckTime = now
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
