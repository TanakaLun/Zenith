package com.etrisad.zenith.ui.screens.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.etrisad.zenith.ui.components.overlay.ShieldOverlay

data class OverlayColorPalette(
    val id: String,
    val seed: Color,
    val label: String,
    val isDynamic: Boolean = false
)

@Composable
fun OverlayAppearanceScreen(
    onBack: () -> Unit,
    innerPadding: PaddingValues
) {
    val isDark = isSystemInDarkTheme()
    val currentScheme = MaterialTheme.colorScheme
    
    val predefinedPalettes = remember {
        listOf(
            OverlayColorPalette("dynamic", Color.Transparent, "Dynamic", isDynamic = true),
            OverlayColorPalette("custom", Color.Transparent, "Custom"),
            OverlayColorPalette("monochrome", Color(0xFF616161), "Monochrome"),
            OverlayColorPalette("zenith", Color(0xFF4D568D), "Zenith"),
            OverlayColorPalette("ocean", Color(0xFF0061A4), "Ocean"),
            OverlayColorPalette("forest", Color(0xFF386B01), "Forest"),
            OverlayColorPalette("rose", Color(0xFF9C4146), "Rose"),
            OverlayColorPalette("sunset", Color(0xFF825500), "Sunset"),
            OverlayColorPalette("lavender", Color(0xFF6750A4), "Lavender"),
            OverlayColorPalette("mint", Color(0xFF006B5D), "Mint")
        )
    }

    var selectedPaletteId by remember { mutableStateOf("dynamic") }
    var sheetOpacity by remember { mutableFloatStateOf(1f) }
    var fullScreen by remember { mutableStateOf(false) }
    var customHue by remember { mutableFloatStateOf(270f) }
    
    val customSeed = remember(customHue) {
        Color(android.graphics.Color.HSVToColor(floatArrayOf(customHue, 0.6f, 0.55f)))
    }

    val selectedPalette = remember(selectedPaletteId) {
        predefinedPalettes.find { it.id == selectedPaletteId } ?: predefinedPalettes.first()
    }

    val paletteScheme = remember(selectedPalette, isDark, currentScheme, customSeed) {
        when {
            selectedPalette.isDynamic -> currentScheme
            selectedPalette.id == "custom" -> generateDynamicColorScheme(customSeed, isDark, currentScheme)
            else -> generateDynamicColorScheme(selectedPalette.seed, isDark, currentScheme)
        }
    }

    val animatedSheetColor by animateColorAsState(
        targetValue = if (selectedPalette.id == "monochrome") {
            if (isDark) Color(0xFF1B1B1B) else Color(0xFFF5F5F5)
        } else {
            paletteScheme.surfaceContainer
        },
        label = "sheetColor"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = innerPadding.calculateTopPadding() + 16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        PreferenceCategory(title = "Preview")

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(560.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    MaterialTheme(colorScheme = paletteScheme) {
                        ShieldOverlay(
                            packageName = "com.example.app",
                            appName = "Example App",
                            shield = null,
                            totalUsageToday = 0L,
                            totalGlobalUsageToday = 0L,
                            previewMode = true,
                            maxHeightFraction = if (fullScreen) null else 0.9f,
                            sheetContentAlpha = sheetOpacity,
                            onAllowUse = { _, _ -> },
                            onCloseApp = { }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        PreferenceCategory(title = "Sheet Customization")

        ColorPaletteSelector(
            palettes = predefinedPalettes,
            selectedPaletteId = selectedPaletteId,
            onPaletteSelect = { selectedPaletteId = it },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp),
            isDark = isDark,
            currentScheme = currentScheme,
            customHue = customHue
        )

        AnimatedVisibility(
            visible = selectedPaletteId == "custom",
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
        ) {
            Column {
                Spacer(modifier = Modifier.height(4.dp))
                HueSelector(
                    hue = customHue,
                    onHueChange = { customHue = it },
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        OpacitySelector(
            opacity = sheetOpacity,
            onOpacityChange = { sheetOpacity = it },
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        SettingsToggle(
            title = "Full Screen",
            description = "Sheet fills the entire screen",
            checked = fullScreen,
            onCheckedChange = { fullScreen = it },
            icon = Icons.Outlined.Fullscreen,
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ColorPaletteSelector(
    palettes: List<OverlayColorPalette>,
    selectedPaletteId: String,
    onPaletteSelect: (String) -> Unit,
    shape: Shape,
    isDark: Boolean,
    currentScheme: ColorScheme,
    customHue: Float = 270f
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
                        imageVector = Icons.Outlined.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Color Theme",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Choose overlay base color palette",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(palettes) { palette ->
                    val circleScheme = remember(palette, isDark, currentScheme, customHue) {
                        if (palette.isDynamic) {
                            currentScheme.copy(
                                secondary = lightenColorHSV(currentScheme.secondary, if (isDark) 0.90f else 0.80f),
                                tertiary = lightenColorHSV(currentScheme.tertiary, if (isDark) 0.90f else 0.82f)
                            )
                        } else if (palette.id == "custom") {
                            val seed = Color(android.graphics.Color.HSVToColor(floatArrayOf(customHue, 0.6f, 0.55f)))
                            generateDynamicColorScheme(seed, isDark, currentScheme)
                        } else {
                            generateDynamicColorScheme(palette.seed, isDark, currentScheme)
                        }
                    }
                    
                    ColorPaletteCircle(
                        palette = palette,
                        scheme = circleScheme,
                        isSelected = selectedPaletteId == palette.id,
                        onClick = { onPaletteSelect(palette.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun ColorPaletteCircle(
    palette: OverlayColorPalette,
    scheme: ColorScheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val clickScale = remember { Animatable(1f) }

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.04f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    val ringAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessVeryLow),
        label = "ringAlpha"
    )

    val primary = scheme.primary
    val secondary = scheme.secondary
    val tertiary = scheme.tertiary

    val cornerSize by animateDpAsState(
        targetValue = if (isSelected) 24.dp else 35.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "corner"
    )

    val ringCornerSize by animateDpAsState(
        targetValue = if (isSelected) 20.dp else 35.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "ringCorner"
    )

    val circleCorner by animateDpAsState(
        targetValue = if (isSelected) 14.dp else 24.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "circleCorner"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                interactionSource = null,
                indication = null,
                onClick = {
                    scope.launch {
                        clickScale.animateTo(0.92f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                        clickScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                    }
                    onClick()
                }
            )
            .padding(vertical = 4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(70.dp)
                .graphicsLayer {
                    scaleX = clickScale.value
                    scaleY = clickScale.value
                }
                .clip(RoundedCornerShape(cornerSize))
        ) {
            // Adaptive Ring (Closer to the circle)
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (ringAlpha > 0.01f) {
                    val stroke = 2.5.dp.toPx()
                    val ringPad = 5.dp.toPx()
                    val ringSize = size.width - 2 * ringPad
                    val ringCorner = ringCornerSize.toPx()
                    drawRoundRect(
                        color = primary.copy(alpha = ringAlpha),
                        style = Stroke(width = stroke),
                        cornerRadius = CornerRadius(ringCorner, ringCorner),
                        topLeft = Offset(ringPad, ringPad),
                        size = Size(ringSize, ringSize)
                    )
                }
            }

            // Segmented Rounded Square
            Canvas(
                modifier = Modifier
                    .size(48.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
            ) {
                val cr = circleCorner.toPx()
                val w = size.width
                val h = size.height
                val halfW = w / 2f
                val halfH = h / 2f

                // Top segment: primary
                val topPath = Path().apply {
                    addRoundRect(RoundRect(0f, 0f, w, halfH, CornerRadius(cr), CornerRadius(cr), CornerRadius.Zero, CornerRadius.Zero))
                }
                drawPath(topPath, color = primary)

                // Bottom-left: secondary
                val blPath = Path().apply {
                    addRoundRect(RoundRect(0f, halfH, halfW, h, CornerRadius.Zero, CornerRadius.Zero, CornerRadius.Zero, CornerRadius(cr)))
                }
                drawPath(blPath, color = secondary)

                // Bottom-right: tertiary
                val brPath = Path().apply {
                    addRoundRect(RoundRect(halfW, halfH, w, h, CornerRadius.Zero, CornerRadius.Zero, CornerRadius(cr), CornerRadius.Zero))
                }
                drawPath(brPath, color = tertiary)
            }
            
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = scheme.onPrimary,
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer {
                            alpha = ringAlpha
                            scaleX = scale
                            scaleY = scale
                        }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = palette.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


@Composable
fun OpacitySelector(
    opacity: Float,
    onOpacityChange: (Float) -> Unit,
    shape: Shape
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
                        imageVector = Icons.Outlined.Opacity,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Opacity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Adjust sheet transparency",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Opacity, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Slider(
                    value = opacity,
                    onValueChange = onOpacityChange,
                    valueRange = 0f..1f,
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
                )
                Icon(
                    Icons.Outlined.Contrast, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = "Opacity: ${(opacity * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun HueSelector(
    hue: Float,
    onHueChange: (Float) -> Unit,
    shape: Shape
) {
    val currentColor = remember(hue) {
        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
    }

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
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(currentColor)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Hue",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Adjust custom palette color",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .align(Alignment.CenterStart)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    val hueStops = listOf(0f, 60f, 120f, 180f, 240f, 300f, 360f).map { h ->
                        Color(android.graphics.Color.HSVToColor(floatArrayOf(h, 1f, 1f)))
                    }
                    val brush = Brush.horizontalGradient(hueStops, 0f, size.width)
                    val thumbPad = 2.dp.toPx()
                    val thumbX = thumbPad + (hue / 360f) * (size.width - 2 * thumbPad)
                    val gap = 8.dp.toPx()

                    if (thumbX - gap > 0f) {
                        clipRect(right = thumbX - gap) {
                            drawRect(brush = brush, size = size)
                        }
                    }
                    if (thumbX + gap < size.width) {
                        clipRect(left = thumbX + gap) {
                            drawRect(brush = brush, size = size)
                        }
                    }
                }

                Slider(
                    value = hue,
                    onValueChange = onHueChange,
                    valueRange = 0f..360f,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = currentColor,
                        activeTrackColor = Color.Transparent,
                        inactiveTrackColor = Color.Transparent
                    )
                )
            }

            Text(
                text = "Hue: ${hue.toInt()}°",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

private fun generateDynamicColorScheme(seed: Color, isDark: Boolean, currentScheme: ColorScheme): ColorScheme {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(seed.toArgb(), hsv)
    
    val hue = hsv[0]
    val sat = hsv[1]
    
    // Check if the color is neutral (monochrome)
    val isMonochrome = sat < 0.05f
    
    // M3 Tonal Palette Approximations (Light: Tone 40, Dark: Tone 80)
    // Primary
    val primaryV = if (isMonochrome) {
        if (isDark) 0.95f else 0.1f
    } else {
        if (isDark) 0.82f else 0.50f
    }
    val primarySat = if (isMonochrome) 0f else sat.coerceIn(0.1f, 0.8f)
    val primary = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, primarySat, primaryV)))
    
    // Secondary: Same hue, lower chroma
    val secondaryV = if (isMonochrome) {
        if (isDark) 0.7f else 0.8f
    } else {
        if (isDark) 0.90f else 0.78f
    }
    val secondarySat = if (isMonochrome) 0f else (sat * 0.2f).coerceAtMost(0.15f)
    val secondary = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, secondarySat, secondaryV)))
    
    // Tertiary: Hue shift + 60 (unless monochrome)
    val tertiaryV = if (isMonochrome) {
        if (isDark) 0.4f else 0.5f
    } else {
        if (isDark) 0.88f else 0.75f
    }
    val tertiaryHue = if (isMonochrome) hue else (hue + 60f) % 360f
    val tertiarySat = if (isMonochrome) 0f else (sat * 0.4f).coerceIn(0.1f, 0.4f)
    val tertiary = Color(android.graphics.Color.HSVToColor(floatArrayOf(tertiaryHue, tertiarySat, tertiaryV)))

    val surfaceV = if (isDark) 0.05f else 0.98f
    val surfaceSat = if (isMonochrome) 0f else (sat * 0.05f).coerceAtMost(0.04f)
    val surface = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, surfaceSat, surfaceV)))
    
    val surfaceContainerLowest = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, if (isMonochrome) 0f else sat * 0.02f, if (isDark) 0.02f else 1.0f)))
    val surfaceContainerLow = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, if (isMonochrome) 0f else sat * 0.04f, if (isDark) 0.08f else 0.96f)))
    val surfaceContainer = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, if (isMonochrome) 0f else sat * 0.06f, if (isDark) 0.12f else 0.94f)))
    val surfaceContainerHigh = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, if (isMonochrome) 0f else sat * 0.08f, if (isDark) 0.17f else 0.9f)))

    return currentScheme.copy(
        primary = primary,
        onPrimary = if (isDark) Color.Black else Color.White,
        primaryContainer = primary.copy(alpha = if (isDark) 0.3f else 0.15f),
        onPrimaryContainer = primary,
        
        secondary = secondary,
        onSecondary = if (isDark) Color.Black else Color.White,
        secondaryContainer = secondary.copy(alpha = if (isDark) 0.25f else 0.2f),
        onSecondaryContainer = secondary,
        
        tertiary = tertiary,
        onTertiary = if (isDark) Color.Black else Color.White,
        tertiaryContainer = tertiary.copy(alpha = if (isDark) 0.25f else 0.14f),
        onTertiaryContainer = tertiary,

        surface = surface,
        onSurface = if (isDark) Color.White else Color.Black,
        surfaceVariant = surfaceContainer,
        onSurfaceVariant = if (isDark) Color.LightGray else Color.DarkGray,
        surfaceContainerLowest = surfaceContainerLowest,
        surfaceContainerLow = surfaceContainerLow,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHigh.copy(alpha = 0.9f)
    )
}

private fun lightenColorHSV(color: Color, targetV: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    hsv[2] = targetV
    return Color(android.graphics.Color.HSVToColor(hsv))
}


