package com.etrisad.zenith.ui.screens.settings

import android.widget.Toast
import android.app.WallpaperManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.etrisad.zenith.R
import com.etrisad.zenith.data.preferences.ForegroundNotificationStatusMode
import com.etrisad.zenith.data.preferences.UserPreferences
import com.etrisad.zenith.util.hasCalendarPermission
import com.etrisad.zenith.ui.components.ZenithButtonSize
import com.etrisad.zenith.ui.components.ZenithToggleButtonGroup
import com.etrisad.zenith.ui.components.ZenithToggleOption
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.etrisad.zenith.R
import androidx.compose.ui.res.stringResource

@Composable
fun FeaturesSettings(
    preferences: UserPreferences,
    onTotalUsagePillEnabledChange: (Boolean) -> Unit,
    onForegroundNotificationStatusModeChange: (ForegroundNotificationStatusMode) -> Unit,
    onSessionUsageOverlayEnabledChange: (Boolean) -> Unit,
    onSessionUsageOverlaySizeChange: (Int) -> Unit,
    onSessionUsageOverlayOpacityChange: (Int) -> Unit,
    onMindfulGatewayEnabledChange: (Boolean) -> Unit,
    onEarlyKickEnabledChange: (Boolean) -> Unit,
    onInterceptAudioFocusEnabledChange: (Boolean) -> Unit,
    onBatteryStatsResetEnabledChange: (Boolean) -> Unit,
    onShowCurrentEventEnabledChange: (Boolean) -> Unit,
    onDailyRecapEnabledChange: (Boolean) -> Unit,
    onWeeklyInsightEnabledChange: (Boolean) -> Unit,
    onTrendMilestoneEnabledChange: (Boolean) -> Unit,
    onIncentiveLockEnabledChange: (Boolean) -> Unit,
    onIncentiveLockDisableRequest: () -> Unit,
    onIncentiveLockCancelDisableRequest: () -> Unit,
    onNavigateToGracePeriod: () -> Unit,
    goalCount: Int
) {
    val context = LocalContext.current
    val calendarPermissionGranted = remember { hasCalendarPermission(context) }
    var showConfirmSheet by remember { mutableStateOf(false) }

    if (showConfirmSheet) {
        com.etrisad.zenith.ui.components.ConfirmBottomSheet(
            onDismiss = { showConfirmSheet = false },
            onConfirm = {
                onIncentiveLockDisableRequest()
                showConfirmSheet = false
            },
            leverCount = 10,
            showTimeSelection = false
        )
    }

    Column {
        PreferenceCategory(title = stringResource(R.string.settings_interface_overlays))

        SettingsToggle(
            title = stringResource(R.string.total_usage_pill),
            description = stringResource(R.string.total_usage_pill_desc),
            checked = preferences.totalUsagePillEnabled,
            onCheckedChange = onTotalUsagePillEnabledChange,
            icon = Icons.Outlined.Public,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))
        SettingsToggle(
            title = stringResource(R.string.session_usage_overlay),
            description = stringResource(R.string.session_usage_overlay_desc),
            checked = preferences.sessionUsageOverlayEnabled,
            onCheckedChange = onSessionUsageOverlayEnabledChange,
            icon = Icons.Outlined.Timer,
            shape = RoundedCornerShape(8.dp)
        )

        AnimatedVisibility(
            visible = preferences.sessionUsageOverlayEnabled,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                Spacer(modifier = Modifier.height(4.dp))
                HUDAppearanceSettings(
                    size = preferences.sessionUsageOverlaySize,
                    opacity = preferences.sessionUsageOverlayOpacity,
                    onSizeChange = onSessionUsageOverlaySizeChange,
                    onOpacityChange = onSessionUsageOverlayOpacityChange,
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        SettingsToggle(
            title = stringResource(R.string.show_current_event),
            description = if (calendarPermissionGranted) stringResource(R.string.show_current_event_desc) else stringResource(R.string.show_current_event_desc_no_permission),
            checked = preferences.showCurrentEvent,
            onCheckedChange = { enabled ->
                if (calendarPermissionGranted) {
                    onShowCurrentEventEnabledChange(enabled)
                } else {
                    Toast.makeText(context, context.getString(R.string.show_current_event_no_permission), Toast.LENGTH_SHORT).show()
                }
            },
            icon = Icons.Outlined.CalendarMonth,
            enabled = calendarPermissionGranted,
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))
        PreferenceCategory(title = stringResource(R.string.settings_notifications))

        ForegroundNotificationStatusSelector(
            selectedMode = preferences.foregroundNotificationStatusMode,
            onModeChange = onForegroundNotificationStatusModeChange,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))
        SettingsToggle(
            title = stringResource(R.string.daily_focus_recap),
            description = stringResource(R.string.daily_focus_recap_desc),
            checked = preferences.dailyRecapEnabled,
            onCheckedChange = onDailyRecapEnabledChange,
            icon = Icons.Outlined.Today,
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))
        SettingsToggle(
            title = stringResource(R.string.weekly_insight),
            description = stringResource(R.string.weekly_insight_desc),
            checked = preferences.weeklyInsightEnabled,
            onCheckedChange = onWeeklyInsightEnabledChange,
            icon = Icons.Outlined.Assessment,
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))
        SettingsToggle(
            title = stringResource(R.string.trend_milestones),
            description = stringResource(R.string.trend_milestones_desc),
            checked = preferences.trendMilestoneEnabled,
            onCheckedChange = onTrendMilestoneEnabledChange,
            icon = Icons.Outlined.EmojiEvents,
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))
        PreferenceCategory(title = stringResource(R.string.settings_entry_control))

        SettingsToggle(
            title = stringResource(R.string.mindful_gateway),
            description = stringResource(R.string.mindful_gateway_desc),
            checked = preferences.mindfulGatewayEnabled,
            onCheckedChange = onMindfulGatewayEnabledChange,
            icon = Icons.Outlined.AutoFixHigh,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))
        val isDisablingIncentiveLock = preferences.incentiveLockDisableRequestTimestamp > 0

        SettingsToggle(
            title = stringResource(R.string.incentive_lock),
            description = stringResource(R.string.incentive_lock_desc),
            checked = preferences.incentiveLockEnabled,
            onCheckedChange = { enabled ->
                if (enabled) {
                    if (goalCount > 0) {
                        onIncentiveLockEnabledChange(true)
                    } else {
                        Toast.makeText(context, context.getString(R.string.incentive_lock_add_goal_first), Toast.LENGTH_SHORT).show()
                    }
                } else {
                    showConfirmSheet = true
                }
            },
            icon = Icons.Outlined.Lock,
            enabled = (preferences.incentiveLockEnabled || goalCount > 0) && !isDisablingIncentiveLock,
            shape = RoundedCornerShape(8.dp)
        )

        AnimatedVisibility(
            visible = isDisablingIncentiveLock,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            val deactivationTime = preferences.incentiveLockDisableRequestTimestamp + 3600000L
            var timeLeftMillis by remember(preferences.incentiveLockDisableRequestTimestamp) { 
                mutableLongStateOf(deactivationTime - System.currentTimeMillis()) 
            }
            
            LaunchedEffect(preferences.incentiveLockDisableRequestTimestamp) {
                if (preferences.incentiveLockDisableRequestTimestamp > 0) {
                    while (timeLeftMillis > 0) {
                        timeLeftMillis = deactivationTime - System.currentTimeMillis()
                        if (timeLeftMillis <= 0) {
                            onIncentiveLockEnabledChange(false)
                            onIncentiveLockCancelDisableRequest()
                        }
                        delay(1000)
                    }
                }
            }

            Column {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.disabling_incentive_lock),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            val minutesLeft = (timeLeftMillis / 60000).coerceAtLeast(0)
                            val secondsLeft = ((timeLeftMillis % 60000) / 1000).coerceAtLeast(0)
                            val timeStr = String.format("%02d:%02d", minutesLeft, secondsLeft)
                            val targetTimeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(deactivationTime))
                            Text(
                                "Feature will be disabled in $timeStr (around $targetTimeStr)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                            )
                        }
                        IconButton(onClick = onIncentiveLockCancelDisableRequest) {
                            Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.cancel), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        SettingsActionItem(
            title = stringResource(R.string.grace_period),
            summary = stringResource(R.string.grace_period_desc),
            onClick = onNavigateToGracePeriod,
            icon = Icons.Outlined.FreeBreakfast,
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))
        PreferenceCategory(title = stringResource(R.string.settings_triggers))

        SettingsToggle(
            title = stringResource(R.string.pause_media),
            description = stringResource(R.string.pause_media_desc),
            checked = preferences.interceptAudioFocusEnabled,
            onCheckedChange = onInterceptAudioFocusEnabledChange,
            icon = Icons.Outlined.MusicNote,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))
        SettingsToggle(
            title = stringResource(R.string.early_kick),
            description = stringResource(R.string.early_kick_desc),
            checked = preferences.earlyKickEnabled,
            onCheckedChange = onEarlyKickEnabledChange,
            icon = Icons.Outlined.ExitToApp,
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))
        SettingsToggle(
            title = stringResource(R.string.battery_usage_reset),
            description = stringResource(R.string.battery_usage_reset_desc),
            checked = preferences.batteryStatsResetEnabled,
            onCheckedChange = onBatteryStatsResetEnabledChange,
            icon = Icons.Outlined.BatteryChargingFull,
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
        )
    }
}

