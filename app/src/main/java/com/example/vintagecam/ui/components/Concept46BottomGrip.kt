package com.example.vintagecam.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vintagecam.camera.CameraUiState
import com.example.vintagecam.theme.Concept46Aluminum
import com.example.vintagecam.theme.Concept46AluminumLight
import com.example.vintagecam.theme.Concept46BadgeDark
import com.example.vintagecam.theme.Concept46BadgeRed
import com.example.vintagecam.theme.Concept46Divider
import com.example.vintagecam.theme.Concept46IndicatorRed
import com.example.vintagecam.theme.Concept46KnurledMetal
import com.example.vintagecam.theme.Concept46LcdDark
import com.example.vintagecam.theme.Concept46OrangeGlow
import com.example.vintagecam.theme.Concept46RubberDark
import com.example.vintagecam.theme.VintageAmber
import com.example.vintagecam.theme.VintageGold
import com.example.vintagecam.theme.VintageMetalDark
import com.example.vintagecam.theme.VintageMetalSilver
import com.example.vintagecam.theme.VintageTextLight
import com.example.vintagecam.theme.VintageTextMuted
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * Asymmetric 'Concept 46' Bottom Camera Grip Panel.
 *
 * Implements the clean industrial camera backplate with:
 * 1. Sandblasted aluminum bevel rail and dark textured rubber base.
 * 2. Instrumentation Deck:
 *    - Top-left 'FVL' orange glow arc dial
 *    - Multi-colored stacked progress meter
 *    - Horizontal 'ruler' calibration gauge with bright red indicator
 *    - Dark telemetry readout display (SS, EV, ISO, vintage 'S' mode indicator)
 *    - Textured vertical 'P S' mechanical sliding switch & metallic button
 * 3. Operational Deck:
 *    - Recessed square photo-box with heavy physical inset frame
 *    - Pill-shaped 'SK' badge in dark gray and red
 *    - Large concentric silver-metallic grain shutter button
 *    - Lens flip toggle button
 */
@Composable
fun Concept46BottomGrip(
    state: CameraUiState,
    onShutterClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onFilmTrayToggle: () -> Unit,
    onSwitchCamera: () -> Unit,
    onEvChanged: (Float) -> Unit,
    onTickHaptic: () -> Unit,
    onDialHaptic: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF26272A),
                        Concept46RubberDark,
                        Color(0xFF141517)
                    )
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Concept46Aluminum,
                        Color(0xFF3A3C40)
                    )
                ),
                shape = androidx.compose.ui.graphics.RectangleShape
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        // ==========================================
        // 1. UPPER INSTRUMENTATION DECK
        // ==========================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF18191B))
                .border(1.dp, Color(0xFF2F3135), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Group: FVL Glow Dial + Multi-color Meter
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Mechanical 'FVL' Orange Glow Arc Dial
                FvlGlowDial(zoomRatio = state.zoomRatio)

                // Multi-colored stacked progress meter
                MultiColorProgressMeter(evValue = state.evValue)
            }

            // Center: Horizontal 'Ruler' Calibration Gauge with Red Indicator
            HorizontalRulerGauge(
                currentEv = state.evValue,
                onEvChanged = onEvChanged,
                onTickHaptic = onTickHaptic,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )

            // Right Group: Dark Readout Display + Vertical P/S Switch
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Dark Readout Display (SS, EV, ISO, Vintage 'S')
                TelemetryReadoutDisplay(state = state)

                // Vertical 'P S' mechanical slider switch & screw
                VerticalSliderSwitch(onToggle = onDialHaptic)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ==========================================
        // 2. LOWER OPERATIONAL DECK
        // ==========================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Far Left: Recessed Square Photo-box with Heavy Inset Frame
            RecessedPhotoWell(
                thumbnail = state.lastThumbnail,
                onClick = onGalleryClick
            )

            // Pill-shaped 'SK' Logo/Badge
            SkBrandBadge(
                onClick = onFilmTrayToggle
            )

            // Center-Left: Large Concentric Silver-Metallic Shutter Button
            Concept46ShutterButton(
                isCapturing = state.isCapturing,
                onClick = onShutterClick
            )

            // Far Right: Machined Lens Switcher Button
            LensSwitcherButton(
                onClick = onSwitchCamera
            )
        }
    }
}

/**
 * Mechanical 'FVL' dial with incandescent orange glow arc gauge.
 */
