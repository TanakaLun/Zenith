package com.etrisad.zenith.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Stars
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.etrisad.zenith.ui.components.SnapshotCard
import com.etrisad.zenith.ui.components.UsageHistoryCard
import com.etrisad.zenith.ui.viewmodel.AppUsageInfo
import com.etrisad.zenith.ui.viewmodel.HomeViewModel
import com.etrisad.zenith.ui.viewmodel.HourlyUsageInfo
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.runtime.saveable.rememberSaveable

@Composable
fun UsageStatsScreen(
    viewModel: HomeViewModel,
    innerPadding: PaddingValues,
    onAppClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedHour by rememberSaveable { mutableStateOf<Int?>(null) }
    var isOtherAppsExpanded by rememberSaveable { mutableStateOf(false) }
    var isOtherHourAppsExpanded by rememberSaveable(selectedHour) { mutableStateOf(false) }

    val (regularApps, lowUsageApps) = remember(uiState.allAppsUsage) {
        uiState.allAppsUsage.partition { it.totalTimeVisible >= 60000L }
    }
    
    val totalLowUsageTime = lowUsageApps.sumOf { it.totalTimeVisible }

    val hourlyAppsData = remember(uiState.hourlyUsage, selectedHour) {
        val hourData = uiState.hourlyUsage.find { it.hour == selectedHour }
        if (hourData != null && hourData.apps.isNotEmpty()) {
            val (regular, low) = hourData.apps.partition { it.totalTimeVisible >= 60000L }
            val totalLowTime = low.sumOf { it.totalTimeVisible }
            Triple(hourData, regular, Triple(low, totalLowTime, hourData.hour))
        } else null
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(
            top = innerPadding.calculateTopPadding() + 16.dp,
            bottom = innerPadding.calculateBottomPadding() + 24.dp
        )
    ) {
        item(key = "hourly_usage_group") {
            Column(modifier = Modifier.animateItem()) {
                GroupedCard(index = 0, total = 2) {
                    HourlyStatsContent(
                        hourlyUsage = uiState.hourlyUsage,
                        selectedHour = selectedHour,
                        onHourClick = { selectedHour = if (selectedHour == it) null else it },
                        formatDuration = viewModel::formatDuration
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                UsageHistoryCard(
                    history = uiState.dailyUsageHistory,
                    targetMillis = uiState.targetMillis,
                    selectedDateMillis = uiState.selectedDateMillis,
                    formatDuration = viewModel::formatDuration,
                    onDaySelected = { usage ->
                        viewModel.selectDate(usage?.date)
                    },
                    title = "Usage Trends",
                    showDatabaseIndicator = true,
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 28.dp, bottomEnd = 28.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            }
        }

        if (hourlyAppsData != null) {
            val (_, regularHourApps, lowData) = hourlyAppsData
            val (lowUsageHourApps, totalLowUsageHourTime, currentTargetHour) = lowData
            
            val totalItems = regularHourApps.size + (if (totalLowUsageHourTime > 0) {
                if (isOtherHourAppsExpanded) 1 + lowUsageHourApps.size else 1
            } else 0)

            item(key = "hourly_title_$currentTargetHour") {
                Text(
                    text = "Apps at $currentTargetHour:00",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .animateItem()
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                        .padding(top = 16.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            itemsIndexed(
                items = regularHourApps,
                key = { _, app -> "hourly-$currentTargetHour-${app.packageName}" }
            ) { index, app ->
                Column(modifier = Modifier.animateItem()) {
                    UsageItem(
                        app = app,
                        formatDuration = viewModel::formatDuration,
                        index = index,
                        total = totalItems,
                        onClick = { onAppClick(app.packageName) }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            if (totalLowUsageHourTime > 0) {
                val otherIndex = regularHourApps.size
                item(key = "hourly_other_header_$currentTargetHour") {
                    Column(modifier = Modifier.animateItem()) {
                        GroupedCard(
                            index = otherIndex,
                            total = totalItems,
                            onClick = { isOtherHourAppsExpanded = !isOtherHourAppsExpanded }
                        ) {
                            ListItem(
                                headlineContent = {
                                    Text(
                                        text = "Other Apps",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                supportingContent = {
                                    Text(
                                        text = "${lowUsageHourApps.size} apps with minimal usage",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                trailingContent = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = viewModel.formatDuration(totalLowUsageHourTime),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                        Icon(
                                            imageVector = if (isOtherHourAppsExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp).padding(start = 4.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                leadingContent = {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Android,
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                colors = ListItemDefaults.colors(
                                    containerColor = Color.Transparent
                                )
                            )
                        }
                        if (isOtherHourAppsExpanded) Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                if (isOtherHourAppsExpanded) {
                    itemsIndexed(
                        items = lowUsageHourApps,
                        key = { _, app -> "hourly-low-$currentTargetHour-${app.packageName}" }
                    ) { index, app ->
                        Column(modifier = Modifier.animateItem()) {
                            UsageItem(
                                app = app,
                                formatDuration = viewModel::formatDuration,
                                index = otherIndex + 1 + index,
                                total = totalItems,
                                onClick = { onAppClick(app.packageName) }
                            )
                            if (index < lowUsageHourApps.size - 1) Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }

        item(key = "snapshot_stamps") {
            Column(modifier = Modifier.animateItem()) {
                Spacer(modifier = Modifier.height(16.dp))
                SnapshotCard(
                    stamps = uiState.snapshotStamps,
                    selectedDateMillis = uiState.selectedDateMillis,
                    onDaySelected = { viewModel.selectDate(it) },
                    formatDuration = viewModel::formatDuration
                )
            }
        }

        item(key = "app_usage_header") {
            Column(modifier = Modifier.animateItem()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "App Usage",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                )
            }
        }

        itemsIndexed(
            items = regularApps,
            key = { _, app -> "regular-${app.packageName}" }
        ) { index, app ->
            val total = regularApps.size + (if (totalLowUsageTime > 0) (if (isOtherAppsExpanded) 1 + lowUsageApps.size else 1) else 0)
            Column(modifier = Modifier.animateItem()) {
                UsageItem(
                    app = app,
                    formatDuration = viewModel::formatDuration,
                    index = index,
                    total = total,
                    onClick = { onAppClick(app.packageName) }
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        if (totalLowUsageTime > 0) {
            val otherIndex = regularApps.size
            val total = regularApps.size + (if (isOtherAppsExpanded) 1 + lowUsageApps.size else 1)
            
            item(key = "other_apps_header") {
                Column(modifier = Modifier.animateItem()) {
                    GroupedCard(
                        index = otherIndex,
                        total = total,
                        onClick = { isOtherAppsExpanded = !isOtherAppsExpanded }
                    ) {
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = "Other Apps",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            supportingContent = {
                                Text(
                                    text = "${lowUsageApps.size} apps with minimal usage",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = viewModel.formatDuration(totalLowUsageTime),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Icon(
                                        imageVector = if (isOtherAppsExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp).padding(start = 4.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Android,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = Color.Transparent
                            )
                        )
                    }
                    if (isOtherAppsExpanded) {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }

            if (isOtherAppsExpanded) {
                itemsIndexed(
                    items = lowUsageApps,
                    key = { _, app -> "low-${app.packageName}" }
                ) { index, app ->
                    Column(modifier = Modifier.animateItem()) {
                        UsageItem(
                            app = app,
                            formatDuration = viewModel::formatDuration,
                            index = otherIndex + 1 + index,
                            total = total,
                            onClick = { onAppClick(app.packageName) }
                        )
                        if (index < lowUsageApps.size - 1) {
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GroupedCard(
    index: Int,
    total: Int,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val shape = when {
        total == 1 -> RoundedCornerShape(28.dp)
        index == 0 -> RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 12.dp, bottomEnd = 12.dp)
        index == total - 1 -> RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 28.dp, bottomEnd = 28.dp)
        else -> RoundedCornerShape(12.dp)
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        content = { content() }
    )
}

@Composable
fun HourlyStatsContent(
    hourlyUsage: List<HourlyUsageInfo>,
    selectedHour: Int?,
    onHourClick: (Int) -> Unit,
    formatDuration: (Long) -> String
) {
    val maxUsage = hourlyUsage.maxOfOrNull { it.usageTimeMillis }?.coerceAtLeast(1L) ?: 1L
    
    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Hourly Usage",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            AnimatedContent(
                targetState = selectedHour,
                transitionSpec = {
                    (fadeIn() + slideInVertically { it / 2 }).togetherWith(fadeOut() + slideOutVertically { -it / 2 })
                },
                label = "SelectedHourText"
            ) { hour ->
                if (hour != null) {
                    val usage = hourlyUsage.find { it.hour == hour }?.usageTimeMillis ?: 0L
                    Text(
                        text = formatDuration(usage),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            hourlyUsage.forEach { hourInfo ->
                val isSelected = selectedHour == hourInfo.hour
                val isCurrentHour = hourInfo.hour == Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                
                val barHeight = (hourInfo.usageTimeMillis.toFloat() / maxUsage).coerceIn(0.05f, 1f)
                val animatedHeight by animateFloatAsState(
                    targetValue = barHeight,
                    animationSpec = tween(500, easing = FastOutSlowInEasing),
                    label = "HourlyBarHeight"
                )
                
                val barColor = when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isCurrentHour -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 1.5.dp)
                        .fillMaxHeight(animatedHeight)
                        .clip(CircleShape)
                        .background(barColor)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onHourClick(hourInfo.hour) }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("00:00", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("12:00", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("23:59", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun UsageItem(
    app: AppUsageInfo,
    formatDuration: (Long) -> String,
    index: Int,
    total: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GroupedCard(index = index, total = total, onClick = onClick, modifier = modifier) {
        ListItem(
            headlineContent = {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            trailingContent = {
                Text(
                    text = formatDuration(app.totalTimeVisible),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            },
            leadingContent = {
                if (app.icon != null) {
                    Image(
                        painter = BitmapPainter(app.icon.toBitmap().asImageBitmap()),
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Android,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent
            )
        )
    }
}
