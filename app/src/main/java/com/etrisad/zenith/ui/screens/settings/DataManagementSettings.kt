package com.etrisad.zenith.ui.screens.settings

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.etrisad.zenith.data.preferences.UserPreferences
import com.etrisad.zenith.ui.components.ZenithToggleButtonGroup
import com.etrisad.zenith.ui.components.ZenithToggleOption
import com.etrisad.zenith.ui.components.ZenithButtonSize

@Composable
fun DataManagementSettings(
    preferences: UserPreferences,
    onAutoBackupEnabledChange: (Boolean) -> Unit,
    onPickBackupDirectory: () -> Unit,
    onSetBackupInterval: (Int) -> Unit,
    onBackup: () -> Unit,
    onRefreshOnOpenUsageStatsChange: (Boolean) -> Unit,
    onRestore: () -> Unit
) {
    Column {
        PreferenceCategory(title = "Data Management")

        SettingsToggle(
            title = "Auto Backup",
            description = "Periodically backup your database to a folder",
            checked = preferences.autoBackupEnabled,
            onCheckedChange = onAutoBackupEnabledChange,
            icon = Icons.Outlined.AutoMode,
            shape = if (preferences.autoBackupEnabled)
                RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
            else RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
        )

        AnimatedVisibility(
            visible = preferences.autoBackupEnabled,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                Spacer(modifier = Modifier.height(4.dp))
                AutoBackupSettings(
                    directoryUri = preferences.backupDirectoryUri,
                    intervalHours = preferences.backupIntervalHours,
                    lastBackupTimestamp = preferences.lastBackupTimestamp,
                    onPickDirectory = onPickBackupDirectory,
                    onSetInterval = onSetBackupInterval,
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        SettingsActionItem(
            title = "Manual Backup",
            summary = "Save your settings and schedules to a file now",
            onClick = onBackup,
            icon = Icons.Outlined.Backup,
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))
        SettingsToggle(
            title = "Sync on Entry",
            description = "Refresh usage stats every time you open the stats screen. Enable this if you experience data inconsistency.",
            checked = preferences.refreshOnOpenUsageStats,
            onCheckedChange = onRefreshOnOpenUsageStatsChange,
            icon = Icons.Outlined.Sync,
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))
        SettingsActionItem(
            title = "Restore Data",
            summary = "Load data from a previous backup file",
            onClick = onRestore,
            icon = Icons.Outlined.Restore,
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
        )
    }
}

@Composable
fun AutoBackupSettings(
    directoryUri: String,
    intervalHours: Int,
    lastBackupTimestamp: Long,
    onPickDirectory: () -> Unit,
    onSetInterval: (Int) -> Unit,
    shape: Shape = RoundedCornerShape(8.dp)
) {
    val readablePath = remember(directoryUri) {
        if (directoryUri.isEmpty()) "Not selected"
        else {
            try {
                val uri = android.net.Uri.parse(directoryUri)
                val path = uri.path ?: ""
                if (path.contains(":")) {
                    path.substringAfterLast(":")
                } else {
                    path
                }
            } catch (e: Exception) {
                "Selected Folder"
            }
        }
    }

    val lastBackupText = remember(lastBackupTimestamp) {
        if (lastBackupTimestamp == 0L) "Never"
        else {
            val sdf = java.text.SimpleDateFormat("MMM d, yyyy HH:mm", java.util.Locale.getDefault())
            sdf.format(java.util.Date(lastBackupTimestamp))
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Auto Backup Configuration",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                onClick = onPickDirectory,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
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
                            imageVector = Icons.Outlined.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Backup Location", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = readablePath,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (directoryUri.isEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                MaterialTheme.colorScheme.secondaryContainer,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Last Backup", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = lastBackupText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(16.dp))

            Text("Backup Interval", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            
            val intervals = listOf(3, 6, 12, 24)
            ZenithToggleButtonGroup(
                options = intervals.map { ZenithToggleOption(text = "${it}h") },
                selectedIndices = setOf(intervals.indexOf(intervalHours)),
                onToggle = { index -> onSetInterval(intervals[index]) },
                size = ZenithButtonSize.Medium,
                isInsideContainer = true
            )
        }
    }
}