@Composable
private fun FvlGlowDial(zoomRatio: Float) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(36.dp)
    ) {
        Text(
            text = "FVL",
            style = androidx.compose.ui.text.TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            color = Concept46OrangeGlow
        )
        Spacer(modifier = Modifier.height(2.dp))
        Canvas(modifier = Modifier.size(28.dp)) {
            val strokeW = 2.5.dp.toPx()
            val centerOffset = Offset(size.width / 2f, size.height / 2f)
            val radius = (size.width - strokeW) / 2f

            // Outer dial track
            drawCircle(
                color = Color(0xFF2B2D31),
                radius = radius,
                style = Stroke(width = strokeW)
            )

            // Glowing orange arc
            val sweepFrac = ((zoomRatio - 1.0f) / 7.0f).coerceIn(0.15f, 0.95f)
            drawArc(
                color = Concept46OrangeGlow,
                startAngle = 135f,
                sweepAngle = 270f * sweepFrac,
                useCenter = false,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )

            // Center mechanical screw core
            drawCircle(
                color = Color(0xFF191A1C),
                radius = 4.dp.toPx()
            )
            drawCircle(
                color = Concept46Aluminum,
                radius = 1.5.dp.toPx()
            )
        }
    }
}

/**
 * Multi-colored stacked progress meter (Green, Yellow, Orange, Red segments).
 */
@Composable
private fun MultiColorProgressMeter(evValue: Float) {
    Canvas(modifier = Modifier.size(width = 8.dp, height = 30.dp)) {
        val segmentCount = 6
        val segHeight = (size.height - (segmentCount - 1) * 1.5.dp.toPx()) / segmentCount
        val colors = listOf(
            Color(0xFFFF2A2A), // Red
            Color(0xFFFF9100), // Orange
            Color(0xFFFFD600), // Yellow
            Color(0xFF00E676), // Green
            Color(0xFF00C853), // Green
            Color(0xFF00A340)  // Dark Green
        )
        val activeThreshold = ((evValue + 2.0f) / 4.0f * segmentCount).toInt().coerceIn(1, segmentCount)

        for (i in 0 until segmentCount) {
            val y = i * (segHeight + 1.5.dp.toPx())
            val isActive = (segmentCount - 1 - i) < activeThreshold
            drawRect(
                color = if (isActive) colors[i] else Color(0xFF222426),
                topLeft = Offset(0f, y),
                size = Size(size.width, segHeight)
            )
        }
    }
}

/**
 * Horizontal 'Ruler' Calibration Gauge with fine millimeter ticks and a red indicator mark.
 */
