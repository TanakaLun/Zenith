package com.etrisad.zenith.ui.screens.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.etrisad.zenith.R

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.ui.text.ExperimentalTextApi::class)
@Composable
fun FontTestScreen(
    onBack: () -> Unit
) {
    var weight by remember { mutableFloatStateOf(400f) }
    var width by remember { mutableFloatStateOf(100f) }
    var opticalSize by remember { mutableFloatStateOf(12f) }
    var grade by remember { mutableFloatStateOf(0f) }
    var slant by remember { mutableFloatStateOf(0f) }
    var roundness by remember { mutableFloatStateOf(0f) }

    val animatedWeight by animateFloatAsState(targetValue = weight, animationSpec = spring(), label = "weight")
    val animatedWidth by animateFloatAsState(targetValue = width, animationSpec = spring(), label = "width")
    val animatedOpsz by animateFloatAsState(targetValue = opticalSize, animationSpec = spring(), label = "opsz")
    val animatedGrade by animateFloatAsState(targetValue = grade, animationSpec = spring(), label = "grade")
    val animatedSlant by animateFloatAsState(targetValue = slant, animationSpec = spring(), label = "slant")
    val animatedRoundness by animateFloatAsState(targetValue = roundness, animationSpec = spring(), label = "roundness")

    val dynamicFontFamily = remember(
        animatedWeight, animatedWidth, animatedOpsz, 
        animatedGrade, animatedSlant, animatedRoundness
    ) {
        FontFamily(
            Font(
                resId = R.font.google_sans_flex_variable,
                variationSettings = FontVariation.Settings(
                    FontVariation.weight(animatedWeight.toInt().coerceIn(1, 1000)),
                    FontVariation.width(animatedWidth.coerceAtLeast(0.1f)),
                    FontVariation.Setting("opsz", animatedOpsz),
                    FontVariation.grade(animatedGrade.toInt()),
                    FontVariation.slant(animatedSlant),
                    FontVariation.Setting("ROND", animatedRoundness)
                )
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GS Flex Local Variable") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Zenith",
                        style = TextStyle(
                            fontFamily = dynamicFontFamily,
                            fontSize = 72.sp
                        )
                    )
                    
                    Text(
                        text = "Variable Font Showcase",
                        style = TextStyle(
                            fontFamily = dynamicFontFamily,
                            fontSize = 18.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = "The quick brown fox jumps over the lazy dog. 1234567890",
                style = TextStyle(
                    fontFamily = dynamicFontFamily,
                    fontSize = 20.sp
                )
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            VariableSlider(label = "Weight (wght)", value = weight, range = 100f..1000f, onValueChange = { weight = it })
            VariableSlider(label = "Width (wdth)", value = width, range = 25f..150f, onValueChange = { width = it })
            VariableSlider(label = "Optical Size (opsz)", value = opticalSize, range = 6f..72f, onValueChange = { opticalSize = it })
            VariableSlider(label = "Grade (GRAD)", value = grade, range = -200f..200f, onValueChange = { grade = it })
            VariableSlider(label = "Slant (slnt)", value = slant, range = -10f..0f, onValueChange = { slant = it })
            VariableSlider(label = "Roundness (ROND)", value = roundness, range = 0f..100f, onValueChange = { roundness = it })

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun VariableSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(
                text = String.format("%.1f", value),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range
        )
    }
}
