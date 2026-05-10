package com.etrisad.zenith.service

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateListOf
import com.etrisad.zenith.ui.components.AppGoalOverlayContent
import com.etrisad.zenith.ui.theme.ZenithTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppGoalOverlayActivity : ComponentActivity() {

    private val activePackageNames = mutableStateListOf<String>()
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val packageList = intent.getStringArrayListExtra(EXTRA_PACKAGE_NAMES) ?: arrayListOf()
        if (packageList.isEmpty()) {
            finish()
            return
        }
        
        activePackageNames.addAll(packageList)
        playGoalSound(packageList.firstOrNull())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            ZenithTheme {
                AppGoalOverlayContent(
                    packageNames = activePackageNames,
                    onAnswer = { pkg ->
                        val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
                        if (launchIntent != null) {
                            startActivity(launchIntent)
                        }
                        finish()
                    },
                    onSnooze = {
                        val shieldRepo = (application as com.etrisad.zenith.ZenithApplication).shieldRepository
                        val packagesToSnooze = activePackageNames.toList()
                        CoroutineScope(Dispatchers.IO).launch {
                            packagesToSnooze.forEach { pkg ->
                                val shield = shieldRepo.getShieldByPackageName(pkg)
                                if (shield != null) {
                                    val periodMillis = shield.goalReminderPeriodMinutes * 60 * 1000L
                                    val snoozeTime = 5 * 60 * 1000L
                                    val newTimestamp = System.currentTimeMillis() - periodMillis + snoozeTime
                                    shieldRepo.updateShield(shield.copy(lastGoalReminderTimestamp = newTimestamp))
                                }
                            }
                        }
                        finish()
                    },
                    onHangUp = {
                        finish()
                    }
                )
            }
        }
    }

    private fun playGoalSound(packageName: String?) {
        if (packageName == null) return
        val shieldRepo = (application as com.etrisad.zenith.ZenithApplication).shieldRepository
        CoroutineScope(Dispatchers.IO).launch {
            val shield = shieldRepo.getShieldByPackageName(packageName)
            // Play sound if shield is null (test scenario) or if explicitly enabled
            if (shield == null || (shield.isGoalCallerEnabled && shield.isGoalCallerSoundEnabled)) {
                withContext(Dispatchers.Main) {
                    try {
                        mediaPlayer?.release()
                        val soundUri = if (shield?.goalCallerSoundUri != null) {
                            Uri.parse(shield.goalCallerSoundUri)
                        } else {
                            android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE)
                        }

                        mediaPlayer = MediaPlayer().apply {
                            setDataSource(this@AppGoalOverlayActivity, soundUri)
                            setAudioAttributes(
                                AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_ALARM)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                    .build()
                            )
                            isLooping = true
                            prepare()
                            start()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val packageList = intent.getStringArrayListExtra(EXTRA_PACKAGE_NAMES) ?: return
        packageList.forEach { pkg ->
            if (pkg !in activePackageNames) {
                activePackageNames.add(pkg)
            }
        }
    }

    companion object {
        const val EXTRA_PACKAGE_NAMES = "extra_package_names"

        fun start(context: Context, packageNames: List<String>) {
            val intent = Intent(context, AppGoalOverlayActivity::class.java).apply {
                putStringArrayListExtra(EXTRA_PACKAGE_NAMES, ArrayList(packageNames))
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }
            context.startActivity(intent)
        }
    }
}
