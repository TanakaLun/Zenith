package com.etrisad.zenith.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.outlined.Stars
import androidx.compose.material3.*
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
import com.etrisad.zenith.ui.viewmodel.AppUsageInfo
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SnapshotCard(
    stamps: List<AppUsageInfo>,
    selectedDateMillis: Long,
    onDaySelected: (Long) -> Unit,
    formatDuration: (Long) -> String
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
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Stars,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Snapshot",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                val selectedApp = selectedIndex?.let { stamps.getOrNull(it) }

                AnimatedContent(
                    targetState = selectedApp,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(300)) + slideInVertically { it / 2 })
                            .togetherWith(fadeOut(animationSpec = tween(300)) + slideOutVertically { -it / 2 })
                    },
                    label = "SnapshotDuration"
                ) { app ->
                    if (app != null && app.totalTimeVisible > 0) {
                        Text(
                            text = formatDuration(app.totalTimeVisible),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { pageIndex ->
                val pageData = pages.getOrNull(pageIndex) ?: emptyList()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    pageData.forEachIndexed { appIndex, app ->
                        val isSelected = selectedAppIndex == appIndex && selectedPageIndex == pageIndex
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                val cal = Calendar.getInstance()
                                val indexInStamps = pageIndex * 7 + appIndex
                                val daysAgo = (stamps.size - 1) - indexInStamps
                                cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
                                onDaySelected(cal.timeInMillis)
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (app.icon != null) {
                                    Image(
                                        painter = BitmapPainter(app.icon.toBitmap().asImageBitmap()),
                                        contentDescription = null,
                                        modifier = Modifier.size(30.dp).clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
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
