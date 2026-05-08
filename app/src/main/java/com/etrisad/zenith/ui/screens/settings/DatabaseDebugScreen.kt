package com.etrisad.zenith.ui.screens.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.etrisad.zenith.ui.viewmodel.HomeViewModel
import com.etrisad.zenith.ui.viewmodel.UsageHistoryGroup
import com.etrisad.zenith.ui.viewmodel.UsageRecord

@Composable
fun DatabaseDebugScreen(
    viewModel: HomeViewModel,
    innerPadding: PaddingValues,
    onBack: () -> Unit
) {
    val historyData by viewModel.fullUsageHistory.collectAsState(initial = emptyList())

    if (historyData.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Outlined.SdStorage,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Initialising history...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        val dateFormatter = remember { java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()) }
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 32.dp
            )
        ) {
            items(
                items = historyData,
                key = { it.date }
            ) { group ->
                ExpandableUsageGroup(
                    group = group,
                    viewModel = viewModel,
                    dateFormatter = dateFormatter
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun ExpandableUsageGroup(
    group: UsageHistoryGroup,
    viewModel: HomeViewModel,
    dateFormatter: java.text.SimpleDateFormat
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (group.isMissing) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .clickable { if (!group.isMissing) expanded = !expanded }
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (group.isMissing) MaterialTheme.colorScheme.errorContainer
                            else if (group.isLive) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (group.isMissing) Icons.Outlined.History 
                                      else if (group.hasDatabaseRecord) Icons.Outlined.CloudDone
                                      else Icons.Outlined.History,
                        contentDescription = null,
                        tint = if (group.isMissing) MaterialTheme.colorScheme.onErrorContainer
                               else if (group.isLive) MaterialTheme.colorScheme.onSecondaryContainer
                               else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = group.date,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (group.isLive) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape,
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text(
                                    "LIVE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (group.hasDatabaseRecord) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = "Saved",
                                modifier = Modifier.padding(start = 8.dp).size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Text(
                        text = if (group.isMissing) "No record found" 
                               else "Total: ${viewModel.formatDuration(group.totalTimeMillis)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (group.isMissing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!group.isMissing) {
                    Icon(
                        imageVector = Icons.Outlined.ExpandMore,
                        contentDescription = "Expand",
                        modifier = Modifier.rotate(rotation),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (expanded) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                ) {
                    HorizontalDivider(
                        modifier = Modifier.padding(bottom = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    
                    group.records.forEachIndexed { index, record ->
                        val isFirst = index == 0
                        val isLast = index == group.records.size - 1
                        
                        when (record) {
                            is UsageRecord.Database -> {
                                UsageRecordItem(
                                    packageName = record.entity.packageName,
                                    formattedDuration = viewModel.formatDuration(record.entity.usageTimeMillis),
                                    formattedTime = dateFormatter.format(java.util.Date(record.entity.lastUpdated)),
                                    isDatabase = true,
                                    isFirst = isFirst,
                                    isLast = isLast
                                )
                            }
                            is UsageRecord.Live -> {
                                UsageRecordItem(
                                    packageName = record.packageName,
                                    formattedDuration = viewModel.formatDuration(record.usageTimeMillis),
                                    formattedTime = "Now",
                                    isDatabase = false,
                                    isFirst = isFirst,
                                    isLast = isLast
                                )
                            }
                        }
                        
                        if (!isLast) {
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UsageRecordItem(
    packageName: String,
    formattedDuration: String,
    formattedTime: String,
    isDatabase: Boolean,
    isFirst: Boolean,
    isLast: Boolean
) {
    val topRadius = if (isFirst) 16.dp else 4.dp
    val bottomRadius = if (isLast) 16.dp else 4.dp
    val shape = RoundedCornerShape(
        topStart = topRadius,
        topEnd = topRadius,
        bottomStart = bottomRadius,
        bottomEnd = bottomRadius
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = if (isDatabase) MaterialTheme.colorScheme.surfaceContainerLow 
                else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (packageName == "TOTAL") Icons.Outlined.History else Icons.Outlined.SdStorage,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (packageName == "TOTAL") MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = packageName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isDatabase) "Saved: $formattedTime" else "Source: Live",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.9f,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = formattedDuration,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
