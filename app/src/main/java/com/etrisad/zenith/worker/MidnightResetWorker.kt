package com.etrisad.zenith.worker

import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.etrisad.zenith.util.AlarmTasksSchedulingHelper
import com.etrisad.zenith.util.SharedPrefsHelper

class MidnightResetWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        SharedPrefsHelper.setShortsScreenTimeMs(applicationContext, 0L)

        val serviceIntent = Intent(applicationContext, com.etrisad.zenith.service.AppUsageMonitorService::class.java).apply {
            action = ACTION_MIDNIGHT_RESET_SERVICE
        }
        applicationContext.startService(serviceIntent)

        AlarmTasksSchedulingHelper.scheduleMidnightResetTask(applicationContext)
        
        return Result.success()
    }

    companion object {
        const val ACTION_MIDNIGHT_RESET_SERVICE = "com.etrisad.zenith.action.MIDNIGHT_RESET_SERVICE"
    }
}