@Composable
private fun HorizontalRulerGauge(
    currentEv: Float,
    onEvChanged: (Float) -> Unit,
    onTickHaptic: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stops = remember {
        listOf(-2.0f, -1.67f, -1.33f, -1.0f, -0.67f, -0.33f, 0.0f, 0.33f, 0.67f, 1.0f, 1.33f, 1.67f, 2.0f)
    }

    Box(
        modifier = modifier
            .height(30.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFF121315))
            .border(0.5.dp, Color(0xFF2C2E32), RoundedCornerShape(3.dp))
            .pointerInput(currentEv) {
                detectHorizontalDragGestures { change, dragAmount ->
                    change.consume()
                    val totalW = size.width.toFloat()
                    if (totalW > 0f) {
                        val delta = (dragAmount / totalW) * 4.0f * 1.8f
                        val newEv = (currentEv + delta).coerceIn(-2.0f, 2.0f)
                        val nearest = stops.minByOrNull { kotlin.math.abs(it - newEv) } ?: newEv
                        if (kotlin.math.abs(nearest - currentEv) > 0.15f) {
                            onTickHaptic()
                            onEvChanged(nearest)
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp)) {
            val totalW = size.width
            val midY = size.height / 2f

            // Baseline horizontal track
            drawLine(
                color = Color(0xFF2C2E32),
                start = Offset(0f, midY),
                end = Offset(totalW, midY),
                strokeWidth = 1.dp.toPx()
            )

            // Precision ruler ticks
            val tickCount = 21
            for (i in 0 until tickCount) {
                val x = i * (totalW / (tickCount - 1))
                val isMajor = i % 5 == 0
                val tickH = if (isMajor) 14.dp.toPx() else 7.dp.toPx()
                val tickCol = if (isMajor) Concept46Aluminum else Color(0xFF42454A)

                drawLine(
                    color = tickCol,
                    start = Offset(x, midY - tickH / 2f),
                    end = Offset(x, midY + tickH / 2f),
                    strokeWidth = if (isMajor) 1.5.dp.toPx() else 1.dp.toPx()
                )
            }

            // Red hairline indicator mark
            val normPos = ((currentEv.coerceIn(-2f, 2f) + 2f) / 4f)
            val indicatorX = normPos * totalW

            drawLine(
                color = Concept46IndicatorRed,
                start = Offset(indicatorX, 2f),
                end = Offset(indicatorX, size.height - 2f),
                strokeWidth = 2.dp.toPx()
            )

            // Red pointer pip
            drawCircle(
                color = Concept46IndicatorRed,
                radius = 2.5.dp.toPx(),
                center = Offset(indicatorX, size.height - 3.dp.toPx())
            )
        }
    }
}

/**
 * Inset dark LCD telemetry readout display with fine hairline dividers.
 * Displays SS (Shutter Speed), EV value, ISO, and a vintage 'S' mode indicator.
 */
@Composable
private fun TelemetryReadoutDisplay(state: CameraUiState) {
    val evDisplay = remember(state.evValue) {
        if (state.evValue > 0f) String.format(Locale.US, "+%.1f", state.evValue)
        else if (state.evValue < 0f) String.format(Locale.US, "%.1f", state.evValue)
        else "0.0"
    }

    val isoDisplay = remember(state.filmPreset) {
        state.filmPreset.isoDisplay.replace("ISO ", "")
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(Concept46LcdDark)
            .border(1.dp, Color(0xFF2A2C30), RoundedCornerShape(3.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // SS (Shutter Speed)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "SS",
                style = androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 6.5.sp,
                    color = Color(0xFF6E7177)
                )
            )
            Text(
                text = "1/1.2k",
                style = androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = VintageTextLight
                )
            )
        }

        // Fine divider line
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(18.dp)
                .background(Concept46Divider)
        )

        // EV
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "EV",
                style = androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 6.5.sp,
                    color = Color(0xFF6E7177)
                )
            )
            Text(
                text = evDisplay,
                style = androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (state.evValue != 0f) VintageAmber else VintageTextLight
                )
            )
        }

        // Fine divider line
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(18.dp)
                .background(Concept46Divider)
        )

        // ISO
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "ISO",
                style = androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 6.5.sp,
                    color = Color(0xFF6E7177)
                )
            )
            Text(
                text = isoDisplay,
                style = androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = VintageGold
                )
            )
        }

        // Fine divider line
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(18.dp)
                .background(Concept46Divider)
        )

        // Vintage 'S' Mode Indicator
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF202225)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "S",
                style = androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = VintageAmber
                )
            )
        }
    }
}

/**
 * Textured vertical 'P S' sliding mechanical switch and machined metallic screw button.
 */
@Composable
private fun VerticalSliderSwitch(onToggle: () -> Unit) {
    var switchState by remember { mutableStateOf(false) } // false = S, true = P

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Vertical switch track
        Box(
            modifier = Modifier
                .width(14.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF101113))
                .border(0.5.dp, Color(0xFF333538), RoundedCornerShape(3.dp))
                .clickable {
                    switchState = !switchState
                    onToggle()
                },
            contentAlignment = if (switchState) Alignment.TopCenter else Alignment.BottomCenter
        ) {
            // Text labels inside track
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "P",
                    fontSize = 6.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (switchState) VintageGold else Color(0xFF55575C)
                )
                Text(
                    text = "S",
                    fontSize = 6.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (!switchState) VintageAmber else Color(0xFF55575C)
                )
            }

            // Knurled sliding metallic thumb
            Box(
                modifier = Modifier
                    .size(width = 12.dp, height = 11.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Concept46AluminumLight,
                                Concept46KnurledMetal,
                                Color(0xFF26272A)
                            )
                        )
                    )
                    .border(0.5.dp, Color(0xFF1B1C1E), RoundedCornerShape(2.dp))
            )
        }

        // Concentric machined screw button
        Canvas(modifier = Modifier.size(10.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(
                color = Concept46Aluminum,
                radius = size.width / 2f
            )
            drawCircle(
                color = Color(0xFF25272A),
                radius = size.width / 3f
            )
            // Screw slot
            drawLine(
                color = Concept46AluminumLight,
                start = Offset(2f, center.y),
                end = Offset(size.width - 2f, center.y),
                strokeWidth = 1.dp.toPx()
            )
        }
    }
}

