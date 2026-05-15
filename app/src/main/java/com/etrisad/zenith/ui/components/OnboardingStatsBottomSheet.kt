package com.etrisad.zenith.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.etrisad.zenith.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingStatsBottomSheet(
    repository: UserPreferencesRepository,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        OnboardingStatsContent(
            onSelection = { useSystemUsage ->
                scope.launch {
                    repository.setPreferSystemUsageHistory(useSystemUsage)
                    repository.setOnboardingStatsCompleted(true)
                    sheetState.hide()
                    onDismiss()
                }
            },
            onSkip = {
                scope.launch {
                    repository.setOnboardingStatsCompleted(true)
                    sheetState.hide()
                    onDismiss()
                }
            }
        )
    }
}

@Composable
fun OnboardingStatsContent(
    onSelection: (Boolean) -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Outlined.BarChart,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Statistic Experience",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Choose how you want to see your usage history starting from today.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))

        OptionCard(
            title = "Start Fresh (Recommended)",
            description = "Only show data recorded by Zenith. Previous days will appear empty.",
            pros = listOf("Accurate Zenith tracking", "Clean visualization"),
            cons = listOf("No history for past days", "Needs more usage for better stats"),
            icon = Icons.Outlined.AutoAwesome,
            isHighlighted = true,
            onClick = { onSelection(false) },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        OptionCard(
            title = "Populate with System Data",
            description = "Use Android system usage to fill history for previous days.",
            pros = listOf("Instant historical data", "Complete overview"),
            cons = listOf("Might not be accurate", "May include background usage", "Less precise than Zenith"),
            icon = Icons.Outlined.History,
            isHighlighted = false,
            onClick = { onSelection(true) },
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 28.dp, bottomEnd = 28.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        ZenithButton(
            onClick = {},
            onHoldComplete = onSkip,
            text = "Hold to Skip",
            type = ZenithButtonType.Hold,
            fillMaxWidth = true,
            holdDuration = 2000L
        )
    }
}

@Composable
fun OptionCard(
    title: String,
    description: String,
    pros: List<String>,
    cons: List<String>,
    icon: ImageVector,
    isHighlighted: Boolean,
    onClick: () -> Unit,
    shape: RoundedCornerShape
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isHighlighted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                      else MaterialTheme.colorScheme.surfaceContainer,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "bgColor"
    )

    val titleColor by animateColorAsState(
        targetValue = if (isHighlighted) MaterialTheme.colorScheme.onSurface 
                      else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "titleColor"
    )

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape),
        shape = shape,
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (isHighlighted) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            else MaterialTheme.colorScheme.primaryContainer,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = titleColor
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isHighlighted) 0.8f else 0.6f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth()) {
                val itemContentColor = if (isHighlighted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Pros",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    pros.forEach { pro ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                            Icon(Icons.Outlined.Add, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(pro, style = MaterialTheme.typography.labelSmall, color = itemContentColor.copy(alpha = 0.7f))
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Cons",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    cons.forEach { con ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                            Icon(Icons.Outlined.Remove, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(con, style = MaterialTheme.typography.labelSmall, color = itemContentColor.copy(alpha = 0.7f))
                        }
                    }
                }
            }
        }
    }
}


