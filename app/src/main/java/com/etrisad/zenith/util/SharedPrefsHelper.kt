package com.etrisad.zenith.util

import android.content.Context
import kotlinx.coroutines.runBlocking
import com.etrisad.zenith.data.preferences.UserPreferencesRepository

object SharedPrefsHelper {
    fun setShortsScreenTimeMs(context: Context, ms: Long) {
        val repository = UserPreferencesRepository(context)
        runBlocking {
            repository.setShortsScreenTimeMs(ms)
        }
    }
}
