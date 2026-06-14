package com.etrisad.zenith.ui.screens.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.etrisad.zenith.data.preferences.PerformanceConfig
import com.etrisad.zenith.data.preferences.PerformanceLevel
import com.etrisad.zenith.data.preferences.UserPreferences
import com.etrisad.zenith.data.preferences.detectPreset
import com.etrisad.zenith.data.preferences.isPreset
import com.etrisad.zenith.data.preferences.toConfig
import kotlinx.coroutines.launch

@Composable
fun PerformanceSettings(
    preferences: UserPreferences,
    selectedLevel: PerformanceLevel,
    onSelectLevel: (PerformanceLevel) -> Unit,
) {
    val allLevels = PerformanceLevel.values()

    PreferenceCategory(title = "Performance Profile")

    allLevels.forEachIndexed { index, level ->
        val isSelected = preferences.performanceLevel == level
        val isChecked = selectedLevel == level
        val isCustom = level == PerformanceLevel.CUSTOM

        val shape = when {
            index == 0 -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
            isCustom && index == allLevels.lastIndex -> RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
            else -> RoundedCornerShape(8.dp)
        }

        val bgColor by animateColorAsState(
            targetValue = if (isChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else if (isCustom) MaterialTheme.colorScheme.surfaceContainerHigh
            else MaterialTheme.colorScheme.surfaceContainerLow,
            label = "preset_bg_${level.name}"
        )

        Surface(
            onClick = { onSelectLevel(level) },
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            color = bgColor,
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            if (isChecked) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceContainerHighest
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isChecked) {
                        Icon(Icons.Filled.Check, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp))
                    } else if (isCustom) {
                        Icon(Icons.Outlined.Tune, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = level.labelRes,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal)
                    if (isCustom && isSelected) {
                        Text(text = activeConfigSummary(preferences.buildPerformanceConfig()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text(text = level.descriptionRes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (isSelected) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "ACTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        if (index < allLevels.lastIndex) {
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    Text(text = "Custom profile lets you tune every parameter manually",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp))
}

@Composable
fun PerformanceTuningPanel(
    preferences: UserPreferences,
    selectedLevel: PerformanceLevel,
    onSelectedLevelChange: (PerformanceLevel) -> Unit,
    onApplyCustomSettings: (Long, Long, Long, Long, Long, Long, Long, Long) -> Unit,
    onSetPerformanceLevel: ((PerformanceLevel) -> Unit)? = null,
    onRegisterApplyAction: ((() -> Unit) -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()

    var a11yActive by remember {
        mutableStateOf(
            if (selectedLevel.isPreset()) selectedLevel.toConfig().a11yActiveDelay
            else preferences.perfA11yActiveDelay
        )
    }
    var a11yInactive by remember {
        mutableStateOf(
            if (selectedLevel.isPreset()) selectedLevel.toConfig().a11yInactiveDelay
            else preferences.perfA11yInactiveDelay
        )
    }
    var screenOff by remember {
        mutableStateOf(
            if (selectedLevel.isPreset()) selectedLevel.toConfig().screenOffDelay
            else preferences.perfScreenOffDelay
        )
    }
    var powerSave by remember {
        mutableStateOf(
            if (selectedLevel.isPreset()) selectedLevel.toConfig().powerSaveDelay
            else preferences.perfPowerSaveDelay
        )
    }
    var usageCache by remember {
        mutableStateOf(
            if (selectedLevel.isPreset()) selectedLevel.toConfig().usageStatsCacheMs
            else preferences.perfUsageStatsCacheMs
        )
    }
    var dbWrite by remember {
        mutableStateOf(
            if (selectedLevel.isPreset()) selectedLevel.toConfig().shieldDbWriteMs
            else preferences.perfShieldDbWriteMs
        )
    }
    var dbWriteNear by remember {
        mutableStateOf(
            if (selectedLevel.isPreset()) selectedLevel.toConfig().shieldDbWriteNearMs
            else preferences.perfShieldDbWriteNearMs
        )
    }
    var launcherCache by remember {
        mutableStateOf(
            if (selectedLevel.isPreset()) selectedLevel.toConfig().launcherCacheMs
            else preferences.perfLauncherCacheMs
        )
    }

    LaunchedEffect(selectedLevel) {
        if (selectedLevel.isPreset()) {
            val cfg = selectedLevel.toConfig()
            a11yActive = cfg.a11yActiveDelay
            a11yInactive = cfg.a11yInactiveDelay
            screenOff = cfg.screenOffDelay
            powerSave = cfg.powerSaveDelay
            usageCache = cfg.usageStatsCacheMs
            dbWrite = cfg.shieldDbWriteMs
            dbWriteNear = cfg.shieldDbWriteNearMs
            launcherCache = cfg.launcherCacheMs
        }
    }

    val activeCfg = preferences.buildPerformanceConfig()

    val autoSwitch = {
        if (selectedLevel.isPreset()) {
            onSelectedLevelChange(PerformanceLevel.CUSTOM)
        }
    }

    LaunchedEffect(Unit) {
        onRegisterApplyAction?.invoke {
            scope.launch {
                val config = PerformanceConfig(
                    a11yActiveDelay = a11yActive,
                    a11yInactiveDelay = a11yInactive,
                    screenOffDelay = screenOff,
                    powerSaveDelay = powerSave,
                    usageStatsCacheMs = usageCache,
                    shieldDbWriteMs = dbWrite,
                    shieldDbWriteNearMs = dbWriteNear,
                    launcherCacheMs = launcherCache,
                )
                val detectedLevel = config.detectPreset()
                onApplyCustomSettings(a11yActive, a11yInactive, screenOff, powerSave, usageCache, dbWrite, dbWriteNear, launcherCache)
                onSelectedLevelChange(detectedLevel)
                onSetPerformanceLevel?.invoke(detectedLevel)
            }
        }
    }

    PreferenceCategory(title = "Custom Tuning")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).background(
                        MaterialTheme.colorScheme.secondaryContainer, CircleShape
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Tune, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Per-Parameter Values",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                    Text(text = "Drag any slider then tap Apply to save",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            GroupHeader("Check Interval", Icons.Outlined.Timer)
            Spacer(modifier = Modifier.height(8.dp))
            SliderItem("Overlay Active", "How often to check when overlay is visible",
                a11yActive / 1000f, 30f..600f, "s", Icons.Outlined.Visibility,
                onChange = { a11yActive = (it * 1000).toLong(); autoSwitch() })
            SliderItem("Overlay Hidden", "How often to check when overlay is hidden",
                a11yInactive / 1000f, 1f..10f, "s", Icons.Outlined.VisibilityOff, 9,
                onChange = { a11yInactive = (it * 1000).toLong(); autoSwitch() })

            Spacer(modifier = Modifier.height(16.dp))
            GroupHeader("Power Saving", Icons.Outlined.BatteryChargingFull)
            Spacer(modifier = Modifier.height(8.dp))
            SliderItem("Screen Off", "How often to check when screen is off",
                screenOff / 60000f, 1f..30f, "min", Icons.Outlined.ScreenLockPortrait, 29,
                onChange = { screenOff = (it * 60000).toLong(); autoSwitch() })
            SliderItem("Battery Saver", "How often to check during power saving",
                powerSave / 60000f, 1f..30f, "min", Icons.Outlined.BatteryStd, 29,
                onChange = { powerSave = (it * 60000).toLong(); autoSwitch() })

            Spacer(modifier = Modifier.height(16.dp))
            GroupHeader("Data Refresh", Icons.Outlined.Storage)
            Spacer(modifier = Modifier.height(8.dp))
            SliderItem("Usage Stats", "Refresh app usage from system",
                usageCache / 60000f, 0.17f..60f, "min", Icons.Outlined.Refresh,
                onChange = { usageCache = (it * 60000).toLong(); autoSwitch() })
            SliderItem("App List", "Refresh list of installed apps",
                launcherCache / 60000f, 5f..240f, "min", Icons.Outlined.Apps,
                onChange = { launcherCache = (it * 60000).toLong(); autoSwitch() })

            Spacer(modifier = Modifier.height(16.dp))
            GroupHeader("Storage", Icons.Outlined.Save)
            Spacer(modifier = Modifier.height(8.dp))
            SliderItem("Save Data", "Write usage data to storage",
                dbWrite / 60000f, 1f..30f, "min", Icons.Outlined.Storage, 29,
                onChange = { dbWrite = (it * 60000).toLong(); autoSwitch() })
            SliderItem("Save Data (Near)", "More frequent saves near daily limit",
                dbWriteNear / 60000f, 0.5f..10f, "min", Icons.Outlined.TimerOff,
                onChange = { dbWriteNear = (it * 60000).toLong(); autoSwitch() })
        }
    }

    Spacer(modifier = Modifier.height(24.dp))
    PreferenceCategory(title = "Active Configuration")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Profile: ${preferences.performanceLevel.labelRes}",
                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = buildString {
                appendLine("- Overlay Active: ${fmtDur(activeCfg.a11yActiveDelay)}")
                appendLine("- Overlay Hidden: ${fmtDur(activeCfg.a11yInactiveDelay)}")
                appendLine("- Screen Off: ${fmtDur(activeCfg.screenOffDelay)}")
                appendLine("- Battery Saver: ${fmtDur(activeCfg.powerSaveDelay)}")
                appendLine("- Usage Stats: ${fmtDur(activeCfg.usageStatsCacheMs)}")
                appendLine("- App List: ${fmtDur(activeCfg.launcherCacheMs)}")
                appendLine("- Save Data: ${fmtDur(activeCfg.shieldDbWriteMs)}")
                appendLine("- Save Data (Near): ${fmtDur(activeCfg.shieldDbWriteNearMs)}")
            }, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun GroupHeader(text: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

private fun fmtDur(ms: Long): String = when {
    ms >= 60000 -> {
        val m = ms / 60000
        val s = (ms % 60000) / 1000
        if (s > 0) "${m}m ${s}s" else "${m}min"
    }
    ms >= 1000 -> "${ms / 1000}s"
    else -> "${ms}ms"
}

@Composable
private fun SliderItem(
    label: String,
    desc: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    unit: String,
    icon: ImageVector,
    steps: Int = 0,
    onChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(icon, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                Text(text = desc, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(text = fmtVal(value, unit), style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.width(32.dp))
            Slider(value = value.coerceIn(range), onValueChange = onChange,
                valueRange = range, steps = steps, modifier = Modifier.weight(1f))
        }
    }
}

private fun fmtVal(value: Float, unit: String): String = when {
    unit == "min" && value >= 60 -> "${(value / 60).toInt()}h"
    unit == "min" && value >= 1 -> "${value.toInt()}m"
    unit == "min" && value < 1 -> "${(value * 60).toInt()}s"
    else -> "${value.toInt()}s"
}

private fun activeConfigSummary(cfg: PerformanceConfig): String {
    val parts = mutableListOf<String>()
    parts.add("Active: ${fmtDur(cfg.a11yActiveDelay)}")
    parts.add("Hidden: ${fmtDur(cfg.a11yInactiveDelay)}")
    parts.add("Save: ${fmtDur(cfg.shieldDbWriteMs)}")
    return parts.joinToString(" · ")
}
