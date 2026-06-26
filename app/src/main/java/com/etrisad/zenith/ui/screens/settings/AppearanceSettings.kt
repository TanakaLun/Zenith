package com.etrisad.zenith.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.etrisad.zenith.R
import com.etrisad.zenith.data.preferences.FontOption
import com.etrisad.zenith.data.preferences.ThemeConfig
import com.etrisad.zenith.data.preferences.UserPreferences

@Composable
fun AppearanceSettings(
    preferences: UserPreferences,
    onThemeChange: (ThemeConfig) -> Unit,
    onFontChange: (FontOption) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onExpressiveColorsChange: (Boolean) -> Unit,
    onFloatingTabBarEnabledChange: (Boolean) -> Unit,
    onNavigateToGSFlexCustomizer: () -> Unit,
    onNavigateToOverlayAppearance: () -> Unit
) {
    Column {
        PreferenceCategory(title = stringResource(R.string.settings_theming))

        ThemeSelector(
            selectedTheme = preferences.themeConfig,
            onThemeChange = onThemeChange,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))
        FontSelector(
            selectedFont = preferences.fontOption,
            onFontChange = onFontChange,
            onCustomizeGSFlex = onNavigateToGSFlexCustomizer,
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))
        SettingsToggle(
            title = stringResource(R.string.dynamic_color),
            description = stringResource(R.string.dynamic_color_desc),
            checked = preferences.dynamicColor,
            onCheckedChange = onDynamicColorChange,
            icon = Icons.Outlined.Palette,
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))
        SettingsToggle(
            title = stringResource(R.string.expressive_color_set),
            description = stringResource(R.string.expressive_color_set_desc),
            checked = preferences.expressiveColors,
            onCheckedChange = onExpressiveColorsChange,
            icon = Icons.Outlined.Layers,
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))
        PreferenceCategory(title = stringResource(R.string.settings_layout_and_navigation))

        SettingsToggle(
            title = stringResource(R.string.floating_tab_bar),
            description = stringResource(R.string.floating_tab_bar_desc),
            checked = preferences.floatingTabBarEnabled,
            onCheckedChange = onFloatingTabBarEnabledChange,
            icon = Icons.Outlined.Flaky,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))
        SettingsActionItem(
            title = stringResource(R.string.overlay_appearance),
            summary = stringResource(R.string.overlay_appearance_desc),
            onClick = onNavigateToOverlayAppearance,
            icon = Icons.Outlined.Style,
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
        )
    }
}
