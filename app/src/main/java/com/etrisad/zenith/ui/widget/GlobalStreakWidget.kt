package com.etrisad.zenith.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Build
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import com.etrisad.zenith.R
import com.etrisad.zenith.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.first

class GlobalStreakWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = UserPreferencesRepository(context)
        val prefs = repository.userPreferencesFlow.first()
        
        // Generate bitmaps for custom shapes
        val sunnyBitmap = createShapeBitmap(context, 64, MaterialShapes.Sunny, 40)
        val backgroundBitmap = createShapeBitmap(context, 200, MaterialShapes.Cookie12Sided)

        provideContent {
            GlanceTheme {
                GlobalStreakContent(
                    currentStreak = prefs.globalCurrentStreak,
                    bestStreak = prefs.globalBestStreak,
                    sunnyBitmap = sunnyBitmap,
                    backgroundBitmap = backgroundBitmap
                )
            }
        }
    }

    private fun createShapeBitmap(
        context: Context,
        sizeDp: Int,
        shape: RoundedPolygon,
        alpha: Int = 255
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
        return bitmap
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @SuppressLint("RestrictedApi")
    @Composable
    private fun GlobalStreakContent(
        currentStreak: Int,
        bestStreak: Int,
        sunnyBitmap: Bitmap,
        backgroundBitmap: Bitmap
    ) {
        val backgroundColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ColorProvider(resId = android.R.color.system_accent2_50)
        } else {
            GlanceTheme.colors.secondaryContainer
        }

        Box(
            modifier = GlanceModifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Background Shape (Cookie12Sided)
            Image(
                provider = ImageProvider(backgroundBitmap),
                contentDescription = null,
                modifier = GlanceModifier.fillMaxSize(),
                colorFilter = ColorFilter.tint(backgroundColor)
            )

            Column(
                modifier = GlanceModifier.fillMaxSize().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        provider = ImageProvider(sunnyBitmap),
                        contentDescription = null,
                        modifier = GlanceModifier.size(64.dp),
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.secondary)
                    )
                    Image(
                        provider = ImageProvider(R.drawable.ic_fire_department_outlined),
                        contentDescription = null,
                        modifier = GlanceModifier.size(26.dp),
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.secondary)
                    )
                }
                
                Spacer(modifier = GlanceModifier.height(2.dp))
                
                Text(
                    text = currentStreak.toString(),
                    style = TextStyle(
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.secondary
                    )
                )
                
                Spacer(modifier = GlanceModifier.height(4.dp))
                
                Row(
                    modifier = GlanceModifier
                        .background(GlanceTheme.colors.primary)
                        .cornerRadius(100.dp)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Best : $bestStreak",
                        style = TextStyle(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = GlanceTheme.colors.onPrimary
                        )
                    )
                }
            }
        }
    }
}
