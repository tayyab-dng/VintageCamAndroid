package com.example.vintagecam.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vintagecam.camera.CameraUiState
import com.example.vintagecam.camera.VintageAspectRatio
import com.example.vintagecam.camera.VintageFlashMode
import com.example.vintagecam.theme.Concept46IndicatorRed
import com.example.vintagecam.theme.Concept46OrangeGlow
import com.example.vintagecam.theme.Concept46TitaniumTop
import com.example.vintagecam.theme.VintageAmber
import com.example.vintagecam.theme.VintageBlack
import com.example.vintagecam.theme.VintageGold
import com.example.vintagecam.theme.VintageMetalDark
import com.example.vintagecam.theme.VintageMetalSilver
import com.example.vintagecam.theme.VintageTextLight
import com.example.vintagecam.theme.VintageTextMuted
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Matte Titanium/Metallic top strip housing discrete metadata in a retro monospace font:
 * 'EXP 03/36', 'RETRO 1980s • 3:2 • ±0.0 EV', and a minimalist calibration meter.
 */
@Composable
fun TopTitaniumBar(
    state: CameraUiState,
    onFlashToggle: () -> Unit,
    onAspectRatioToggle: () -> Unit,
    onGridToggle: () -> Unit,
    onDateStampToggle: () -> Unit,
    onFilmPresetClick: () -> Unit
) {
    val formattedExp = remember(state.frameCounter, state.maxFrames) {
        String.format(Locale.US, "EXP %02d/%02d", state.frameCounter, state.maxFrames)
    }

    val evString = remember(state.evValue) {
        if (state.evValue > 0f) String.format(Locale.US, "+%.1f EV", state.evValue)
        else if (state.evValue < 0f) String.format(Locale.US, "%.1f EV", state.evValue)
        else "±0.0 EV"
    }

    val metadataText = remember(state.filmPreset, state.aspectRatio, evString) {
        "${state.filmPreset.title} • ${state.aspectRatio.title} • $evString"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF35373A),
                        Concept46TitaniumTop,
                        Color(0xFF232426)
                    )
                )
            )
            .border(width = 1.dp, color = Color(0xFF404245))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: EXP 03/36 counter badge in recessed metallic bevel
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF1B1C1E))
                    .border(1.dp, Color(0xFF4A4D52), RoundedCornerShape(3.dp))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    text = formattedExp,
                    style = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    ),
                    color = VintageAmber
                )
            }

            // Center: Discrete metadata (RETRO 1980s • 3:2 • ±0.0 EV)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onFilmPresetClick() }
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = metadataText,
                    style = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp
                    ),
                    color = VintageTextLight
                )
            }

            // Right: Minimalist calibration meter + quick hardware icons
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Micro 4-bar calibration meter
                MinimalCalibrationMeter(evValue = state.evValue)

                Spacer(modifier = Modifier.width(4.dp))

                // Aspect Ratio Quick Toggle
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF222326))
                        .border(1.dp, Color(0xFF404246), RoundedCornerShape(3.dp))
                        .clickable { onAspectRatioToggle() }
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = state.aspectRatio.title,
                        style = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = VintageGold
                    )
                }

                // Grid Guide Toggle
                IconButton(
                    onClick = onGridToggle,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GridOn,
                        contentDescription = "Toggle Grid",
                        tint = if (state.isGridVisible) VintageAmber else Color(0xFF6E7177),
                        modifier = Modifier.size(15.dp)
                    )
                }

                // Flash Toggle
                IconButton(
                    onClick = onFlashToggle,
                    modifier = Modifier.size(28.dp)
                ) {
                    val (flashIcon, flashTint) = when (state.flashMode) {
                        VintageFlashMode.AUTO -> Pair(Icons.Default.FlashAuto, VintageAmber)
                        VintageFlashMode.ON -> Pair(Icons.Default.FlashOn, VintageGold)
                        VintageFlashMode.OFF -> Pair(Icons.Default.FlashOff, Color(0xFF6E7177))
                    }
                    Icon(
                        imageVector = flashIcon,
                        contentDescription = "Flash Mode",
                        tint = flashTint,
                        modifier = Modifier.size(15.dp)
                    )
                }

                // Date Stamp Toggle
                IconButton(
                    onClick = onDateStampToggle,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Quartz Date Stamp",
                        tint = if (state.isDateStampEnabled) Color(0xFFFF7A00) else Color(0xFF6E7177),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * Minimalist analog calibration meter showing 5 discrete LED-style bars.
 */
@Composable
private fun MinimalCalibrationMeter(evValue: Float) {
    Canvas(modifier = Modifier.size(width = 24.dp, height = 12.dp)) {
        val barWidth = 3.dp.toPx()
        val spacing = 2.dp.toPx()
        val totalBars = 5
        val normIndex = ((evValue + 2.0f) / 4.0f * (totalBars - 1)).roundToInt().coerceIn(0, totalBars - 1)

        for (i in 0 until totalBars) {
            val x = i * (barWidth + spacing)
            val barHeight = when (i) {
                0, 4 -> size.height * 0.5f
                1, 3 -> size.height * 0.75f
                else -> size.height
            }
            val y = size.height - barHeight
            val color = when {
                i == normIndex -> Concept46OrangeGlow
                i == 2 -> Color(0xFF888A8E)
                else -> Color(0xFF383A3E)
            }
            drawRect(
                color = color,
                topLeft = Offset(x, y),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
            )
        }
    }
}

/**
 * Subtle horizontal slider for exposure fine-tuning at the top of the viewfinder.
 * Compact, tactile vernier scale spanning -2.0 EV to +2.0 EV in 1/3-stop steps.
 */
@Composable
fun ViewfinderExposureSlider(
    currentEv: Float,
    minEv: Float = -2.0f,
    maxEv: Float = 2.0f,
    onEvChanged: (Float) -> Unit,
    onTickHaptic: () -> Unit
) {
    val stops = remember {
        listOf(-2.0f, -1.67f, -1.33f, -1.0f, -0.67f, -0.33f, 0.0f, 0.33f, 0.67f, 1.0f, 1.33f, 1.67f, 2.0f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xD91E2022),
                        Color(0xB3151618)
                    )
                )
            )
            .border(width = 0.5.dp, color = Color(0x55404245))
            .pointerInput(currentEv) {
                detectHorizontalDragGestures { change, dragAmount ->
                    change.consume()
                    val totalWidth = size.width.toFloat()
                    if (totalWidth > 0f) {
                        val evRange = maxEv - minEv
                        val deltaEv = (dragAmount / totalWidth) * evRange * 1.5f
                        val newEv = (currentEv + deltaEv).coerceIn(minEv, maxEv)
                        val nearest = stops.minByOrNull { kotlin.math.abs(it - newEv) } ?: newEv
                        if (kotlin.math.abs(nearest - currentEv) > 0.15f) {
                            onTickHaptic()
                            onEvChanged(nearest)
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val fraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    val targetEv = minEv + fraction * (maxEv - minEv)
                    val nearest = stops.minByOrNull { kotlin.math.abs(it - targetEv) } ?: targetEv
                    onTickHaptic()
                    onEvChanged(nearest)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(18.dp)) {
            val totalWidth = size.width
            val midY = size.height / 2f
            val zeroX = totalWidth * (0.0f - minEv) / (maxEv - minEv)

            // Baseline hairline
            drawLine(
                color = Color(0x44FFFFFF),
                start = Offset(16f, midY),
                end = Offset(totalWidth - 16f, midY),
                strokeWidth = 1.dp.toPx()
            )

            // Scale tick marks
            stops.forEach { stop ->
                val frac = (stop - minEv) / (maxEv - minEv)
                val x = 16f + frac * (totalWidth - 32f)
                val isZero = stop == 0.0f
                val isInt = stop == -2.0f || stop == -1.0f || stop == 1.0f || stop == 2.0f
                val tickHeight = if (isZero) 14.dp.toPx() else if (isInt) 10.dp.toPx() else 6.dp.toPx()
                val tickColor = if (isZero) VintageGold else if (isInt) VintageMetalSilver else Color(0x66FFFFFF)

                drawLine(
                    color = tickColor,
                    start = Offset(x, midY - tickHeight / 2f),
                    end = Offset(x, midY + tickHeight / 2f),
                    strokeWidth = if (isZero) 2.dp.toPx() else 1.dp.toPx()
                )
            }

            // Current EV Indicator hairline (Concept 46 bright red)
            val currentFrac = (currentEv.coerceIn(minEv, maxEv) - minEv) / (maxEv - minEv)
            val indicatorX = 16f + currentFrac * (totalWidth - 32f)

            drawLine(
                color = Concept46IndicatorRed,
                start = Offset(indicatorX, 0f),
                end = Offset(indicatorX, size.height),
                strokeWidth = 2.dp.toPx()
            )

            // Glowing indicator dot on top
            drawCircle(
                color = Concept46IndicatorRed,
                radius = 3.dp.toPx(),
                center = Offset(indicatorX, size.height - 3.dp.toPx())
            )
        }
    }
}
