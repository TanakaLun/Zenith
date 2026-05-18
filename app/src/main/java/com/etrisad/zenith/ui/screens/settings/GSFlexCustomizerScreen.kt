package com.etrisad.zenith.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.SettingsSuggest
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.etrisad.zenith.data.preferences.GSFlexPreset
import com.etrisad.zenith.data.preferences.UserPreferencesRepository
import com.etrisad.zenith.ui.components.ZenithToggleButtonGroup
import com.etrisad.zenith.ui.components.ZenithToggleOption
import com.etrisad.zenith.ui.components.ZenithButtonSize
import com.etrisad.zenith.ui.theme.FontAxes
import com.etrisad.zenith.ui.theme.GSFlexSettings
import com.etrisad.zenith.ui.theme.VariableFontFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GSFlexCustomizerScreen(
    repository: UserPreferencesRepository,
    onBack: () -> Unit
) {
    val preferences by repository.userPreferencesFlow.collectAsState(initial = com.etrisad.zenith.data.preferences.UserPreferences())
    val scope = rememberCoroutineScope()

    var tempSettings by remember(preferences.gsFlexSettings) { mutableStateOf(preferences.gsFlexSettings) }
    var selectedLevelTab by remember { mutableIntStateOf(0) }

    val previewTypography = VariableFontFactory.createTypography(tempSettings)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GS Flex Designer") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } },
                actions = {
                    Button(
                        onClick = { scope.launch { repository.setGSFlexSettings(tempSettings); onBack() } },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Outlined.Check, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Apply")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp).clip(RoundedCornerShape(32.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Zenith", style = previewTypography.displayLarge.copy(fontSize = 64.sp), color = MaterialTheme.colorScheme.primary)
                    Text("Headline Treatment", style = previewTypography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text("Standard body text for optimized reading experience in the Zenith app.", style = previewTypography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp), textAlign = TextAlign.Center)
                }
            }

            Column(modifier = Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Design System Presets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                val presetOptions = listOf(GSFlexPreset.ZENITH to "Zenith", GSFlexPreset.NEO to "Neo", GSFlexPreset.COMPACT to "Impact", GSFlexPreset.AIRY to "Airy", GSFlexPreset.CUSTOM to "Custom")
                ZenithToggleButtonGroup(
                    options = presetOptions.map { ZenithToggleOption(text = it.second) },
                    selectedIndices = setOf(presetOptions.indexOfFirst { it.first == tempSettings.preset }),
                    onToggle = { tempSettings = tempSettings.copy(preset = presetOptions[it].first) },
                    size = ZenithButtonSize.Medium, isInsideContainer = true
                )
            }

            AnimatedVisibility(visible = tempSettings.preset == GSFlexPreset.CUSTOM, modifier = Modifier.animateContentSize()) {
                Column(modifier = Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.SettingsSuggest, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Fine-tune Axes per Level", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    PrimaryTabRow(selectedTabIndex = selectedLevelTab, containerColor = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.clip(RoundedCornerShape(16.dp))) {
                        Tab(selected = selectedLevelTab == 0, onClick = { selectedLevelTab = 0 }, text = { Text("Display") })
                        Tab(selected = selectedLevelTab == 1, onClick = { selectedLevelTab = 1 }, text = { Text("Headline") })
                        Tab(selected = selectedLevelTab == 2, onClick = { selectedLevelTab = 2 }, text = { Text("Body") })
                    }
                    
                    val currentAxes = when(selectedLevelTab) { 0 -> tempSettings.display; 1 -> tempSettings.headline; else -> tempSettings.body }
                    
                    fun updateAxes(newAxes: FontAxes) {
                        tempSettings = when(selectedLevelTab) {
                            0 -> tempSettings.copy(display = newAxes)
                            1 -> tempSettings.copy(headline = newAxes)
                            else -> tempSettings.copy(body = newAxes)
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        VariableSlider("Weight", currentAxes.weight, 100f..1000f) { updateAxes(currentAxes.copy(weight = it)) }
                        VariableSlider("Width", currentAxes.width, 25f..150f) { updateAxes(currentAxes.copy(width = it)) }
                        VariableSlider("Optical Size", currentAxes.opsz, 6f..72f) { updateAxes(currentAxes.copy(opsz = it)) }
                        VariableSlider("Grade", currentAxes.grade, -200f..200f) { updateAxes(currentAxes.copy(grade = it)) }
                        VariableSlider("Slant", currentAxes.slant, -10f..0f) { updateAxes(currentAxes.copy(slant = it)) }
                        VariableSlider("Roundness", currentAxes.roundness, 0f..100f) { updateAxes(currentAxes.copy(roundness = it)) }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun VariableSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(String.format(java.util.Locale.getDefault(), "%.0f", value), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = range, colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary, inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant))
    }
}
