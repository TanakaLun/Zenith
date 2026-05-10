package com.etrisad.zenith.ui.components

import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.toPath
import coil.compose.AsyncImage

data class GoalAppInfo(
    val packageName: String,
    val appName: String,
    val icon: Any?
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppGoalOverlayContent(
    packageNames: List<String>,
    onAnswer: (String) -> Unit,
    onSnooze: () -> Unit,
    onHangUp: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    var showSelectionSheet by remember { mutableStateOf(false) }

    val appInfos = remember(packageNames) {
        packageNames.map { pkg ->
            val label = try {
                val appInfo = context.packageManager.getApplicationInfo(pkg, 0)
                context.packageManager.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                pkg
            }
            val icon = try {
                context.packageManager.getApplicationIcon(pkg)
            } catch (e: Exception) {
                null
            }
            GoalAppInfo(pkg, label, icon)
        }
    }

    val displayTitle = remember(appInfos) {
        if (appInfos.size == 1) {
            appInfos[0].appName
        } else {
            "${appInfos[0].appName} +${appInfos.size - 1} Others"
        }
    }

    val sunnyShape = remember {
        GenericShape { size, _ ->
            val path = MaterialShapes.Cookie12Sided.toPath().asComposePath()
            val matrix = androidx.compose.ui.graphics.Matrix()
            matrix.scale(size.width, size.height)
            path.transform(matrix)
            addPath(path)
        }
    }

    val decorativeShape = remember {
        val availableShapes = listOf(
            MaterialShapes.Sunny,
            MaterialShapes.Slanted,
            MaterialShapes.Arch,
            MaterialShapes.Burst,
            MaterialShapes.Puffy,
            MaterialShapes.Bun,
            MaterialShapes.PixelCircle,
            MaterialShapes.Flower
        )
        val selected = availableShapes.random()
        GenericShape { size, _ ->
            val path = selected.toPath().asComposePath()
            val matrix = androidx.compose.ui.graphics.Matrix()
            matrix.scale(size.width, size.height)
            path.transform(matrix)
            addPath(path)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing)
        ),
        label = "rotationAngle"
    )

    val hintTransition = rememberInfiniteTransition(label = "hint")
    val hintPhase by hintTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing)
        ),
        label = "hintPhase"
    )

    val backgroundAlpha by infiniteTransition.animateFloat(
        initialValue = 0.04f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bgAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = backgroundAlpha),
                            Color.Transparent,
                            MaterialTheme.colorScheme.secondary.copy(alpha = backgroundAlpha)
                        )
                    )
                )
        )

        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1.2f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Ready to make some progress?",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    AppGoalMultiIconBox(
                        appInfos = appInfos,
                        size = 240.dp,
                        innerSize = 190.dp,
                        iconSize = 130.dp,
                        rotationAngle = rotationAngle,
                        decorativeShape = decorativeShape,
                        sunnyShape = sunnyShape
                    )
                }

                Spacer(modifier = Modifier.width(32.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AppGoalSnoozeButton(onSnooze = onSnooze)
                    Spacer(modifier = Modifier.height(32.dp))
                    AppGoalActionGroup(
                        hintPhase = hintPhase,
                        isMulti = packageNames.size > 1,
                        onHangUp = onHangUp,
                        onAnswer = {
                            if (packageNames.size > 1) {
                                showSelectionSheet = true
                            } else if (packageNames.isNotEmpty()) {
                                onAnswer(packageNames[0])
                            }
                        }
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(64.dp))
                
                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Ready to make some progress?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                AppGoalMultiIconBox(
                    appInfos = appInfos,
                    size = 320.dp,
                    innerSize = 260.dp,
                    iconSize = 180.dp,
                    rotationAngle = rotationAngle,
                    decorativeShape = decorativeShape,
                    sunnyShape = sunnyShape
                )

                Spacer(modifier = Modifier.weight(1.3f))

                AppGoalSnoozeButton(onSnooze = onSnooze)
                
                Spacer(modifier = Modifier.height(32.dp))

                AppGoalActionGroup(
                    hintPhase = hintPhase,
                    isMulti = packageNames.size > 1,
                    onHangUp = onHangUp,
                    onAnswer = {
                        if (packageNames.size > 1) {
                            showSelectionSheet = true
                        } else if (packageNames.isNotEmpty()) {
                            onAnswer(packageNames[0])
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showSelectionSheet) {
        AppGoalSelectionBottomSheet(
            packageNames = packageNames,
            onAppSelected = { pkg ->
                showSelectionSheet = false
                onAnswer(pkg)
            },
            onDismiss = { showSelectionSheet = false }
        )
    }
}

@Composable
private fun AppGoalMultiIconBox(
    appInfos: List<GoalAppInfo>,
    size: androidx.compose.ui.unit.Dp,
    innerSize: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp,
    rotationAngle: Float,
    decorativeShape: androidx.compose.ui.graphics.Shape,
    sunnyShape: androidx.compose.ui.graphics.Shape
) {
    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer { rotationZ = rotationAngle }
                .clip(decorativeShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
        )
        Box(
            modifier = Modifier
                .size(innerSize)
                .graphicsLayer { rotationZ = -rotationAngle * 0.7f }
                .clip(decorativeShape)
                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f))
        )
        
        Box(
            modifier = Modifier
                .size(iconSize)
                .graphicsLayer { rotationZ = rotationAngle * 0.4f }
                .clip(sunnyShape),
            contentAlignment = Alignment.Center
        ) {
            if (appInfos.size > 1) {
                Box(contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = appInfos[0].icon,
                        contentDescription = null,
                        modifier = Modifier
                            .size(iconSize * 0.75f)
                            .offset(x = (-10).dp, y = (-10).dp)
                            .clip(CircleShape)
                            .graphicsLayer { rotationZ = -rotationAngle * 0.4f }
                    )
                    AsyncImage(
                        model = appInfos.getOrNull(1)?.icon,
                        contentDescription = null,
                        modifier = Modifier
                            .size(iconSize * 0.75f)
                            .offset(x = 10.dp, y = 10.dp)
                            .clip(CircleShape)
                            .graphicsLayer { rotationZ = -rotationAngle * 0.4f }
                    )
                }
            } else if (appInfos.isNotEmpty()) {
                AsyncImage(
                    model = appInfos[0].icon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(iconSize)
                        .graphicsLayer { rotationZ = -rotationAngle * 0.4f }
                )
            }
        }
    }
}

