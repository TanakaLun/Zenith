package com.etrisad.zenith.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Camera
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.etrisad.zenith.data.local.entity.FocusType
import com.etrisad.zenith.ui.viewmodel.AppUsageInfo
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SnapshotCard(
    stamps: List<AppUsageInfo>,
    selectedDateMillis: Long,
    getAppType: (String) -> FocusType?,
    onDaySelected: (Long) -> Unit,
    formatDuration: (Long) -> String,
    showDatabaseIndicator: Boolean = false,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(28.dp),
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceContainerLow
) {
    val pages = remember(stamps) { stamps.chunked(7) }
    val pageCount = pages.size.coerceAtLeast(1)
    val pagerState = rememberPagerState(pageCount = { pageCount }, initialPage = (pageCount - 1).coerceAtLeast(0))
    
    val selectedIndex = remember(stamps, selectedDateMillis) {
        val cal = Calendar.getInstance()
        cal.timeInMillis = selectedDateMillis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val target = cal.timeInMillis

        stamps.indices.find { i ->
            val dCal = Calendar.getInstance()
            val daysAgo = (stamps.size - 1) - i
            dCal.add(Calendar.DAY_OF_YEAR, -daysAgo)
            dCal.set(Calendar.HOUR_OF_DAY, 0)
            dCal.set(Calendar.MINUTE, 0)
            dCal.set(Calendar.SECOND, 0)
            dCal.set(Calendar.MILLISECOND, 0)
            dCal.timeInMillis == target
        }
    }

    val selectedPageIndex = selectedIndex?.let { it / 7 }
    val selectedAppIndex = selectedIndex?.let { it % 7 }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Camera,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Snapshot",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                val selectedApp = selectedIndex?.let { stamps.getOrNull(it) }

                AnimatedContent(
                    targetState = selectedApp,
                    transitionSpec = {
                        (fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) + 
                         slideInVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)) { it / 2 })
                            .togetherWith(fadeOut(animationSpec = spring(stiffness = Spring.StiffnessLow)) + 
                                         slideOutVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) { -it / 2 })
                    },
                    label = "SnapshotDuration"
                ) { app ->
                    if (app != null && app.totalTimeVisible > 0) {
                        val appType = remember(app.packageName) { getAppType(app.packageName) }
                        val textColor = when (appType) {
                            FocusType.GOAL -> MaterialTheme.colorScheme.tertiary
                            FocusType.SHIELD -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Text(
                            text = formatDuration(app.totalTimeVisible),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = textColor
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { pageIndex ->
                val pageData = pages.getOrNull(pageIndex) ?: emptyList()
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    pageData.forEachIndexed { appIndex, app ->
                        val isSelected = selectedAppIndex == appIndex && selectedPageIndex == pageIndex
                        
                        val indexInStamps = pageIndex * 7 + appIndex
                        val streak = remember(stamps, pageIndex, appIndex) {
                            val currentApp = stamps.getOrNull(indexInStamps)
                            if (currentApp == null || currentApp.packageName.isEmpty()) 0
                            else {
                                var count = 0
                                var checkIndex = indexInStamps - 1
                                while (checkIndex >= 0 && stamps.getOrNull(checkIndex)?.packageName == currentApp.packageName) {
                                    count++
                                    checkIndex--
                                }
                                count
                            }
                        }

                        val hasPrevConnection = remember(stamps, indexInStamps) {
                            app.packageName.isNotEmpty() && indexInStamps > 0 && 
                            stamps.getOrNull(indexInStamps - 1)?.packageName == app.packageName
                        }
                        
                        val hasNextConnection = remember(stamps, indexInStamps) {
                            app.packageName.isNotEmpty() && indexInStamps < stamps.size - 1 && 
                            stamps.getOrNull(indexInStamps + 1)?.packageName == app.packageName
                        }

                        val indicatorSize by animateDpAsState(
                            targetValue = if (app.icon != null) 40.dp else 24.dp,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
                            label = "SnapshotIndicatorSize"
                        )

                        val dayShape = when (streak) {
                            0 -> MaterialShapes.Square.toShape()
                            1 -> MaterialShapes.Circle.toShape()
                            2 -> MaterialShapes.Pill.toShape()
                            3 -> MaterialShapes.Pentagon.toShape()
                            4 -> MaterialShapes.Gem.toShape()
                            5 -> MaterialShapes.Cookie7Sided.toShape()
                            else -> MaterialShapes.Sunny.toShape()
                        }

                        val appType = remember(app.packageName) { getAppType(app.packageName) }
                        val streakColor = when (appType) {
                            FocusType.GOAL -> MaterialTheme.colorScheme.tertiary
                            FocusType.SHIELD -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }

                        val accentAlpha = (streak * 0.15f).coerceAtMost(0.6f)
                        val baseColor = if (streak > 0) {
                            streakColor.copy(alpha = 0.1f + accentAlpha)
                        } else {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    val cal = Calendar.getInstance()
                                    val daysAgo = (stamps.size - 1) - indexInStamps
                                    cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
                                    onDaySelected(cal.timeInMillis)
                                }
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier.size(40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(indicatorSize)
                                            .clip(dayShape)
                                            .background(
                                                if (isSelected) streakColor.copy(alpha = 0.3f + accentAlpha)
                                                else baseColor
                                            )
                                    )
                                }
                            }

                            if (hasPrevConnection || hasNextConnection) {
                                val lineColor = streakColor
                                androidx.compose.foundation.Canvas(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp)
                                ) {
                                    val centerY = size.height / 2
                                    if (hasPrevConnection) {
                                        drawLine(
                                            color = lineColor,
                                            start = androidx.compose.ui.geometry.Offset(0f, centerY),
                                            end = androidx.compose.ui.geometry.Offset(size.width / 2, centerY),
                                            strokeWidth = 2.dp.toPx()
                                        )
                                    }
                                    if (hasNextConnection) {
                                        drawLine(
                                            color = lineColor,
                                            start = androidx.compose.ui.geometry.Offset(size.width / 2, centerY),
                                            end = androidx.compose.ui.geometry.Offset(size.width, centerY),
                                            strokeWidth = 2.dp.toPx()
                                        )
                                    }
                                }
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier.size(40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (app.icon != null) {
                                        Image(
                                            painter = BitmapPainter(app.icon.toBitmap().asImageBitmap()),
                                            contentDescription = null,
                                            modifier = Modifier.size(28.dp).clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                val dayLabel = remember(pageIndex, appIndex, stamps.size) {
                                    val cal = Calendar.getInstance()
                                    val indexInStamps = pageIndex * 7 + appIndex
                                    val daysAgo = (stamps.size - 1) - indexInStamps
                                    cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
                                    SimpleDateFormat("E", Locale.getDefault()).format(cal.time).first().toString()
                                }
                                Text(
                                    text = dayLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (showDatabaseIndicator) {
                                    val indicatorColor = when {
                                        app.totalTimeVisible == 0L && !app.isLive -> MaterialTheme.colorScheme.error
                                        app.isLive -> MaterialTheme.colorScheme.tertiary
                                        app.hasDatabaseRecord -> MaterialTheme.colorScheme.primary
                                        app.hasSystemData -> MaterialTheme.colorScheme.secondary
                                        else -> MaterialTheme.colorScheme.error
                                    }
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 2.dp)
                                            .width(12.dp)
                                            .height(2.dp)
                                            .clip(CircleShape)
                                            .background(indicatorColor)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(pageCount) { iteration ->
                    val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(color)
                            .size(6.dp)
                    )
                }
            }
        }
    }
}