/**
 * Square recessed photo-box with heavy physical inset frame.
 */
@Composable
private fun RecessedPhotoWell(
    thumbnail: Bitmap?,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(54.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF141517),
                        Color(0xFF252629)
                    )
                )
            )
            .border(
                width = 2.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Concept46Aluminum,
                        Color(0xFF2A2B2E)
                    )
                ),
                shape = RoundedCornerShape(6.dp)
            )
            .clickable { onClick() }
            .padding(3.dp),
        contentAlignment = Alignment.Center
    ) {
        // Inner recessed cavity
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF0C0C0E))
                .border(1.dp, Color(0xFF1E2023), RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (thumbnail != null) {
                Image(
                    bitmap = thumbnail.asImageBitmap(),
                    contentDescription = "Open Gallery",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = "Gallery",
                    tint = Color(0xFF6E7177),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Pill-shaped 'SK' Logo/Badge in dark gray and red with orange camera icon.
 */
@Composable
private fun SkBrandBadge(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Concept46BadgeDark)
            .border(
                width = 1.5.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF4A4D53),
                        Color(0xFF1E1F22)
                    )
                ),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Red circular emblem with 'SK'
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Concept46BadgeRed),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SK",
                    style = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                )
            }

            // 'CONCEPT 46' label in crisp titanium styling
            Column {
                Text(
                    text = "CONCEPT 46",
                    style = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = VintageTextLight
                )
                Text(
                    text = "35mm OPTICAL",
                    style = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 6.sp,
                        letterSpacing = 1.sp
                    ),
                    color = VintageTextMuted
                )
            }
        }
    }
}

/**
 * Grand Circular Shutter Button featuring concentric silver-metallic grain,
 * specular highlight rim, and beveled brushed chrome bezel.
 */
@Composable
private fun Concept46ShutterButton(
    isCapturing: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scaleFactor by animateFloatAsState(
        targetValue = if (isPressed || isCapturing) 0.91f else 1.0f,
        label = "shutterScale"
    )

    Box(
        modifier = Modifier
            .size(86.dp)
            .scale(scaleFactor)
            .shadow(elevation = 10.dp, shape = CircleShape, spotColor = Color.Black)
            .clip(CircleShape)
            // Outer 3D beveled sandblasted aluminum chassis
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFE2E4E8),
                        Concept46Aluminum,
                        Color(0xFF4A4C50)
                    )
                )
            )
            .border(2.5.dp, Color(0xFF2C2D30), CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        // Middle ring: Concentric silver-metallic grain simulation
        Canvas(modifier = Modifier.size(68.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxR = size.width / 2f

            // Concentric annular lathe rings
            val ringCount = 8
            for (i in 1..ringCount) {
                val r = maxR * (i / ringCount.toFloat())
                val ringCol = if (i % 2 == 0) Color(0xFFD6D8DC) else Color(0xFF888B90)
                drawCircle(
                    color = ringCol,
                    radius = r,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            // Specular radial highlights
            drawArc(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x80FFFFFF), Color(0x00FFFFFF)),
                    center = Offset(size.width * 0.3f, size.height * 0.3f),
                    radius = maxR * 0.7f
                ),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = true
            )
        }

        // Inner plunger: Concave tactile center trigger
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF333538),
                            Color(0xFF1E1F22),
                            Color(0xFF111214)
                        )
                    )
                )
                .border(1.5.dp, Color(0xFF5A5C62), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Subtle red or silver core pip
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Concept46IndicatorRed,
                                Color(0xFF990000)
                            )
                        )
                    )
                    .border(0.5.dp, Color(0xFF330000), CircleShape)
            )
        }
    }
}

/**
 * Machined metallic lens switcher button (front/back camera).
 */
@Composable
private fun LensSwitcherButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF3A3C40),
                        Concept46RubberDark,
                        Color(0xFF161719)
                    )
                )
            )
            .border(1.5.dp, Concept46Aluminum, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Cameraswitch,
            contentDescription = "Switch Lens",
            tint = VintageAmber,
            modifier = Modifier.size(22.dp)
        )
    }
}
