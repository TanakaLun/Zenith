package com.etrisad.zenith.data.preferences

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.etrisad.zenith.data.local.entity.FocusType
import com.etrisad.zenith.data.repository.ShieldRepository
import com.etrisad.zenith.ui.theme.FontAxes
import com.etrisad.zenith.ui.theme.GSFlexSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
val Context.runtimeDataStore: DataStore<Preferences> by preferencesDataStore(name = "runtime_state")

enum class ThemeConfig {
    FOLLOW_SYSTEM, LIGHT, DARK
}

enum class FontOption {
    SYSTEM, GOOGLE_SANS_FLEX, NUNITO
}

enum class GSFlexPreset {
    ZENITH, NEO, COMPACT, AIRY, CUSTOM
}

enum class PerformanceLevel(val labelRes: String, val descriptionRes: String) {
    MAX_RESPONSIVENESS("Max Responsiveness", "Overlay appears instantly (0.5-30s). Higher battery usage."),
    RESPONSIVE("Responsive", "Overlay appears within 1-60s. Moderate battery usage."),
    BALANCED("Balanced", "Overlay appears within 3-120s. Optimized battery usage. [DEFAULT]"),
    BATTERY_SAVER("Battery Saver", "Overlay appears within 5-300s. Low battery usage."),
    MAX_BATTERY("Max Battery", "Overlay appears within 10-600s. Minimal battery usage."),
    CUSTOM("Custom", "Manually tuned performance values.")
}

fun PerformanceLevel.isPreset() = this != PerformanceLevel.CUSTOM

data class PerformanceConfig(
    val a11yActiveDelay: Long = 120_000L,
    val a11yInactiveDelay: Long = 3_000L,
    val screenOffDelay: Long = 300_000L,
    val powerSaveDelay: Long = 300_000L,
    val usageStatsCacheMs: Long = 600_000L,
    val shieldDbWriteMs: Long = 300_000L,
    val shieldDbWriteNearMs: Long = 120_000L,
    val launcherCacheMs: Long = 3_600_000L,
    val goalReminderTick: Long = 2L,
    val dayChangeTick: Long = 2L,
)