@Composable
private fun AppGoalSnoozeButton(onSnooze: () -> Unit) {
    Surface(
        onClick = onSnooze,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Snooze, 
                contentDescription = null, 
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Snooze for 5 minutes", 
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun AppGoalActionGroup(
    hintPhase: Float,
    isMulti: Boolean,
    onHangUp: () -> Unit,
    onAnswer: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainer),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val leftWeight by animateFloatAsState(
            targetValue = if (hintPhase < 1f) 1.25f + (0.15f * kotlin.math.sin(hintPhase * kotlin.math.PI.toFloat())) else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
            label = "leftWeight"
        )
        
        Box(
            modifier = Modifier
                .weight(leftWeight)
                .fillMaxHeight()
                .clip(RoundedCornerShape(topStart = 42.dp, bottomStart = 42.dp, topEnd = 10.dp, bottomEnd = 10.dp))
                .background(MaterialTheme.colorScheme.errorContainer)
                .clickable(onClick = onHangUp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CallEnd, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Hang up", 
                    fontWeight = FontWeight.Bold, 
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        val rightWeight by animateFloatAsState(
            targetValue = if (hintPhase >= 1f) 1.25f + (0.15f * kotlin.math.sin((hintPhase - 1f) * kotlin.math.PI.toFloat())) else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
            label = "rightWeight"
        )

        Box(
            modifier = Modifier
                .weight(rightWeight)
                .fillMaxHeight()
                .clip(RoundedCornerShape(topEnd = 42.dp, bottomEnd = 42.dp, topStart = 10.dp, bottomStart = 10.dp))
                .background(MaterialTheme.colorScheme.primary)
                .clickable(onClick = onAnswer),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Call, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (isMulti) "Pick App" else "Answer", 
                    fontWeight = FontWeight.ExtraBold, 
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}