@Composable
fun ForegroundNotificationStatusSelector(
    selectedMode: ForegroundNotificationStatusMode,
    onModeChange: (ForegroundNotificationStatusMode) -> Unit,
    shape: Shape = RoundedCornerShape(8.dp)
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.status_notification),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    Text(
                        text = "Choose what Zenith shows while monitoring is active",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val modeOptions = listOf(
                ForegroundNotificationStatusMode.DAILY_USAGE to "Usage",
                ForegroundNotificationStatusMode.ACTIVE_FOCUS to "Focus",
                ForegroundNotificationStatusMode.DEFAULT to "Default"
            )

            ZenithToggleButtonGroup(
                options = modeOptions.map { ZenithToggleOption(text = it.second) },
                selectedIndices = setOf(modeOptions.indexOfFirst { it.first == selectedMode }.coerceAtLeast(0)),
                onToggle = { index -> onModeChange(modeOptions[index].first) },
                size = ZenithButtonSize.Medium,
                isInsideContainer = true
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HUDAppearanceSettings(
    size: Int,
    opacity: Int,
    onSizeChange: (Int) -> Unit,
    onOpacityChange: (Int) -> Unit,
    shape: Shape = RoundedCornerShape(8.dp)
) {
    val context = LocalContext.current
    val wallpaperDrawable = remember {
        try {
            WallpaperManager.getInstance(context).drawable
        } catch (_: Exception) {
            null
        }
    }

    var localSize by remember(size) { mutableFloatStateOf(size.toFloat()) }
    var localOpacity by remember(opacity) { mutableFloatStateOf(opacity.toFloat()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.hud_appearance),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.PhotoSizeSelectSmall, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = localSize,
                    onValueChange = { localSize = it },
                    onValueChangeFinished = { onSizeChange(localSize.toInt()) },
                    valueRange = 50f..200f,
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
                )
                Icon(Icons.Outlined.PhotoSizeSelectLarge, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = stringResource(R.string.hud_size, localSize.toInt()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Opacity, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = localOpacity,
                    onValueChange = { localOpacity = it },
                    onValueChangeFinished = { onOpacityChange(localOpacity.toInt()) },
                    valueRange = 20f..100f,
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
                )
                Icon(Icons.Outlined.Contrast, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = stringResource(R.string.hud_opacity, localOpacity.toInt()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = stringResource(R.string.preview),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (wallpaperDrawable != null) {
                    androidx.compose.foundation.Image(
                        painter = rememberDrawablePainter(wallpaperDrawable),
                        contentDescription = null,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)))
                }

                val animatedScale by animateFloatAsState(
                    targetValue = localSize / 100f,
                    label = "HUDScale"
                )
                val animatedOpacity by animateFloatAsState(
                    targetValue = localOpacity / 100f,
                    label = "HUDOpacity"
                )

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .graphicsLayer {
                            scaleX = animatedScale
                            scaleY = animatedScale
                            alpha = animatedOpacity
                        }
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularWavyProgressIndicator(
                        progress = { 0.75f },
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        stroke = Stroke(width = 6.dp.value),
                        trackStroke = Stroke(width = 6.dp.value),
                        wavelength = 20.dp,
                        waveSpeed = 0.dp
                    )
                    Text(
                        text = "15m",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