fun PerformanceLevel.toConfig(): PerformanceConfig = when (this) {
    PerformanceLevel.MAX_RESPONSIVENESS -> PerformanceConfig(
        a11yActiveDelay = 30_000L,
        a11yInactiveDelay = 1_000L,
        screenOffDelay = 60_000L,
        powerSaveDelay = 60_000L,
        usageStatsCacheMs = 10_000L,
        shieldDbWriteMs = 60_000L,
        shieldDbWriteNearMs = 30_000L,
        launcherCacheMs = 1_800_000L,
        goalReminderTick = 1L,
        dayChangeTick = 1L,
    )
    PerformanceLevel.RESPONSIVE -> PerformanceConfig(
        a11yActiveDelay = 60_000L,
        a11yInactiveDelay = 2_000L,
        screenOffDelay = 120_000L,
        powerSaveDelay = 120_000L,
        usageStatsCacheMs = 30_000L,
        shieldDbWriteMs = 120_000L,
        shieldDbWriteNearMs = 60_000L,
        launcherCacheMs = 3_600_000L,
        goalReminderTick = 1L,
        dayChangeTick = 1L,
    )
    PerformanceLevel.BALANCED -> PerformanceConfig(
        a11yActiveDelay = 120_000L,
        a11yInactiveDelay = 3_000L,
        screenOffDelay = 300_000L,
        powerSaveDelay = 300_000L,
        usageStatsCacheMs = 600_000L,
        shieldDbWriteMs = 300_000L,
        shieldDbWriteNearMs = 120_000L,
        launcherCacheMs = 3_600_000L,
        goalReminderTick = 2L,
        dayChangeTick = 2L,
    )
    PerformanceLevel.BATTERY_SAVER -> PerformanceConfig(
        a11yActiveDelay = 300_000L,
        a11yInactiveDelay = 5_000L,
        screenOffDelay = 600_000L,
        powerSaveDelay = 600_000L,
        usageStatsCacheMs = 1_800_000L,
        shieldDbWriteMs = 600_000L,
        shieldDbWriteNearMs = 300_000L,
        launcherCacheMs = 7_200_000L,
        goalReminderTick = 5L,
        dayChangeTick = 5L,
    )
    PerformanceLevel.MAX_BATTERY -> PerformanceConfig(
        a11yActiveDelay = 600_000L,
        a11yInactiveDelay = 10_000L,
        screenOffDelay = 1_800_000L,
        powerSaveDelay = 1_800_000L,
        usageStatsCacheMs = 3_600_000L,
        shieldDbWriteMs = 1_800_000L,
        shieldDbWriteNearMs = 600_000L,
        launcherCacheMs = 14_400_000L,
        goalReminderTick = 10L,
        dayChangeTick = 10L,
    )
    PerformanceLevel.CUSTOM -> PerformanceConfig()
}

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val THEME_CONFIG = stringPreferencesKey("theme_config")
        val FONT_OPTION = stringPreferencesKey("font_option")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val ACCESSIBILITY_DISABLED = booleanPreferencesKey("accessibility_disabled")
        val SCREEN_TIME_TARGET = intPreferencesKey("screen_time_target")
        val EMERGENCY_RECHARGE_DURATION_MINUTES = intPreferencesKey("emergency_recharge_duration_minutes")
        val DELAY_APP_DURATION_SECONDS = intPreferencesKey("delay_app_duration_seconds")
        val SESSION_USAGE_OVERLAY_ENABLED = booleanPreferencesKey("session_usage_overlay_enabled")
        val SESSION_USAGE_OVERLAY_SIZE = intPreferencesKey("session_usage_overlay_size")
        val SESSION_USAGE_OVERLAY_OPACITY = intPreferencesKey("session_usage_overlay_opacity")
        val WHITELISTED_PACKAGES = stringPreferencesKey("whitelisted_packages")
        val AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
        val BACKUP_DIRECTORY_URI = stringPreferencesKey("backup_directory_uri")
        val BACKUP_INTERVAL_HOURS = intPreferencesKey("backup_interval_hours")
        val FLOATING_TAB_BAR_ENABLED = booleanPreferencesKey("floating_tab_bar_enabled")
        val EXPRESSIVE_COLORS = booleanPreferencesKey("expressive_colors")
        val TOTAL_USAGE_PILL_ENABLED = booleanPreferencesKey("total_usage_pill_enabled")

        val BEDTIME_ENABLED = booleanPreferencesKey("bedtime_enabled")
        val BEDTIME_START_TIME = stringPreferencesKey("bedtime_start_time")
        val BEDTIME_END_TIME = stringPreferencesKey("bedtime_end_time")
        val BEDTIME_DAYS = stringPreferencesKey("bedtime_days")
        val BEDTIME_DND_ENABLED = booleanPreferencesKey("bedtime_dnd_enabled")
        val BEDTIME_WIND_DOWN_ENABLED = booleanPreferencesKey("bedtime_wind_down_enabled")
        val BEDTIME_NOTIFICATION_ENABLED = booleanPreferencesKey("bedtime_notification_enabled")
        val BEDTIME_WHITELISTED_PACKAGES = stringPreferencesKey("bedtime_whitelisted_packages")
        val USER_NAME = stringPreferencesKey("user_name")
        val EARLY_KICK_ENABLED = booleanPreferencesKey("early_kick_enabled")
        val INTERCEPT_AUDIO_FOCUS_ENABLED = booleanPreferencesKey("intercept_audio_focus_enabled")
        val SHOW_DATABASE_INDICATOR = booleanPreferencesKey("show_database_indicator")
        val DEVELOPER_MODE_ENABLED = booleanPreferencesKey("developer_mode_enabled")
        val PREFER_SYSTEM_USAGE_HISTORY = booleanPreferencesKey("prefer_system_usage_history")
        val ONBOARDING_STATS_COMPLETED = booleanPreferencesKey("onboarding_stats_completed")
        val ONBOARDING_UPDATE_COMPLETED = booleanPreferencesKey("onboarding_update_completed")
        val HUD_HIDE_FEATURE_LEARNED = booleanPreferencesKey("hud_hide_feature_learned")
        val SMART_REPAIR_ON_REFRESH = booleanPreferencesKey("smart_repair_on_refresh")
        val ALLOW_REPAIR_NON_UNAVAILABLE = booleanPreferencesKey("allow_repair_non_unavailable")
        val CUSTOM_DELAY_ENABLED = booleanPreferencesKey("custom_delay_enabled")
        val DELAY_POWER_SAVE = longPreferencesKey("delay_power_save")
        val DELAY_OVERLAY_SHOWING = longPreferencesKey("delay_overlay_showing")
        val DELAY_GOAL_NEAR = longPreferencesKey("delay_goal_near")
        val DELAY_GOAL_MID = longPreferencesKey("delay_goal_mid")
        val DELAY_GOAL_FAR = longPreferencesKey("delay_goal_far")
        val DELAY_SHIELD_VERY_FAR = longPreferencesKey("delay_shield_very_far")
        val DELAY_SHIELD_FAR = longPreferencesKey("delay_shield_far")
        val DELAY_SHIELD_MID = longPreferencesKey("delay_shield_mid")
        val DELAY_SHIELD_NEAR = longPreferencesKey("delay_shield_near")
        val DELAY_DEFAULT = longPreferencesKey("delay_default")
        val MINDFUL_GATEWAY_ENABLED = booleanPreferencesKey("mindful_gateway_enabled")
        val REFRESH_ON_OPEN_USAGE_STATS = booleanPreferencesKey("refresh_on_open_usage_stats")
        val CHECK_UPDATE_ON_START = booleanPreferencesKey("check_update_on_start")
        val BATTERY_STATS_RESET_ENABLED = booleanPreferencesKey("battery_stats_reset_enabled")

        val PERFORMANCE_LEVEL = stringPreferencesKey("performance_level")
        val PERF_A11Y_ACTIVE_DELAY = longPreferencesKey("perf_a11y_active_delay")
        val PERF_A11Y_INACTIVE_DELAY = longPreferencesKey("perf_a11y_inactive_delay")
        val PERF_SCREEN_OFF_DELAY = longPreferencesKey("perf_screen_off_delay")
        val PERF_POWER_SAVE_DELAY = longPreferencesKey("perf_power_save_delay")
        val PERF_USAGE_STATS_CACHE = longPreferencesKey("perf_usage_stats_cache")
        val PERF_SHIELD_DB_WRITE = longPreferencesKey("perf_shield_db_write")
        val PERF_SHIELD_DB_WRITE_NEAR = longPreferencesKey("perf_shield_db_write_near")
        val PERF_LAUNCHER_CACHE = longPreferencesKey("perf_launcher_cache")
        val PERF_GOAL_REMINDER_TICK = longPreferencesKey("perf_goal_reminder_tick")
        val PERF_DAY_CHANGE_TICK = longPreferencesKey("perf_day_change_tick")

        val GS_FLEX_PRESET = stringPreferencesKey("gs_flex_preset")
        val GS_D_WGHT = floatPreferencesKey("gs_d_wght")
        val GS_D_WDTH = floatPreferencesKey("gs_d_wdth")
        val GS_D_OPSZ = floatPreferencesKey("gs_d_opsz")
        val GS_D_GRAD = floatPreferencesKey("gs_d_grad")
        val GS_D_SLNT = floatPreferencesKey("gs_d_slnt")
        val GS_D_ROND = floatPreferencesKey("gs_d_rond")
        val GS_H_WGHT = floatPreferencesKey("gs_h_wght")
        val GS_H_WDTH = floatPreferencesKey("gs_h_wdth")
        val GS_H_OPSZ = floatPreferencesKey("gs_h_opsz")
        val GS_H_GRAD = floatPreferencesKey("gs_h_grad")
        val GS_H_SLNT = floatPreferencesKey("gs_h_slnt")
        val GS_H_ROND = floatPreferencesKey("gs_h_rond")
        val GS_B_WGHT = floatPreferencesKey("gs_b_wght")
        val GS_B_WDTH = floatPreferencesKey("gs_b_wdth")
        val GS_B_OPSZ = floatPreferencesKey("gs_b_opsz")
        val GS_B_GRAD = floatPreferencesKey("gs_b_grad")
        val GS_B_SLNT = floatPreferencesKey("gs_b_slnt")
        val GS_B_ROND = floatPreferencesKey("gs_b_rond")
    }

    private object RuntimeKeys {
        val LAST_RESET_DATE = stringPreferencesKey("last_reset_date")
        val LAST_STREAK_CHECK_DATE = stringPreferencesKey("last_streak_check_date")
        val GLOBAL_CURRENT_STREAK = intPreferencesKey("global_current_streak")
        val GLOBAL_BEST_STREAK = intPreferencesKey("global_best_streak")
        val GLOBAL_LAST_STREAK_UPDATE_TIMESTAMP = longPreferencesKey("global_last_streak_update_timestamp")
        val LAST_BACKUP_TIMESTAMP = longPreferencesKey("last_backup_timestamp")
        val LAST_KNOWN_DAILY_USAGE = longPreferencesKey("last_known_daily_usage")
        val LAST_KNOWN_DAILY_USAGE_DATE = stringPreferencesKey("last_known_daily_usage_date")
        val BEDTIME_CURRENT_STREAK = intPreferencesKey("bedtime_mode_streak_current")
        val BEDTIME_BEST_STREAK = intPreferencesKey("bedtime_mode_streak_best")
        val BEDTIME_STREAK_RESET_DATE = stringPreferencesKey("bedtime_streak_reset_date")
        val LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")
        val WHITELIST_INITIALIZED = booleanPreferencesKey("whitelist_initialized")
        val SHORTS_SCREEN_TIME_MS = longPreferencesKey("shorts_screen_time_ms")
        val LAST_CHARGE_TIMESTAMP = longPreferencesKey("last_charge_timestamp")
        val MANUAL_RESET_TIMESTAMPS = stringPreferencesKey("manual_reset_timestamps")
        val STREAK_RECOVERY_PERFORMED = booleanPreferencesKey("streak_recovery_performed")
    }

    val userPreferencesFlow: Flow<UserPreferences> = combine(
        context.dataStore.data.catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception },
        context.runtimeDataStore.data.catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
    ) { settings, runtime ->
        UserPreferences(
            themeConfig = ThemeConfig.valueOf(settings[PreferencesKeys.THEME_CONFIG] ?: ThemeConfig.FOLLOW_SYSTEM.name),
            fontOption = FontOption.valueOf(settings[PreferencesKeys.FONT_OPTION] ?: FontOption.GOOGLE_SANS_FLEX.name),
            dynamicColor = settings[PreferencesKeys.DYNAMIC_COLOR] ?: true,
            accessibilityDisabled = settings[PreferencesKeys.ACCESSIBILITY_DISABLED] ?: false,
            screenTimeTargetMinutes = settings[PreferencesKeys.SCREEN_TIME_TARGET] ?: 0,
            emergencyRechargeDurationMinutes = settings[PreferencesKeys.EMERGENCY_RECHARGE_DURATION_MINUTES] ?: 60,
            delayAppDurationSeconds = settings[PreferencesKeys.DELAY_APP_DURATION_SECONDS] ?: 30,
            sessionUsageOverlayEnabled = settings[PreferencesKeys.SESSION_USAGE_OVERLAY_ENABLED] ?: false,
            sessionUsageOverlaySize = settings[PreferencesKeys.SESSION_USAGE_OVERLAY_SIZE] ?: 100,
            sessionUsageOverlayOpacity = settings[PreferencesKeys.SESSION_USAGE_OVERLAY_OPACITY] ?: 90,
            whitelistedPackages = settings[PreferencesKeys.WHITELISTED_PACKAGES]?.split(",")?.filter { it.isNotEmpty() }?.toSet() ?: emptySet(),
            lastResetDate = runtime[RuntimeKeys.LAST_RESET_DATE] ?: "",
            lastStreakCheckDate = runtime[RuntimeKeys.LAST_STREAK_CHECK_DATE] ?: "",
            globalCurrentStreak = runtime[RuntimeKeys.GLOBAL_CURRENT_STREAK] ?: 0,
            globalBestStreak = runtime[RuntimeKeys.GLOBAL_BEST_STREAK] ?: 0,
            globalLastStreakUpdateTimestamp = runtime[RuntimeKeys.GLOBAL_LAST_STREAK_UPDATE_TIMESTAMP] ?: 0L,
            autoBackupEnabled = settings[PreferencesKeys.AUTO_BACKUP_ENABLED] ?: false,
            backupDirectoryUri = settings[PreferencesKeys.BACKUP_DIRECTORY_URI] ?: "",
            backupIntervalHours = settings[PreferencesKeys.BACKUP_INTERVAL_HOURS] ?: 3,
            lastBackupTimestamp = runtime[RuntimeKeys.LAST_BACKUP_TIMESTAMP] ?: 0L,
            floatingTabBarEnabled = settings[PreferencesKeys.FLOATING_TAB_BAR_ENABLED] ?: false,
            expressiveColors = settings[PreferencesKeys.EXPRESSIVE_COLORS] ?: false,
            totalUsagePillEnabled = settings[PreferencesKeys.TOTAL_USAGE_PILL_ENABLED] ?: false,
            lastKnownDailyUsage = runtime[RuntimeKeys.LAST_KNOWN_DAILY_USAGE] ?: 0L,
            lastKnownDailyUsageDate = runtime[RuntimeKeys.LAST_KNOWN_DAILY_USAGE_DATE] ?: "",
            bedtimeEnabled = settings[PreferencesKeys.BEDTIME_ENABLED] ?: false,
            bedtimeStartTime = settings[PreferencesKeys.BEDTIME_START_TIME] ?: "22:00",
            bedtimeEndTime = settings[PreferencesKeys.BEDTIME_END_TIME] ?: "07:00",
            bedtimeDays = settings[PreferencesKeys.BEDTIME_DAYS]?.split(",")?.filter { it.isNotEmpty() }?.map { it.toInt() }?.toSet() ?: setOf(1, 2, 3, 4, 5, 6, 7),
            bedtimeDndEnabled = settings[PreferencesKeys.BEDTIME_DND_ENABLED] ?: false,
            bedtimeWindDownEnabled = settings[PreferencesKeys.BEDTIME_WIND_DOWN_ENABLED] ?: false,
            bedtimeNotificationEnabled = settings[PreferencesKeys.BEDTIME_NOTIFICATION_ENABLED] ?: true,
            bedtimeWhitelistedPackages = settings[PreferencesKeys.BEDTIME_WHITELISTED_PACKAGES]?.split(",")?.filter { it.isNotEmpty() }?.toSet() ?: emptySet(),
            bedtimeCurrentStreak = runtime[RuntimeKeys.BEDTIME_CURRENT_STREAK] ?: 0,
            bedtimeBestStreak = runtime[RuntimeKeys.BEDTIME_BEST_STREAK] ?: 0,
            bedtimeStreakResetDate = runtime[RuntimeKeys.BEDTIME_STREAK_RESET_DATE] ?: "",
            userName = settings[PreferencesKeys.USER_NAME] ?: "User",
            earlyKickEnabled = settings[PreferencesKeys.EARLY_KICK_ENABLED] ?: false,
            interceptAudioFocusEnabled = settings[PreferencesKeys.INTERCEPT_AUDIO_FOCUS_ENABLED] ?: true,
            showDatabaseIndicator = settings[PreferencesKeys.SHOW_DATABASE_INDICATOR] ?: false,
            developerModeEnabled = settings[PreferencesKeys.DEVELOPER_MODE_ENABLED] ?: false,
            lastSyncTimestamp = runtime[RuntimeKeys.LAST_SYNC_TIMESTAMP] ?: 0L,
            preferSystemUsageHistory = settings[PreferencesKeys.PREFER_SYSTEM_USAGE_HISTORY] ?: true,
            onboardingStatsCompleted = settings[PreferencesKeys.ONBOARDING_STATS_COMPLETED] ?: false,
            onboardingUpdateCompleted = settings[PreferencesKeys.ONBOARDING_UPDATE_COMPLETED] ?: false,
            whitelistInitialized = runtime[RuntimeKeys.WHITELIST_INITIALIZED] ?: false,
            hudHideFeatureLearned = settings[PreferencesKeys.HUD_HIDE_FEATURE_LEARNED] ?: false,
            smartRepairOnRefresh = settings[PreferencesKeys.SMART_REPAIR_ON_REFRESH] ?: false,
            allowRepairNonUnavailable = settings[PreferencesKeys.ALLOW_REPAIR_NON_UNAVAILABLE] ?: false,
            shortsScreenTimeMs = runtime[RuntimeKeys.SHORTS_SCREEN_TIME_MS] ?: 0L,
            customDelayEnabled = settings[PreferencesKeys.CUSTOM_DELAY_ENABLED] ?: false,
            delayPowerSave = settings[PreferencesKeys.DELAY_POWER_SAVE] ?: 5000L,
            delayOverlayShowing = settings[PreferencesKeys.DELAY_OVERLAY_SHOWING] ?: 8000L,
            delayGoalNear = settings[PreferencesKeys.DELAY_GOAL_NEAR] ?: 600L,
            delayGoalMid = settings[PreferencesKeys.DELAY_GOAL_MID] ?: 1200L,
            delayGoalFar = settings[PreferencesKeys.DELAY_GOAL_FAR] ?: 1800L,
            delayShieldVeryFar = settings[PreferencesKeys.DELAY_SHIELD_VERY_FAR] ?: 5000L,
            delayShieldFar = settings[PreferencesKeys.DELAY_SHIELD_FAR] ?: 3000L,
            delayShieldMid = settings[PreferencesKeys.DELAY_SHIELD_MID] ?: 1500L,
            delayShieldNear = settings[PreferencesKeys.DELAY_SHIELD_NEAR] ?: 600L,
            delayDefault = settings[PreferencesKeys.DELAY_DEFAULT] ?: 1200L,
            mindfulGatewayEnabled = settings[PreferencesKeys.MINDFUL_GATEWAY_ENABLED] ?: false,
            refreshOnOpenUsageStats = settings[PreferencesKeys.REFRESH_ON_OPEN_USAGE_STATS] ?: false,
            checkUpdateOnStart = settings[PreferencesKeys.CHECK_UPDATE_ON_START] ?: false,
            batteryStatsResetEnabled = settings[PreferencesKeys.BATTERY_STATS_RESET_ENABLED] ?: false,
            performanceLevel = PerformanceLevel.valueOf(settings[PreferencesKeys.PERFORMANCE_LEVEL] ?: PerformanceLevel.BALANCED.name),
            perfA11yActiveDelay = settings[PreferencesKeys.PERF_A11Y_ACTIVE_DELAY] ?: PerformanceConfig().a11yActiveDelay,
            perfA11yInactiveDelay = settings[PreferencesKeys.PERF_A11Y_INACTIVE_DELAY] ?: PerformanceConfig().a11yInactiveDelay,
            perfScreenOffDelay = settings[PreferencesKeys.PERF_SCREEN_OFF_DELAY] ?: PerformanceConfig().screenOffDelay,
            perfPowerSaveDelay = settings[PreferencesKeys.PERF_POWER_SAVE_DELAY] ?: PerformanceConfig().powerSaveDelay,
            perfUsageStatsCacheMs = settings[PreferencesKeys.PERF_USAGE_STATS_CACHE] ?: PerformanceConfig().usageStatsCacheMs,
            perfShieldDbWriteMs = settings[PreferencesKeys.PERF_SHIELD_DB_WRITE] ?: PerformanceConfig().shieldDbWriteMs,
            perfShieldDbWriteNearMs = settings[PreferencesKeys.PERF_SHIELD_DB_WRITE_NEAR] ?: PerformanceConfig().shieldDbWriteNearMs,
            perfLauncherCacheMs = settings[PreferencesKeys.PERF_LAUNCHER_CACHE] ?: PerformanceConfig().launcherCacheMs,
            perfGoalReminderTick = settings[PreferencesKeys.PERF_GOAL_REMINDER_TICK] ?: PerformanceConfig().goalReminderTick,
            perfDayChangeTick = settings[PreferencesKeys.PERF_DAY_CHANGE_TICK] ?: PerformanceConfig().dayChangeTick,
            lastChargeTimestamp = runtime[RuntimeKeys.LAST_CHARGE_TIMESTAMP] ?: 0L,
            manualResetTimestamps = runtime[RuntimeKeys.MANUAL_RESET_TIMESTAMPS]?.split(",")
                ?.filter { it.contains(":") }
                ?.associate { 
                    val parts = it.split(":")
                    parts[0] to (parts[1].toLongOrNull() ?: 0L)
                } ?: emptyMap(),
            gsFlexSettings = GSFlexSettings(
                preset = GSFlexPreset.valueOf(settings[PreferencesKeys.GS_FLEX_PRESET] ?: GSFlexPreset.ZENITH.name),
                display = FontAxes(
                    weight = settings[PreferencesKeys.GS_D_WGHT] ?: 400f,
                    width = settings[PreferencesKeys.GS_D_WDTH] ?: 100f,
                    opsz = settings[PreferencesKeys.GS_D_OPSZ] ?: 72f,
                    grade = settings[PreferencesKeys.GS_D_GRAD] ?: 0f,
                    slant = settings[PreferencesKeys.GS_D_SLNT] ?: 0f,
                    roundness = settings[PreferencesKeys.GS_D_ROND] ?: 0f
                ),
                headline = FontAxes(
                    weight = settings[PreferencesKeys.GS_H_WGHT] ?: 400f,
                    width = settings[PreferencesKeys.GS_H_WDTH] ?: 100f,
                    opsz = settings[PreferencesKeys.GS_H_OPSZ] ?: 32f,
                    grade = settings[PreferencesKeys.GS_H_GRAD] ?: 0f,
                    slant = settings[PreferencesKeys.GS_H_SLNT] ?: 0f,
                    roundness = settings[PreferencesKeys.GS_H_ROND] ?: 0f
                ),
                body = FontAxes(
                    weight = settings[PreferencesKeys.GS_B_WGHT] ?: 400f,
                    width = settings[PreferencesKeys.GS_B_WDTH] ?: 100f,
                    opsz = settings[PreferencesKeys.GS_B_OPSZ] ?: 16f,
                    grade = settings[PreferencesKeys.GS_B_GRAD] ?: 0f,
                    slant = settings[PreferencesKeys.GS_B_SLNT] ?: 0f,
                    roundness = settings[PreferencesKeys.GS_B_ROND] ?: 0f
                ),
            ),
            streakRecoveryPerformed = runtime[RuntimeKeys.STREAK_RECOVERY_PERFORMED] ?: false
        )
    }


    suspend fun setUserName(name: String) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.USER_NAME] = name }
    }

    suspend fun setThemeConfig(themeConfig: ThemeConfig) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.THEME_CONFIG] = themeConfig.name }
    }

    suspend fun setFontOption(fontOption: FontOption) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.FONT_OPTION] = fontOption.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun setAccessibilityDisabled(disabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.ACCESSIBILITY_DISABLED] = disabled }
    }

    suspend fun setScreenTimeTarget(minutes: Int) {
        val currentTarget = userPreferencesFlow.first().screenTimeTargetMinutes
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SCREEN_TIME_TARGET] = minutes
        }
        if (minutes > currentTarget && currentTarget > 0) {
            context.runtimeDataStore.edit { preferences ->
                preferences[RuntimeKeys.GLOBAL_CURRENT_STREAK] = 0
            }
        }
    }

    suspend fun setEmergencyRechargeDuration(minutes: Int) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.EMERGENCY_RECHARGE_DURATION_MINUTES] = minutes }
    }

    suspend fun setDelayAppDuration(seconds: Int) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.DELAY_APP_DURATION_SECONDS] = seconds }
    }

    suspend fun setSessionUsageOverlayEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.SESSION_USAGE_OVERLAY_ENABLED] = enabled }
    }

    suspend fun setSessionUsageOverlaySize(size: Int) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.SESSION_USAGE_OVERLAY_SIZE] = size }
    }

    suspend fun setSessionUsageOverlayOpacity(opacity: Int) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.SESSION_USAGE_OVERLAY_OPACITY] = opacity }
    }

    suspend fun setWhitelistedPackages(packages: Set<String>) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.WHITELISTED_PACKAGES] = packages.joinToString(",") }
    }

    suspend fun initializeDefaultWhitelist() {
        val prefs = userPreferencesFlow.first()
        if (!prefs.whitelistInitialized && prefs.whitelistedPackages.isEmpty()) {
            withContext(Dispatchers.IO) {
                val pm = context.packageManager
                val installedApps = try {
                    pm.getInstalledApplications(0)
                } catch (e: Exception) {
                    emptyList()
                }

                val systemApps = installedApps.filter {
                    val isSystemFlag = (it.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0
                    val hasLauncher = pm.getLaunchIntentForPackage(it.packageName) != null
                    val pkg = it.packageName

                    val isCoreComponent = pkg == "android" ||
                            pkg.startsWith("com.android.settings") ||
                            pkg.startsWith("com.android.systemui") ||
                            pkg.startsWith("com.android.shell") ||
                            pkg.startsWith("com.android.phone") ||
                            pkg.startsWith("com.android.angle") ||
                            pkg.startsWith("com.android.providers") ||
                            pkg.startsWith("com.google.android.angle") ||
                            pkg.startsWith("com.google.android.setupwizard") ||
                            pkg.contains("restore") ||
                            pkg.contains("overlay") ||
                            pkg.contains("documentsui")

                    isSystemFlag && (!hasLauncher || isCoreComponent)
                }.map { it.packageName }.toSet()

                if (systemApps.isNotEmpty()) {
                    setWhitelistedPackages(systemApps)
                }

                context.runtimeDataStore.edit { preferences ->
                    preferences[RuntimeKeys.WHITELIST_INITIALIZED] = true
                }
            }
        }
    }

    suspend fun setLastResetDate(date: String) {
        context.runtimeDataStore.edit { preferences -> preferences[RuntimeKeys.LAST_RESET_DATE] = date }
    }

    suspend fun setLastStreakCheckDate(date: String) {
        context.runtimeDataStore.edit { preferences -> preferences[RuntimeKeys.LAST_STREAK_CHECK_DATE] = date }
    }

    suspend fun updateGlobalStreak(current: Int, best: Int, timestamp: Long) {
        context.runtimeDataStore.edit { preferences ->
            preferences[RuntimeKeys.GLOBAL_CURRENT_STREAK] = current
            preferences[RuntimeKeys.GLOBAL_BEST_STREAK] = best
            preferences[RuntimeKeys.GLOBAL_LAST_STREAK_UPDATE_TIMESTAMP] = timestamp
        }
    }

    suspend fun refreshGlobalStreak(shieldRepository: ShieldRepository): Pair<Int, Int> {
        val prefs = userPreferencesFlow.first()
        val targetMillis = prefs.screenTimeTargetMinutes * 60 * 1000L
        if (targetMillis <= 0) {
            updateGlobalStreak(0, prefs.globalBestStreak, System.currentTimeMillis())
            return Pair(0, prefs.globalBestStreak)
        }

        shieldRepository.isShieldsLoaded.first { it }
        val dbUsage = shieldRepository.getAllUsage().first()
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val (launcherApps, launcherPackage) = try {
            val pm = context.packageManager
            val apps = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0).map { it.activityInfo.packageName }.toSet()
            val lPkg = pm.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
            apps to lPkg
        } catch (_: Exception) { emptySet<String>() to null }

        val excludePackages = setOfNotNull(context.packageName, launcherPackage)

        val calendar = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
        val todayStart = calendar.timeInMillis

        val stats = com.etrisad.zenith.util.ScreenUsageHelper.fetchDetailedUsageToday(usageStatsManager).appUsageMap
        var totalToday = 0L
        stats.forEach { (pkg, time) ->
            if (pkg !in excludePackages && pkg in launcherApps) totalToday += time
        }

        val globalHistory = dbUsage.filter { it.packageName == "TOTAL" }
        val oldestHistoryDate = globalHistory.map { it.date }.minOrNull()

        var pastStreak = 0
        var foundDefiniteFailure = false
        val c = Calendar.getInstance()
        val globalStreakLoopLimit = (prefs.globalCurrentStreak + 30).coerceAtMost(90)
        for (i in 1..globalStreakLoopLimit) {
            c.timeInMillis = todayStart; c.add(Calendar.DAY_OF_YEAR, -i)
            val dStr = dateFormat.format(c.time)
            var usage = globalHistory.find { it.date == dStr }?.usageTimeMillis

            if (usage == null) {
                if (oldestHistoryDate != null && dStr >= oldestHistoryDate) {
                    usage = 0L
                    } else if (i <= 14) {
                        usage = fetchSystemTotalUsageForDate(usageStatsManager, c.timeInMillis, launcherApps, excludePackages)
                    }
                }

                if (usage != null) {
                    if (usage <= targetMillis) pastStreak++
                    else { foundDefiniteFailure = true; break }
                } else break
            }

            val lastUpdateDayStart = Calendar.getInstance().apply {
                timeInMillis = prefs.globalLastStreakUpdateTimestamp
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val isLastUpdateYesterday = lastUpdateDayStart == todayStart - 86400000L
            val isLastUpdateToday = lastUpdateDayStart == todayStart

            val isSuccessToday = totalToday <= targetMillis
            val liveStreak = if (isSuccessToday) {
                if (!foundDefiniteFailure && (isLastUpdateYesterday || isLastUpdateToday)) {
                    maxOf(pastStreak + 1, prefs.globalCurrentStreak + (if (isLastUpdateYesterday) 1 else 0))
            } else {
                pastStreak + 1
            }
        } else 0

        var bestStreak = prefs.globalBestStreak
        var tempStreak = 0
        val todayStr = dateFormat.format(Date(todayStart))
        val startDateStr = oldestHistoryDate ?: todayStr
        val calendarForBest = Calendar.getInstance()
        try {
            val startD = dateFormat.parse(startDateStr) ?: Date()
            calendarForBest.time = startD
            val todayDate = dateFormat.parse(todayStr) ?: Date()
            
            while (!calendarForBest.time.after(todayDate)) {
                val dStr = dateFormat.format(calendarForBest.time)
                val usage = if (dStr == todayStr) totalToday else globalHistory.find { it.date == dStr }?.usageTimeMillis
                
                val effectiveUsage = usage ?: 0L
                if (effectiveUsage <= targetMillis) {
                    tempStreak++
                    bestStreak = maxOf(bestStreak, tempStreak)
                } else {
                    tempStreak = 0
                }
                calendarForBest.add(Calendar.DAY_OF_YEAR, 1)
            }
        } catch (_: Exception) {}

        bestStreak = maxOf(bestStreak, liveStreak)
        updateGlobalStreak(liveStreak, bestStreak, now)
        return Pair(liveStreak, bestStreak)
    }

    suspend fun refreshAllAppStreaks(shieldRepository: ShieldRepository) {
        shieldRepository.isShieldsLoaded.first { it }
        val shields = shieldRepository.allShields.first()
        val allUsage = shieldRepository.getAllUsage().first().groupBy { it.packageName }
        val now = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = dateFormat.format(Date(now))

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val todayUsageMap = com.etrisad.zenith.util.ScreenUsageHelper.fetchDetailedUsageToday(usageStatsManager).appUsageMap

        shields.forEach { shield ->
            val pkg = shield.packageName
            val history = allUsage[pkg] ?: emptyList()
            val oldestHistoryDate = history.map { it.date }.minOrNull()
            val limitMillis = shield.timeLimitMinutes * 60 * 1000L

            if (limitMillis <= 0 && shield.type == FocusType.SHIELD) {
                shieldRepository.updateShield(shield.copy(currentStreak = 0, bestStreak = 0))
                return@forEach
            }

            val todayUsage = todayUsageMap[pkg] ?: 0L

            var pastStreak = 0
            var foundDefiniteFailure = false
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val c = Calendar.getInstance()
            val shieldStreakLimit = (shield.currentStreak + 30).coerceAtMost(90)
            for (i in 1..shieldStreakLimit) {
                c.timeInMillis = todayStart; c.add(Calendar.DAY_OF_YEAR, -i)
                val dStr = dateFormat.format(c.time)
                var usage = history.find { it.date == dStr }?.usageTimeMillis

                if (usage == null) {
                    if (oldestHistoryDate != null && dStr >= oldestHistoryDate) {
                        if (shield.type == FocusType.SHIELD) usage = 0L else break
                    } else if (i <= 14) {
                        usage = fetchSystemAppUsageForDate(usageStatsManager, pkg, c.timeInMillis)
                    }
                }

                if (usage != null) {
                    val success = if (shield.type == FocusType.GOAL) usage >= limitMillis else usage <= limitMillis
                    if (success) pastStreak++ else { foundDefiniteFailure = true; break }
                } else break
            }

            val isSuccessToday = if (shield.type == FocusType.GOAL) todayUsage >= limitMillis else todayUsage <= limitMillis

            val lastUpdateDayStart = Calendar.getInstance().apply {
                timeInMillis = shield.lastStreakUpdateTimestamp
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val isLastUpdateYesterday = lastUpdateDayStart == todayStart - 86400000L
            val isLastUpdateToday = lastUpdateDayStart == todayStart

            val currentStreak = if (shield.type == FocusType.GOAL) {
                if (isSuccessToday) {
                    if (!foundDefiniteFailure && (isLastUpdateYesterday || isLastUpdateToday)) {
                        maxOf(pastStreak + 1, shield.currentStreak + (if (isLastUpdateYesterday) 1 else 0))
                    } else pastStreak + 1
                } else pastStreak
            } else {
                if (isSuccessToday) {
                    if (!foundDefiniteFailure && (isLastUpdateYesterday || isLastUpdateToday)) {
                        maxOf(pastStreak + 1, shield.currentStreak + (if (isLastUpdateYesterday) 1 else 0))
                    } else pastStreak + 1
                } else 0
            }

            var bestStreak = shield.bestStreak
            var tempStreak = 0
            val calendarForBest = Calendar.getInstance()
            try {
                val startDateStr = oldestHistoryDate ?: todayStr
                val startD = dateFormat.parse(startDateStr) ?: Date()
                calendarForBest.time = startD
                val todayDate = dateFormat.parse(todayStr) ?: Date()

                while (!calendarForBest.time.after(todayDate)) {
                    val dStr = dateFormat.format(calendarForBest.time)
                    val usage = if (dStr == todayStr) todayUsage else history.find { it.date == dStr }?.usageTimeMillis
                    
                    val effectiveUsage = if (usage == null && shield.type == FocusType.SHIELD) 0L else usage
                    if (effectiveUsage != null) {
                        val success = if (shield.type == FocusType.GOAL) effectiveUsage >= limitMillis else effectiveUsage <= limitMillis
                        if (success) {
                            tempStreak++
                            bestStreak = maxOf(bestStreak, tempStreak)
                        } else {
                            tempStreak = 0
                        }
                    } else {
                        tempStreak = 0
                    }
                    calendarForBest.add(Calendar.DAY_OF_YEAR, 1)
                }
            } catch (_: Exception) {}

            shieldRepository.updateShield(shield.copy(
                currentStreak = currentStreak,
                bestStreak = maxOf(bestStreak, currentStreak),
                remainingTimeMillis = if (shield.type == FocusType.GOAL) (limitMillis - todayUsage).coerceAtLeast(0L) else shield.remainingTimeMillis,
                lastStreakUpdateTimestamp = if (isSuccessToday && (shield.type == FocusType.GOAL || todayUsage > 0)) now else shield.lastStreakUpdateTimestamp
            ))
        }
    }

    suspend fun refreshBedtimeStreak(): Pair<Int, Int> {
        val prefs = userPreferencesFlow.first()
        if (!prefs.bedtimeEnabled) return Pair(0, prefs.bedtimeBestStreak)

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        val startH = try { prefs.bedtimeStartTime.split(":")[0].toInt() } catch(_: Exception) { 22 }
        val startM = try { prefs.bedtimeStartTime.split(":")[1].toInt() } catch(_: Exception) { 0 }
        val endH = try { prefs.bedtimeEndTime.split(":")[0].toInt() } catch(_: Exception) { 7 }
        val endM = try { prefs.bedtimeEndTime.split(":")[1].toInt() } catch(_: Exception) { 0 }

        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val launcherPackage = try {
            context.packageManager.resolveActivity(launcherIntent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
        } catch (_: Exception) { null }

        val launcherApps = context.packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0
        ).map { it.activityInfo.packageName }.toSet()

        val excludePackages = setOfNotNull(context.packageName, launcherPackage) + prefs.whitelistedPackages + prefs.bedtimeWhitelistedPackages

        var liveStreak = 0
        var currentBest = prefs.bedtimeBestStreak

        val bedtimeLoopLimit = (prefs.bedtimeBestStreak + 15).coerceAtMost(30)
        for (i in 0..bedtimeLoopLimit) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -i)

            if (prefs.bedtimeStreakResetDate.isNotEmpty()) {
                val dayStr = sdf.format(cal.time)
                if (dayStr < prefs.bedtimeStreakResetDate) break
            }

            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)

            if (dayOfWeek !in prefs.bedtimeDays) continue

            val startCal = Calendar.getInstance().apply {
                timeInMillis = cal.timeInMillis
                set(Calendar.HOUR_OF_DAY, startH)
                set(Calendar.MINUTE, startM)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val endCal = Calendar.getInstance().apply {
                timeInMillis = startCal.timeInMillis
                if (endH < startH || (endH == startH && endM <= startM)) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
                set(Calendar.HOUR_OF_DAY, endH)
                set(Calendar.MINUTE, endM)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val startTime = startCal.timeInMillis
            val endTime = endCal.timeInMillis

            if (startTime > now) continue

            val isSessionCompleted = endTime <= now
            val actualEnd = if (isSessionCompleted) endTime else now
            if (actualEnd <= startTime) continue

            val totalDuration = endTime - startTime
            val targetMillis = (totalDuration * 0.1).toLong()

            val events = try {
                usageStatsManager.queryEvents(startTime - 30 * 60 * 1000L, actualEnd)
            } catch (e: Exception) {
                null
            }
            val event = android.app.usage.UsageEvents.Event()
            val usageMap = mutableMapOf<String, Long>()
            var activePkg: String? = null
            var activeStartTime = startTime

            while (events?.hasNextEvent() == true) {
                events.getNextEvent(event)
                val pkg = event.packageName
                val time = event.timeStamp

                if (time < startTime) {
                    when (event.eventType) {
                        android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED,
                        android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                            activePkg = pkg
                            activeStartTime = startTime
                        }
                        android.app.usage.UsageEvents.Event.ACTIVITY_PAUSED,
                        android.app.usage.UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                            activePkg = null
                        }
                    }
                    continue
                }

                when (event.eventType) {
                    android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED,
                    android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                        if (activePkg != null) {
                            val duration = time - activeStartTime
                            if (duration > 0) usageMap[activePkg!!] = (usageMap[activePkg!!] ?: 0L) + duration
                        }
                        activePkg = pkg
                        activeStartTime = time
                    }
                    android.app.usage.UsageEvents.Event.ACTIVITY_PAUSED,
                    android.app.usage.UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                        if (activePkg == pkg) {
                            val duration = time - activeStartTime
                            if (duration > 0) usageMap[pkg] = (usageMap[pkg] ?: 0L) + duration
                            activePkg = null
                        }
                    }
                }
            }
            activePkg?.let { pkg ->
                val duration = actualEnd - activeStartTime
                if (duration > 0) usageMap[pkg] = (usageMap[pkg] ?: 0L) + duration
            }

            var usage = 0L
            usageMap.forEach { (pkg, time) ->
                if (pkg !in excludePackages && pkg in launcherApps) {
                    usage += time
                }
            }

            if (usage > targetMillis) {
                break
            } else if (isSessionCompleted) {
                liveStreak++
            }
        }

        currentBest = maxOf(currentBest, liveStreak)
        updateBedtimeStreak(liveStreak, currentBest)
        return Pair(liveStreak, currentBest)
    }

    suspend fun setAutoBackupEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.AUTO_BACKUP_ENABLED] = enabled }
    }

    suspend fun setBackupDirectoryUri(uri: String) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.BACKUP_DIRECTORY_URI] = uri }
    }

    suspend fun setBackupIntervalHours(hours: Int) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.BACKUP_INTERVAL_HOURS] = hours }
    }

    suspend fun setLastBackupTimestamp(timestamp: Long) {
        context.runtimeDataStore.edit { preferences -> preferences[RuntimeKeys.LAST_BACKUP_TIMESTAMP] = timestamp }
    }

    suspend fun setFloatingTabBarEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.FLOATING_TAB_BAR_ENABLED] = enabled }
    }

    suspend fun setExpressiveColors(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.EXPRESSIVE_COLORS] = enabled }
    }

    suspend fun setTotalUsagePillEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.TOTAL_USAGE_PILL_ENABLED] = enabled }
    }

    suspend fun setLastKnownDailyUsage(usage: Long, date: String) {
        context.runtimeDataStore.edit { preferences -> preferences[RuntimeKeys.LAST_KNOWN_DAILY_USAGE] = usage; preferences[RuntimeKeys.LAST_KNOWN_DAILY_USAGE_DATE] = date }
    }

    suspend fun setBedtimeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.BEDTIME_ENABLED] = enabled }
    }

    suspend fun setBedtimeStartTime(time: String) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.BEDTIME_START_TIME] = time }
    }

    suspend fun setBedtimeEndTime(time: String) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.BEDTIME_END_TIME] = time }
    }

    suspend fun setBedtimeDays(days: Set<Int>) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.BEDTIME_DAYS] = days.joinToString(",") }
    }

    suspend fun setBedtimeDndEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.BEDTIME_DND_ENABLED] = enabled }
    }

    suspend fun setBedtimeWindDownEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.BEDTIME_WIND_DOWN_ENABLED] = enabled }
    }

    suspend fun setBedtimeNotificationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.BEDTIME_NOTIFICATION_ENABLED] = enabled }
    }

    suspend fun setBedtimeWhitelistedPackages(packages: Set<String>) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.BEDTIME_WHITELISTED_PACKAGES] = packages.joinToString(",") }
    }

    suspend fun updateBedtimeStreak(current: Int, best: Int) {
        context.runtimeDataStore.edit { preferences ->
            preferences[RuntimeKeys.BEDTIME_CURRENT_STREAK] = current
            preferences[RuntimeKeys.BEDTIME_BEST_STREAK] = best
        }
    }

    suspend fun resetBedtimeStreak() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(System.currentTimeMillis())
        context.runtimeDataStore.edit { preferences ->
            preferences[RuntimeKeys.BEDTIME_CURRENT_STREAK] = 0
            preferences[RuntimeKeys.BEDTIME_BEST_STREAK] = 0
            preferences[RuntimeKeys.BEDTIME_STREAK_RESET_DATE] = today
        }
    }

    suspend fun setEarlyKickEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.EARLY_KICK_ENABLED] = enabled }
    }

    suspend fun setInterceptAudioFocusEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.INTERCEPT_AUDIO_FOCUS_ENABLED] = enabled }
    }

    suspend fun setShowDatabaseIndicator(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.SHOW_DATABASE_INDICATOR] = enabled }
    }

    suspend fun setDeveloperModeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.DEVELOPER_MODE_ENABLED] = enabled }
    }

    suspend fun setLastSyncTimestamp(timestamp: Long) {
        context.runtimeDataStore.edit { preferences -> preferences[RuntimeKeys.LAST_SYNC_TIMESTAMP] = timestamp }
    }

    suspend fun setPreferSystemUsageHistory(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.PREFER_SYSTEM_USAGE_HISTORY] = enabled }
    }

    suspend fun setOnboardingStatsCompleted(completed: Boolean) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.ONBOARDING_STATS_COMPLETED] = completed }
    }

    suspend fun setOnboardingUpdateCompleted(completed: Boolean) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.ONBOARDING_UPDATE_COMPLETED] = completed }
    }

    suspend fun setHudHideFeatureLearned(learned: Boolean) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.HUD_HIDE_FEATURE_LEARNED] = learned }
    }

    suspend fun setSmartRepairOnRefresh(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.SMART_REPAIR_ON_REFRESH] = enabled }
    }

    suspend fun setAllowRepairNonUnavailable(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.ALLOW_REPAIR_NON_UNAVAILABLE] = enabled }
    }

    suspend fun setShortsScreenTimeMs(ms: Long) {
        context.runtimeDataStore.edit { preferences -> preferences[RuntimeKeys.SHORTS_SCREEN_TIME_MS] = ms }
    }

    suspend fun setCustomDelayEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.CUSTOM_DELAY_ENABLED] = enabled }
    }

    suspend fun setDelayPowerSave(delay: Long) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.DELAY_POWER_SAVE] = delay }
    }

    suspend fun setDelayOverlayShowing(delay: Long) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.DELAY_OVERLAY_SHOWING] = delay }
    }

    suspend fun setDelayGoalNear(delay: Long) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.DELAY_GOAL_NEAR] = delay }
    }

    suspend fun setDelayGoalMid(delay: Long) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.DELAY_GOAL_MID] = delay }
    }

    suspend fun setDelayGoalFar(delay: Long) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.DELAY_GOAL_FAR] = delay }
    }

    suspend fun setDelayShieldVeryFar(delay: Long) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.DELAY_SHIELD_VERY_FAR] = delay }
    }

    suspend fun setDelayShieldFar(delay: Long) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.DELAY_SHIELD_FAR] = delay }
    }

    suspend fun setDelayShieldMid(delay: Long) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.DELAY_SHIELD_MID] = delay }
    }

    suspend fun setDelayShieldNear(delay: Long) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.DELAY_SHIELD_NEAR] = delay }
    }

    suspend fun setDelayDefault(delay: Long) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.DELAY_DEFAULT] = delay }
    }

    suspend fun setMindfulGatewayEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.MINDFUL_GATEWAY_ENABLED] = enabled }
    }

    suspend fun setRefreshOnOpenUsageStats(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.REFRESH_ON_OPEN_USAGE_STATS] = enabled }
    }

    suspend fun setCheckUpdateOnStart(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.CHECK_UPDATE_ON_START] = enabled }
    }

    suspend fun setBatteryStatsResetEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.BATTERY_STATS_RESET_ENABLED] = enabled }
    }

    suspend fun setPerformanceLevel(level: PerformanceLevel) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PERFORMANCE_LEVEL] = level.name
        }
    }

    suspend fun applyPerformanceSettings(
        perfA11yActiveDelay: Long,
        perfA11yInactiveDelay: Long,
        perfScreenOffDelay: Long,
        perfPowerSaveDelay: Long,
        perfUsageStatsCacheMs: Long,
        perfShieldDbWriteMs: Long,
        perfShieldDbWriteNearMs: Long,
        perfLauncherCacheMs: Long,
    ) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.PERF_A11Y_ACTIVE_DELAY] = perfA11yActiveDelay
            prefs[PreferencesKeys.PERF_A11Y_INACTIVE_DELAY] = perfA11yInactiveDelay
            prefs[PreferencesKeys.PERF_SCREEN_OFF_DELAY] = perfScreenOffDelay
            prefs[PreferencesKeys.PERF_POWER_SAVE_DELAY] = perfPowerSaveDelay
            prefs[PreferencesKeys.PERF_USAGE_STATS_CACHE] = perfUsageStatsCacheMs
            prefs[PreferencesKeys.PERF_SHIELD_DB_WRITE] = perfShieldDbWriteMs
            prefs[PreferencesKeys.PERF_SHIELD_DB_WRITE_NEAR] = perfShieldDbWriteNearMs
            prefs[PreferencesKeys.PERF_LAUNCHER_CACHE] = perfLauncherCacheMs
        }
    }

    suspend fun updateLastChargeTimestamp(timestamp: Long) {
        context.runtimeDataStore.edit { preferences -> preferences[RuntimeKeys.LAST_CHARGE_TIMESTAMP] = timestamp }
    }

    suspend fun resetAppStats(packageName: String) {
        context.runtimeDataStore.edit { preferences ->
            val currentMap = preferences[RuntimeKeys.MANUAL_RESET_TIMESTAMPS]?.split(",")
                ?.filter { it.contains(":") }
                ?.associate { 
                    val parts = it.split(":")
                    parts[0] to parts[1]
                }                ?.toMutableMap() ?: hashMapOf()
            
            currentMap[packageName] = System.currentTimeMillis().toString()
            preferences[RuntimeKeys.MANUAL_RESET_TIMESTAMPS] = currentMap.entries.joinToString(",") { "${it.key}:${it.value}" }
        }
    }

    suspend fun setGSFlexSettings(settings: GSFlexSettings) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.GS_FLEX_PRESET] = settings.preset.name
            preferences[PreferencesKeys.GS_D_WGHT] = settings.display.weight
            preferences[PreferencesKeys.GS_D_WDTH] = settings.display.width
            preferences[PreferencesKeys.GS_D_OPSZ] = settings.display.opsz
            preferences[PreferencesKeys.GS_D_GRAD] = settings.display.grade
            preferences[PreferencesKeys.GS_D_SLNT] = settings.display.slant
            preferences[PreferencesKeys.GS_D_ROND] = settings.display.roundness
            preferences[PreferencesKeys.GS_H_WGHT] = settings.headline.weight
            preferences[PreferencesKeys.GS_H_WDTH] = settings.headline.width
            preferences[PreferencesKeys.GS_H_OPSZ] = settings.headline.opsz
            preferences[PreferencesKeys.GS_H_GRAD] = settings.headline.grade
            preferences[PreferencesKeys.GS_H_SLNT] = settings.headline.slant
            preferences[PreferencesKeys.GS_H_ROND] = settings.headline.roundness
            preferences[PreferencesKeys.GS_B_WGHT] = settings.body.weight
            preferences[PreferencesKeys.GS_B_WDTH] = settings.body.width
            preferences[PreferencesKeys.GS_B_OPSZ] = settings.body.opsz
            preferences[PreferencesKeys.GS_B_GRAD] = settings.body.grade
            preferences[PreferencesKeys.GS_B_SLNT] = settings.body.slant
            preferences[PreferencesKeys.GS_B_ROND] = settings.body.roundness
        }
    }

    suspend fun runManualStreakRecovery(shieldRepository: ShieldRepository) {
        val prefs = userPreferencesFlow.first()
        val targetMillis = prefs.screenTimeTargetMinutes * 60 * 1000L
        val dbUsage = shieldRepository.getAllUsage().first()
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
        val todayStart = calendar.timeInMillis
        if (targetMillis > 0) {
            val (launcherApps, launcherPackage) = try {
                val pm = context.packageManager
                val apps = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0).map { it.activityInfo.packageName }.toSet()
                val lPkg = pm.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
                apps to lPkg
            } catch (_: Exception) { emptySet<String>() to null }
            val excludePackages = setOfNotNull(context.packageName, launcherPackage)

            val globalHistory = dbUsage.filter { it.packageName == "TOTAL" }
            val oldestHistoryDate = globalHistory.map { it.date }.minOrNull()
            var pastStreak = 0
            val recoveryGlobalLimit = (prefs.globalCurrentStreak + 30).coerceAtMost(90)
            for (i in 1..recoveryGlobalLimit) {
                val c = Calendar.getInstance().apply { timeInMillis = todayStart; add(Calendar.DAY_OF_YEAR, -i) }
                val dStr = dateFormat.format(c.time)
                var usage = globalHistory.find { it.date == dStr }?.usageTimeMillis
                if (usage == null) {
                    if (oldestHistoryDate != null && dStr >= oldestHistoryDate) {
                        usage = 0L
                    } else if (i <= 14) {
                        usage = fetchSystemTotalUsageForDate(usageStatsManager, c.timeInMillis, launcherApps, excludePackages)
                    }
                }

                if (usage != null) {
                    if (usage <= targetMillis) pastStreak++ else break
                } else break
            }

            val stats = com.etrisad.zenith.util.ScreenUsageHelper.fetchDetailedUsageToday(usageStatsManager).appUsageMap
            var totalToday = 0L
            stats.forEach { (pkg, time) -> if (pkg !in excludePackages && pkg in launcherApps) totalToday += time }

            val isSuccessToday = totalToday <= targetMillis
            if (isSuccessToday && (pastStreak + 1) < prefs.globalBestStreak) {
                var provenDays = 1
                for (j in 1..3) {
                    val cal = Calendar.getInstance().apply { timeInMillis = todayStart; add(Calendar.DAY_OF_YEAR, -j) }
                    val u = globalHistory.find { it.date == dateFormat.format(cal.time) }?.usageTimeMillis
                        ?: fetchSystemTotalUsageForDate(usageStatsManager, cal.timeInMillis, launcherApps, excludePackages)
                    if (u <= targetMillis) provenDays++ else break
                }
                if (provenDays >= 3) {
                    updateGlobalStreak(maxOf(pastStreak + 1, prefs.globalBestStreak), prefs.globalBestStreak, System.currentTimeMillis())
                }
            }
        }
        val shields = shieldRepository.allShields.first()
        val allUsage = dbUsage.groupBy { it.packageName }
        val todayUsageMap = com.etrisad.zenith.util.ScreenUsageHelper.fetchDetailedUsageToday(usageStatsManager).appUsageMap

        shields.forEach { shield ->
            val pkg = shield.packageName
            val history = allUsage[pkg] ?: emptyList()
            val oldestHistoryDate = history.map { it.date }.minOrNull()
            val limitMillis = shield.timeLimitMinutes * 60 * 1000L
            if (limitMillis <= 0 && shield.type == FocusType.SHIELD) return@forEach

            var pastStreak = 0
            val recoveryShieldLimit = (shield.currentStreak + 30).coerceAtMost(90)
            for (i in 1..recoveryShieldLimit) {
                val c = Calendar.getInstance().apply { timeInMillis = todayStart; add(Calendar.DAY_OF_YEAR, -i) }
                val dStr = dateFormat.format(c.time)
                var usage = history.find { it.date == dStr }?.usageTimeMillis
                if (usage == null) {
                    if (oldestHistoryDate != null && dStr >= oldestHistoryDate) {
                        if (shield.type == FocusType.SHIELD) usage = 0L else break
                    } else if (i <= 14) {
                        usage = fetchSystemAppUsageForDate(usageStatsManager, pkg, c.timeInMillis)
                    }
                }

                if (usage != null) {
                    val success = if (shield.type == FocusType.GOAL) usage >= limitMillis else usage <= limitMillis
                    if (success) pastStreak++ else break
                } else break
            }

            val todayUsage = todayUsageMap[pkg] ?: 0L
            val isSuccessToday = if (shield.type == FocusType.GOAL) todayUsage >= limitMillis else todayUsage <= limitMillis
            val currentStreak = if (shield.type == FocusType.GOAL) (if (isSuccessToday) pastStreak + 1 else pastStreak) else (if (isSuccessToday) pastStreak + 1 else 0)

            if (isSuccessToday && currentStreak < shield.bestStreak) {
                var provenDays = 1
                for (j in 1..3) {
                    val cal = Calendar.getInstance().apply { timeInMillis = todayStart; add(Calendar.DAY_OF_YEAR, -j) }
                    val u = history.find { it.date == dateFormat.format(cal.time) }?.usageTimeMillis
                        ?: fetchSystemAppUsageForDate(usageStatsManager, pkg, cal.timeInMillis)
                    val success = if (shield.type == FocusType.GOAL) u >= limitMillis else u <= limitMillis
                    if (success) provenDays++ else break
                }
                if (provenDays >= 3) {
                    shieldRepository.updateShield(shield.copy(currentStreak = maxOf(currentStreak, shield.bestStreak)))
                }
            }
        }
    }

    suspend fun setStreakRecoveryPerformed(performed: Boolean) {
        context.runtimeDataStore.edit { preferences -> preferences[RuntimeKeys.STREAK_RECOVERY_PERFORMED] = performed }
    }

    private fun fetchSystemTotalUsageForDate(
        usm: UsageStatsManager,
        startTime: Long,
        launcherApps: Set<String>,
        excludePackages: Set<String>
    ): Long {
        val stats = try {
            usm.queryAndAggregateUsageStats(startTime, startTime + 86400000L)
        } catch (e: Exception) {
            null
        }
        var total = 0L
        stats?.forEach { (pkg, stat) ->
            if (pkg in launcherApps && pkg !in excludePackages) {
                total += stat.totalTimeVisible.coerceAtLeast(stat.totalTimeInForeground)
            }
        }
        return total
    }

    private fun fetchSystemAppUsageForDate(
        usm: UsageStatsManager,
        packageName: String,
        startTime: Long
    ): Long {
        val stats = try {
            usm.queryAndAggregateUsageStats(startTime, startTime + 86400000L)
        } catch (e: Exception) {
            null
        }
        val stat = stats?.get(packageName) ?: return 0L
        return stat.totalTimeVisible.coerceAtLeast(stat.totalTimeInForeground)
    }

    suspend fun resetCustomDelays() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.DELAY_POWER_SAVE); preferences.remove(PreferencesKeys.DELAY_OVERLAY_SHOWING); preferences.remove(PreferencesKeys.DELAY_GOAL_NEAR)
            preferences.remove(PreferencesKeys.DELAY_GOAL_MID); preferences.remove(PreferencesKeys.DELAY_GOAL_FAR); preferences.remove(PreferencesKeys.DELAY_SHIELD_VERY_FAR)
            preferences.remove(PreferencesKeys.DELAY_SHIELD_FAR); preferences.remove(PreferencesKeys.DELAY_SHIELD_MID); preferences.remove(PreferencesKeys.DELAY_SHIELD_NEAR)
            preferences.remove(PreferencesKeys.DELAY_DEFAULT)
        }
    }
}

