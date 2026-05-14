package com.etrisad.zenith.ui.components.overlay

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.etrisad.zenith.data.preferences.UserPreferences
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun TotalUsagePill(totalGlobalUsageToday: Long, userPrefs: UserPreferences, modifier: Modifier = Modifier) {
    if (!userPrefs.totalUsagePillEnabled) return

    val screenTimeTargetMinutes = userPrefs.screenTimeTargetMinutes
    val totalUsageMinutes = totalGlobalUsageToday / 60000
    val isTargetExceeded = totalUsageMinutes >= screenTimeTargetMinutes && screenTimeTargetMinutes > 0

    val totalPillColor = if (isTargetExceeded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
    val totalPillContentColor = if (isTargetExceeded) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onTertiary

    val baseColor = MaterialTheme.colorScheme.primary
    val fillColor = MaterialTheme.colorScheme.tertiary
    val contentColor = MaterialTheme.colorScheme.onPrimary
    val cornerRadiusLarge = 24.dp
    val cornerRadiusSmall = 4.dp

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Surface(
            color = totalPillColor,
            shape = RoundedCornerShape(
                topStart = cornerRadiusLarge,
                bottomStart = cornerRadiusLarge,
                topEnd = cornerRadiusSmall,
                bottomEnd = cornerRadiusSmall
            )
        ) {
            Text(
                text = "Total",
                style = MaterialTheme.typography.labelSmall,
                color = totalPillContentColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        val remainingMinutes = (screenTimeTargetMinutes - totalUsageMinutes).coerceAtLeast(0)
        val fillRatio = if (screenTimeTargetMinutes > 0) {
            remainingMinutes.toFloat() / screenTimeTargetMinutes
        } else 0f

        Surface(
            color = baseColor,
            shape = RoundedCornerShape(
                topStart = cornerRadiusSmall,
                bottomStart = cornerRadiusSmall,
                topEnd = cornerRadiusLarge,
                bottomEnd = cornerRadiusLarge
            )
        ) {
            val separatorColor = MaterialTheme.colorScheme.surface
            Box(
                modifier = Modifier.drawBehind {
                    val progressWidth = size.width * fillRatio.coerceIn(0f, 1f)
                    drawRect(
                        color = fillColor,
                        size = Size(width = progressWidth, height = size.height)
                    )

                    if (fillRatio > 0f && fillRatio < 1f) {
                        drawRect(
                            color = separatorColor,
                            topLeft = Offset(progressWidth - 1.dp.toPx(), 0f),
                            size = Size(width = 2.dp.toPx(), height = size.height)
                        )
                    }
                }
            ) {
                Text(
                    text = formatMillis(totalGlobalUsageToday),
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EmergencyButton(onEmergencyUse: () -> Unit, onHoldingChange: (Boolean) -> Unit = {}) {
    var isHolding by remember { mutableStateOf(false) }
    var holdProgressTarget by remember { mutableFloatStateOf(0f) }

    val animatedProgressState = animateFloatAsState(
        targetValue = holdProgressTarget,
        animationSpec = if (isHolding) tween(5000, easing = LinearEasing) else tween(300),
        label = "holdProgress"
    )

    LaunchedEffect(isHolding) {
        onHoldingChange(isHolding)
        if (isHolding) {
            holdProgressTarget = 1f
            delay(5000)
            if (isHolding) {
                onEmergencyUse()
                isHolding = false
                holdProgressTarget = 0f
            }
        } else {
            holdProgressTarget = 0f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isHolding = true
                        try {
                            awaitRelease()
                        } finally {
                            isHolding = false
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        val progressColor = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(
                        color = progressColor,
                        size = size.copy(width = size.width * animatedProgressState.value)
                    )
                }
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.width(8.dp))
            val secondsRemaining by remember {
                derivedStateOf { 5 - (animatedProgressState.value * 5).toInt() }
            }
            Text(
                text = if (isHolding) "Hold for ${secondsRemaining}s..." else "Hold for 5s to use Emergency",
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CloseAppTextButton(onCloseApp: () -> Unit, autoKickProgress: () -> Float = { 0f }) {
    val density = LocalDensity.current
    val infiniteTransition = rememberInfiniteTransition(label = "autoKickWavy")
    val waveAmplitudeState = infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "amplitude"
    )

    TextButton(
        onClick = onCloseApp,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.error
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Close App",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            val isVisible by remember { derivedStateOf { autoKickProgress() > 0.4f } }
            AnimatedVisibility(
                visible = isVisible,
                enter = expandHorizontally(
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
                ) + fadeIn(),
                exit = shrinkHorizontally() + fadeOut()
            ) {
                CircularWavyProgressIndicator(
                    progress = autoKickProgress,
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .size(24.dp),
                    color = MaterialTheme.colorScheme.error,
                    trackColor = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                    stroke = with(density) { Stroke(width = 2.dp.toPx()) },
                    trackStroke = with(density) { Stroke(width = 2.dp.toPx()) },
                    wavelength = 8.dp,
                    amplitude = { waveAmplitudeState.value }
                )
            }
        }
    }
}

@Composable
fun DurationButtonsGrid(remainingMinutes: Int?, onAllowUse: (Int) -> Unit) {
    val b20 = if (remainingMinutes != null && remainingMinutes < 20) remainingMinutes else 20
    val b10 = if (remainingMinutes != null && remainingMinutes < 10) (remainingMinutes * 0.5).toInt().coerceAtLeast(1) else 10
    val b5 = if (remainingMinutes != null && remainingMinutes < 5) {
        (remainingMinutes * 0.75).toInt().coerceAtLeast(1)
    } else 5
    val b2 = if (remainingMinutes != null && remainingMinutes < 2) remainingMinutes else 2

    val outerRadius = 24.dp
    val innerRadius = 4.dp

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            DurationButton(
                minutes = b2,
                delaySeconds = 0,
                onAllowUse = onAllowUse,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(topStart = outerRadius, topEnd = innerRadius, bottomStart = innerRadius, bottomEnd = innerRadius)
            )
            DurationButton(
                minutes = b5,
                delaySeconds = 3,
                onAllowUse = onAllowUse,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(topStart = innerRadius, topEnd = outerRadius, bottomStart = innerRadius, bottomEnd = innerRadius)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            DurationButton(
                minutes = b10,
                delaySeconds = 6,
                onAllowUse = onAllowUse,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(topStart = innerRadius, topEnd = innerRadius, bottomStart = outerRadius, bottomEnd = innerRadius)
            )
            DurationButton(
                minutes = b20,
                delaySeconds = 10,
                onAllowUse = onAllowUse,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(topStart = innerRadius, topEnd = innerRadius, bottomStart = innerRadius, bottomEnd = outerRadius)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DurationButton(
    minutes: Int,
    delaySeconds: Int,
    onAllowUse: (Int) -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large
) {
    var startAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { startAnimation = true }

    val progressState = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = if (delaySeconds > 0) {
            tween(durationMillis = delaySeconds * 1000, easing = LinearEasing)
        } else {
            snap()
        },
        label = "buttonProgress"
    )

    val isEnabled = progressState.value >= 1f
    val buttonScale = remember { Animatable(1f) }

    LaunchedEffect(isEnabled) {
        if (isEnabled) {
            buttonScale.animateTo(
                targetValue = 1f,
                initialVelocity = 1.5f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
    }

    FilledTonalButton(
        onClick = { if (isEnabled) onAllowUse(minutes) },
        enabled = isEnabled,
        modifier = modifier
            .height(64.dp)
            .graphicsLayer {
                scaleX = buttonScale.value
                scaleY = buttonScale.value
                alpha = if (isEnabled) 1f else 0.8f
            },
        shape = shape,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = if (isEnabled) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        AnimatedContent(
            targetState = isEnabled,
            transitionSpec = {
                fadeIn(animationSpec = tween(400))
                    .togetherWith(fadeOut(animationSpec = tween(200)))
            },
            label = "buttonContent"
        ) { enabled ->
            if (!enabled) {
                Box(contentAlignment = Alignment.Center) {
                    CircularWavyProgressIndicator(
                        progress = { progressState.value },
                        modifier = Modifier.size(36.dp),
                        color = MaterialTheme.colorScheme.tertiary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        stroke = Stroke(width = 6.dp.value),
                        trackStroke = Stroke(width = 6.dp.value),
                        wavelength = 12.dp
                    )
                    val secondsLeft by remember(delaySeconds) {
                        derivedStateOf {
                            if (delaySeconds > 0) {
                                kotlin.math.ceil(delaySeconds * (1f - progressState.value)).toInt().coerceAtLeast(1)
                            } else 0
                        }
                    }

                    if (secondsLeft > 0) {
                        Text(
                            text = secondsLeft.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            } else {
                Text(
                    text = "$minutes mins",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

fun formatMillis(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val hours = minutes / 60
    return if (hours > 0) {
        "${hours}h ${minutes % 60}m"
    } else {
        "${minutes}m"
    }
}

fun formatCountdown(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
}
