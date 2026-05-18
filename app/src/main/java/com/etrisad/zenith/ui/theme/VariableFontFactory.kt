package com.etrisad.zenith.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.etrisad.zenith.R
import com.etrisad.zenith.data.preferences.GSFlexPreset

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
data class FontAxes(
    val weight: Float,
    val width: Float,
    val opsz: Float,
    val grade: Float,
    val slant: Float,
    val roundness: Float
) {
    fun toVariationSettings() = FontVariation.Settings(
        FontVariation.weight(weight.toInt().coerceIn(1, 1000)),
        FontVariation.width(width.coerceIn(25f, 150f)),
        FontVariation.Setting("opsz", opsz.coerceIn(6f, 72f)),
        FontVariation.grade(grade.toInt().coerceIn(-200, 200)),
        FontVariation.slant(slant.coerceIn(-10f, 0f)),
        FontVariation.Setting("ROND", roundness.coerceIn(0f, 100f))
    )
}

data class GSFlexSettings(
    val preset: GSFlexPreset = GSFlexPreset.ZENITH,
    val display: FontAxes = FontAxes(400f, 100f, 72f, 0f, 0f, 0f),
    val headline: FontAxes = FontAxes(400f, 100f, 32f, 0f, 0f, 0f),
    val body: FontAxes = FontAxes(400f, 100f, 16f, 0f, 0f, 0f)
)

object VariableFontFactory {
    
    fun createTypography(settings: GSFlexSettings): Typography {
        if (settings.preset != GSFlexPreset.CUSTOM) {
            val p = getPresetAxes(settings.preset)
            return createExpressiveTypography(p.first, p.second, p.third)
        }
        return createExpressiveTypography(
            settings.display.toVariationSettings(),
            settings.headline.toVariationSettings(),
            settings.body.toVariationSettings()
        )
    }

    private fun getPresetAxes(preset: GSFlexPreset): Triple<FontVariation.Settings, FontVariation.Settings, FontVariation.Settings> {
        return when (preset) {
            GSFlexPreset.ZENITH -> Triple(
                FontVariation.Settings(FontVariation.weight(950), FontVariation.width(85f), FontVariation.Setting("opsz", 72f), FontVariation.Setting("ROND", 100f)),
                FontVariation.Settings(FontVariation.weight(700), FontVariation.width(115f), FontVariation.Setting("opsz", 32f), FontVariation.Setting("ROND", 60f)),
                FontVariation.Settings(FontVariation.weight(450), FontVariation.width(100f), FontVariation.Setting("opsz", 16f), FontVariation.grade(20), FontVariation.Setting("ROND", 0f))
            )
            GSFlexPreset.NEO -> Triple(
                FontVariation.Settings(FontVariation.weight(800), FontVariation.width(125f), FontVariation.Setting("opsz", 72f), FontVariation.Setting("ROND", 0f)),
                FontVariation.Settings(FontVariation.weight(600), FontVariation.width(100f), FontVariation.Setting("opsz", 32f), FontVariation.Setting("ROND", 0f)),
                FontVariation.Settings(FontVariation.weight(400), FontVariation.width(95f), FontVariation.Setting("opsz", 16f), FontVariation.grade(10), FontVariation.Setting("ROND", 0f))
            )
            GSFlexPreset.COMPACT -> Triple(
                FontVariation.Settings(FontVariation.weight(900), FontVariation.width(75f), FontVariation.Setting("opsz", 72f), FontVariation.Setting("ROND", 30f)),
                FontVariation.Settings(FontVariation.weight(800), FontVariation.width(85f), FontVariation.Setting("opsz", 32f), FontVariation.grade(50), FontVariation.Setting("ROND", 20f)),
                FontVariation.Settings(FontVariation.weight(500), FontVariation.width(90f), FontVariation.Setting("opsz", 16f), FontVariation.grade(30), FontVariation.Setting("ROND", 10f))
            )
            GSFlexPreset.AIRY -> Triple(
                FontVariation.Settings(FontVariation.weight(300), FontVariation.width(140f), FontVariation.Setting("opsz", 72f), FontVariation.Setting("ROND", 100f)),
                FontVariation.Settings(FontVariation.weight(500), FontVariation.width(120f), FontVariation.Setting("opsz", 32f), FontVariation.Setting("ROND", 100f)),
                FontVariation.Settings(FontVariation.weight(400), FontVariation.width(110f), FontVariation.Setting("opsz", 16f), FontVariation.Setting("ROND", 50f))
            )
            else -> getPresetAxes(GSFlexPreset.ZENITH)
        }
    }

    @OptIn(ExperimentalTextApi::class)
    private fun createExpressiveTypography(
        displaySettings: FontVariation.Settings,
        headlineSettings: FontVariation.Settings,
        bodySettings: FontVariation.Settings
    ): Typography {
        val displayFont = FontFamily(Font(resId = R.font.google_sans_flex_variable, variationSettings = displaySettings))
        val headlineFont = FontFamily(Font(resId = R.font.google_sans_flex_variable, variationSettings = headlineSettings))
        val bodyFont = FontFamily(Font(resId = R.font.google_sans_flex_variable, variationSettings = bodySettings))

        return Typography(
            displayLarge = TextStyle(fontFamily = displayFont, fontWeight = FontWeight.Normal, fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-0.25).sp),
            displayMedium = TextStyle(fontFamily = displayFont, fontWeight = FontWeight.Normal, fontSize = 45.sp, lineHeight = 52.sp, letterSpacing = 0.sp),
            displaySmall = TextStyle(fontFamily = displayFont, fontWeight = FontWeight.Normal, fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = 0.sp),
            headlineLarge = TextStyle(fontFamily = headlineFont, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = 0.sp),
            headlineMedium = TextStyle(fontFamily = headlineFont, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = 0.sp),
            headlineSmall = TextStyle(fontFamily = headlineFont, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = 0.sp),
            titleLarge = TextStyle(fontFamily = headlineFont, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.sp),
            titleMedium = TextStyle(fontFamily = headlineFont, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
            titleSmall = TextStyle(fontFamily = headlineFont, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
            bodyLarge = TextStyle(fontFamily = bodyFont, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
            bodyMedium = TextStyle(fontFamily = bodyFont, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
            bodySmall = TextStyle(fontFamily = bodyFont, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
            labelLarge = TextStyle(fontFamily = bodyFont, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
            labelMedium = TextStyle(fontFamily = bodyFont, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
            labelSmall = TextStyle(fontFamily = bodyFont, fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp)
        )
    }
}