data class UserPreferences(
    val themeConfig: ThemeConfig = ThemeConfig.FOLLOW_SYSTEM,
    val fontOption: FontOption = FontOption.GOOGLE_SANS_FLEX,
    val dynamicColor: Boolean = true,
    val accessibilityDisabled: Boolean = false,
    val screenTimeTargetMinutes: Int = 0,
    val emergencyRechargeDurationMinutes: Int = 60,
    val delayAppDurationSeconds: Int = 30,
    val sessionUsageOverlayEnabled: Boolean = false,
    val sessionUsageOverlaySize: Int = 100,
    val sessionUsageOverlayOpacity: Int = 90,
    val whitelistedPackages: Set<String> = emptySet(),
    val lastResetDate: String = "",
    val lastStreakCheckDate: String = "",
    val globalCurrentStreak: Int = 0,
    val globalBestStreak: Int = 0,
    val globalLastStreakUpdateTimestamp: Long = 0L,
    val autoBackupEnabled: Boolean = false,
    val backupDirectoryUri: String = "",
    val backupIntervalHours: Int = 3,
    val lastBackupTimestamp: Long = 0L,
    val floatingTabBarEnabled: Boolean = false,
    val expressiveColors: Boolean = false,
    val totalUsagePillEnabled: Boolean = false,
    val lastKnownDailyUsage: Long = 0L,
    val lastKnownDailyUsageDate: String = "",
    val bedtimeEnabled: Boolean = false,
    val bedtimeStartTime: String = "22:00",
    val bedtimeEndTime: String = "07:00",
    val bedtimeDays: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7),
    val bedtimeDndEnabled: Boolean = false,
    val bedtimeWindDownEnabled: Boolean = false,
    val bedtimeNotificationEnabled: Boolean = true,
    val bedtimeWhitelistedPackages: Set<String> = emptySet(),
    val bedtimeCurrentStreak: Int = 0,
    val bedtimeBestStreak: Int = 0,
    val bedtimeStreakResetDate: String = "",
    val userName: String = "User",
    val earlyKickEnabled: Boolean = false,
    val interceptAudioFocusEnabled: Boolean = true,
    val showDatabaseIndicator: Boolean = false,
    val developerModeEnabled: Boolean = false,
    val lastSyncTimestamp: Long = 0L,
    val preferSystemUsageHistory: Boolean = true,
    val onboardingStatsCompleted: Boolean = false,
    val onboardingUpdateCompleted: Boolean = false,
    val whitelistInitialized: Boolean = false,
    val hudHideFeatureLearned: Boolean = false,
    val smartRepairOnRefresh: Boolean = false,
    val allowRepairNonUnavailable: Boolean = false,
    val shortsScreenTimeMs: Long = 0L,
    val customDelayEnabled: Boolean = false,
    val delayPowerSave: Long = 5000L,
    val delayOverlayShowing: Long = 8000L,
    val delayGoalNear: Long = 600L,
    val delayGoalMid: Long = 1200L,
    val delayGoalFar: Long = 1800L,
    val delayShieldVeryFar: Long = 5000L,
    val delayShieldFar: Long = 3000L,
    val delayShieldMid: Long = 1500L,
    val delayShieldNear: Long = 600L,
    val delayDefault: Long = 1200L,
    val mindfulGatewayEnabled: Boolean = false,
    val refreshOnOpenUsageStats: Boolean = false,
    val checkUpdateOnStart: Boolean = false,
    val batteryStatsResetEnabled: Boolean = false,
    val performanceLevel: PerformanceLevel = PerformanceLevel.BALANCED,
    val perfA11yActiveDelay: Long = PerformanceConfig().a11yActiveDelay,
    val perfA11yInactiveDelay: Long = PerformanceConfig().a11yInactiveDelay,
    val perfScreenOffDelay: Long = PerformanceConfig().screenOffDelay,
    val perfPowerSaveDelay: Long = PerformanceConfig().powerSaveDelay,
    val perfUsageStatsCacheMs: Long = PerformanceConfig().usageStatsCacheMs,
    val perfShieldDbWriteMs: Long = PerformanceConfig().shieldDbWriteMs,
    val perfShieldDbWriteNearMs: Long = PerformanceConfig().shieldDbWriteNearMs,
    val perfLauncherCacheMs: Long = PerformanceConfig().launcherCacheMs,
    val perfGoalReminderTick: Long = PerformanceConfig().goalReminderTick,
    val perfDayChangeTick: Long = PerformanceConfig().dayChangeTick,
    val lastChargeTimestamp: Long = 0L,
    val manualResetTimestamps: Map<String, Long> = emptyMap(),
    val gsFlexSettings: GSFlexSettings = GSFlexSettings(),
    val streakRecoveryPerformed: Boolean = false
) {
    fun buildPerformanceConfig(): PerformanceConfig {
        if (performanceLevel.isPreset()) return performanceLevel.toConfig()
        return PerformanceConfig(
            a11yActiveDelay = perfA11yActiveDelay,
            a11yInactiveDelay = perfA11yInactiveDelay,
            screenOffDelay = perfScreenOffDelay,
            powerSaveDelay = perfPowerSaveDelay,
            usageStatsCacheMs = perfUsageStatsCacheMs,
            shieldDbWriteMs = perfShieldDbWriteMs,
            shieldDbWriteNearMs = perfShieldDbWriteNearMs,
            launcherCacheMs = perfLauncherCacheMs,
            goalReminderTick = perfGoalReminderTick,
            dayChangeTick = perfDayChangeTick,
        )
    }
}

fun PerformanceConfig.detectPreset(): PerformanceLevel {
    PerformanceLevel.entries.forEach { level ->
        if (level.isPreset() && this == level.toConfig()) return level
    }
    return PerformanceLevel.CUSTOM
}