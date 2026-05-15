package com.etrisad.zenith.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.appwidget.AppWidgetManager
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.text.TextAlign
import com.etrisad.zenith.R
import com.etrisad.zenith.ZenithApplication
import androidx.glance.Image
import androidx.glance.ColorFilter
import androidx.glance.unit.ColorProvider
import android.os.Build
import androidx.core.graphics.drawable.toBitmap
import android.graphics.*
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import androidx.glance.currentState
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.Preferences
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import com.etrisad.zenith.MainActivity

class AppStreakWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    companion object {
        val SELECTED_PACKAGE_KEY = stringPreferencesKey("selected_package")
        val EXTRA_WIDGET_ID_KEY = ActionParameters.Key<Int>(AppWidgetManager.EXTRA_APPWIDGET_ID)
        val PACKAGE_NAME_KEY = ActionParameters.Key<String>("package_name")
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as ZenithApplication
        val sunnyBitmap = createShapeBitmap(context, 48, MaterialShapes.Sunny)
        val cookieBitmap = createShapeBitmap(context, 48, MaterialShapes.Cookie12Sided)
        val pillBitmap = createShapeBitmap(context, 200, MaterialShapes.Pill)
        val circleBitmap = createShapeBitmap(context, 100, MaterialShapes.Circle)

        provideContent {
            val prefs = currentState<Preferences>()
            val selectedPackage = prefs[SELECTED_PACKAGE_KEY]
            
            val shields by app.shieldRepository.allShields.collectAsState(initial = emptyList())

            val packageToDisplay = if (!selectedPackage.isNullOrEmpty()) {
                selectedPackage
            } else {
                shields.filter { it.currentStreak > 0 }.maxByOrNull { it.currentStreak }?.packageName
            }
            
            val targetShield = shields.find { it.packageName == packageToDisplay }
            
            val iconBitmap = packageToDisplay?.let {
                try {
                    val original = context.packageManager.getApplicationIcon(it).toBitmap()
                    createShapeBitmap(context, 48, MaterialShapes.Cookie12Sided, sourceBitmap = original)
                } catch (_: Exception) {
                    null
                }
            }

            GlanceTheme {
                val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)

                Box(
                    modifier = GlanceModifier.fillMaxSize()
                        .cornerRadius(24.dp)
                        .clickable(
                            if (!packageToDisplay.isNullOrEmpty()) {
                                actionStartActivity<MainActivity>(
                                    actionParametersOf(PACKAGE_NAME_KEY.to(packageToDisplay))
                                )
                            } else {
                                actionStartActivity<AppStreakWidgetConfigurationActivity>(
                                    if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                                        actionParametersOf(EXTRA_WIDGET_ID_KEY.to(appWidgetId))
                                    } else actionParametersOf()
                                )
                            }
                        )
                ) {
                    if (!packageToDisplay.isNullOrEmpty()) {
                        AppStreakContent(
                            icon = iconBitmap,
                            currentStreak = targetShield?.currentStreak ?: 0,
                            bestStreak = targetShield?.bestStreak ?: 0,
                            sunnyBitmap = sunnyBitmap,
                            cookieBitmap = cookieBitmap,
                            pillBitmap = pillBitmap,
                            circleBitmap = circleBitmap
                        )
                    } else {
                        Box(
                            modifier = GlanceModifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                provider = ImageProvider(pillBitmap),
                                contentDescription = null,
                                modifier = GlanceModifier.fillMaxSize(),
                                colorFilter = ColorFilter.tint(GlanceTheme.colors.surface)
                            )
                            Text(
                                text = "Tap to select app",
                                style = TextStyle(color = GlanceTheme.colors.secondary)
                            )
                        }
                    }
                }
            }
        }
    }

    private fun createShapeBitmap(
        context: Context,
        sizeDp: Int,
        shape: RoundedPolygon,
        alpha: Int = 255,
        sourceBitmap: Bitmap? = null
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val sizePx = (sizeDp * density).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val path = shape.toPath()
        val matrix = Matrix()
        matrix.setScale(sizePx.toFloat(), sizePx.toFloat())
        path.transform(matrix)

        val paint = Paint().apply {
            color = Color.WHITE
            this.alpha = alpha
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        canvas.drawPath(path, paint)

        sourceBitmap?.let {
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(it, null, Rect(0, 0, sizePx, sizePx), paint)
        }

        return bitmap
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @SuppressLint("RestrictedApi")
    @Composable
    private fun AppStreakContent(
        icon: Bitmap?,
        currentStreak: Int,
        bestStreak: Int,
        sunnyBitmap: Bitmap,
        cookieBitmap: Bitmap,
        pillBitmap: Bitmap,
        circleBitmap: Bitmap
    ) {
        val size = LocalSize.current
        val squareSize = minOf(size.width, size.height)
        
        val scaleFactor = squareSize.value / 100f
        val containerSize = (44 * scaleFactor).dp
        val iconSize = (44 * scaleFactor).dp
        
        val currentStreakStr = currentStreak.toString()
        val mainFontSize = when {
            currentStreakStr.length >= 4 -> (8 * scaleFactor).sp
            currentStreakStr.length == 3 -> (11 * scaleFactor).sp
            currentStreakStr.length == 2 -> (14 * scaleFactor).sp
            else -> (18 * scaleFactor).sp
        }

        val fireSize = (12 * scaleFactor).dp
        val contentPadding = (12 * scaleFactor).dp

        val bestGemSize = (28 * scaleFactor).dp
        val bestStreakStr = bestStreak.toString()
        val bestPillFontSize = when {
            bestStreakStr.length >= 4 -> (7 * scaleFactor).sp
            bestStreakStr.length == 3 -> (8 * scaleFactor).sp
            else -> (10 * scaleFactor).sp
        }
        val bestIconSize = (10 * scaleFactor).dp

        val pillColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ColorProvider(resId = android.R.color.system_accent2_50)
        } else {
            GlanceTheme.colors.secondaryContainer
        }

        Box(
            modifier = GlanceModifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = GlanceModifier.size(squareSize),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(pillBitmap),
                    contentDescription = null,
                    modifier = GlanceModifier.fillMaxSize(),
                    colorFilter = ColorFilter.tint(pillColor)
                )

                Box(
                    modifier = GlanceModifier.fillMaxSize().padding(contentPadding),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            provider = ImageProvider(cookieBitmap),
                            contentDescription = null,
                            modifier = GlanceModifier.size(containerSize),
                            colorFilter = ColorFilter.tint(GlanceTheme.colors.secondaryContainer)
                        )
                        if (icon != null) {
                            Image(
                                provider = ImageProvider(icon),
                                contentDescription = null,
                                modifier = GlanceModifier.size(iconSize)
                            )
                        }
                    }
                }

                Box(
                    modifier = GlanceModifier.fillMaxSize().padding(contentPadding),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            provider = ImageProvider(sunnyBitmap),
                            contentDescription = null,
                            modifier = GlanceModifier.size(containerSize),
                            colorFilter = ColorFilter.tint(GlanceTheme.colors.primary)
                        )
                        Column(
                            modifier = GlanceModifier.size(containerSize).padding(top = (4 * scaleFactor).dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                provider = ImageProvider(R.drawable.ic_fire_department_outlined),
                                contentDescription = null,
                                modifier = GlanceModifier.size(fireSize),
                                colorFilter = ColorFilter.tint(GlanceTheme.colors.primaryContainer)
                            )
                            Text(
                                text = currentStreak.toString(),
                                style = TextStyle(
                                    fontSize = mainFontSize,
                                    fontWeight = FontWeight.Medium,
                                    color = GlanceTheme.colors.primaryContainer,
                                    textAlign = TextAlign.Center
                                )
                            )
                        }
                    }
                }

                Box(
                    modifier = GlanceModifier.fillMaxSize()
                        .padding(bottom = contentPadding - (6 * scaleFactor).dp, end = (2 * scaleFactor).dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            provider = ImageProvider(circleBitmap),
                            contentDescription = null,
                            modifier = GlanceModifier.size(bestGemSize),
                            colorFilter = ColorFilter.tint(GlanceTheme.colors.tertiary)
                        )
                        Column(
                            modifier = GlanceModifier.size(bestGemSize).padding(top = (2 * scaleFactor).dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                provider = ImageProvider(R.drawable.ic_crown),
                                contentDescription = null,
                                modifier = GlanceModifier.size(bestIconSize),
                                colorFilter = ColorFilter.tint(GlanceTheme.colors.onTertiary)
                            )
                            Text(
                                text = bestStreak.toString(),
                                style = TextStyle(
                                    fontSize = bestPillFontSize,
                                    fontWeight = FontWeight.Medium,
                                    color = GlanceTheme.colors.onTertiary,
                                    textAlign = TextAlign.Center
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
