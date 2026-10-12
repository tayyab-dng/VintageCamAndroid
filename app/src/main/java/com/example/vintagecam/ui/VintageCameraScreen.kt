package com.example.vintagecam.ui

import android.os.Environment
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Exposure
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vintagecam.camera.CameraPreview
import com.example.vintagecam.camera.CameraUiState
import com.example.vintagecam.camera.VintageAspectRatio
import com.example.vintagecam.camera.VintageFilmPreset
import com.example.vintagecam.camera.VintageFlashMode
import com.example.vintagecam.camera.rememberVintageCameraController
import com.example.vintagecam.haptics.rememberHapticManager
import com.example.vintagecam.theme.VintageAmber
import com.example.vintagecam.theme.VintageBlack
import com.example.vintagecam.theme.VintageBodyDark
import com.example.vintagecam.theme.VintageGold
import com.example.vintagecam.theme.VintageGreenAccent
import com.example.vintagecam.theme.VintageLeatherGrip
import com.example.vintagecam.theme.VintageMetalDark
import com.example.vintagecam.theme.VintageMetalSilver
import com.example.vintagecam.theme.VintageShutterRed
import com.example.vintagecam.theme.VintageShutterRedDark
import com.example.vintagecam.theme.VintageSurfaceElevated
import com.example.vintagecam.theme.VintageTextLight
import com.example.vintagecam.theme.VintageTextMuted
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun VintageCameraScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val controller = rememberVintageCameraController()
    val hapticManager = rememberHapticManager()

    var state by remember { mutableStateOf(CameraUiState()) }
    var showFilmTray by remember { mutableStateOf(false) }

    // Shutter animation states
    val shutterAlpha = remember { Animatable(0f) }
    val flashAlpha = remember { Animatable(0f) }

    // 90s Quartz Date Stamp string (e.g. '26 10 13)
    val quartzDateString = remember {
        SimpleDateFormat("''yy MM dd", Locale.US).format(System.currentTimeMillis())
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = VintageBlack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. TOP ANALOG TELEMETRY & HARDWARE SWITCHES
            TopControlsDeck(
                state = state,
                onFlashToggle = {
                    hapticManager.dialClick()
                    val nextFlash = when (state.flashMode) {
                        VintageFlashMode.AUTO -> VintageFlashMode.ON
                        VintageFlashMode.ON -> VintageFlashMode.OFF
                        VintageFlashMode.OFF -> VintageFlashMode.AUTO
                    }
                    state = state.copy(flashMode = nextFlash)
                },
                onAspectRatioToggle = {
                    hapticManager.dialClick()
                    val nextRatio = when (state.aspectRatio) {
                        VintageAspectRatio.RATIO_3_2 -> VintageAspectRatio.RATIO_4_3
                        VintageAspectRatio.RATIO_4_3 -> VintageAspectRatio.RATIO_9_16
                        VintageAspectRatio.RATIO_9_16 -> VintageAspectRatio.RATIO_3_2
                    }
                    state = state.copy(aspectRatio = nextRatio)
                },
                onGridToggle = {
                    hapticManager.dialClick()
                    state = state.copy(isGridVisible = !state.isGridVisible)
                },
                onDateStampToggle = {
                    hapticManager.dialClick()
                    state = state.copy(isDateStampEnabled = !state.isDateStampEnabled)
                },
                onEvToggle = {
                    hapticManager.dialClick()
                    state = state.copy(showEvDial = !state.showEvDial)
                },
                onToggleFilmTray = {
                    hapticManager.dialClick()
                    showFilmTray = !showFilmTray
                }
            )

            // 2. FILM TRAY POPUP (Expandable film emulsions)
            AnimatedVisibility(
                visible = showFilmTray,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                FilmSimulationSelector(
                    activePreset = state.filmPreset,
                    onSelectPreset = { preset ->
                        hapticManager.ratchetTick()
                        state = state.copy(filmPreset = preset)
                        showFilmTray = false
                    }
                )
            }

            // 3. EXPANDABLE ANALOG EV EXPOSURE WHEEL
            AnimatedVisibility(
                visible = state.showEvDial,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                AnalogEvDialWheel(
                    currentEv = state.evValue,
                    onEvChanged = { newEv ->
                        val newIndex = (newEv / state.evStep).roundToInt().coerceIn(state.minEvIndex, state.maxEvIndex)
                        if (newIndex != state.evIndex) {
                            hapticManager.ratchetTick()
                            controller.setExposureCompensation(newIndex)
                            state = state.copy(evValue = newEv, evIndex = newIndex)
                        }
                    },
                    onResetEv = {
                        hapticManager.dialClick()
                        controller.setExposureCompensation(0)
                        state = state.copy(evValue = 0f, evIndex = 0)
                    }
                )
            }

            // 4. CENTRAL VIEWFINDER WITH PRECISION LETTERBOX/PILLARBOX MASKING
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                ViewfinderFrame(
                    state = state,
                    quartzDateString = quartzDateString,
                    content = {
                        CameraPreview(
                            modifier = Modifier.fillMaxSize(),
                            lensFacing = state.lensFacing,
                            aspectRatio = state.aspectRatio,
                            flashMode = state.flashMode,
                            filmPreset = state.filmPreset,
                            controller = controller,
                            onZoomChanged = { current, min, max ->
                                state = state.copy(
                                    zoomRatio = current,
                                    minZoom = min,
                                    maxZoom = max
                                )
                            },
                            onExposureStateChanged = { isSupported, minIdx, maxIdx, step, currIdx ->
                                state = state.copy(
                                    isEvSupported = isSupported,
                                    minEvIndex = minIdx,
                                    maxEvIndex = maxIdx,
                                    evStep = step,
                                    evIndex = currIdx,
                                    evValue = currIdx * step
                                )
                            },
                            onCaptureStarted = {
                                coroutineScope.launch {
                                    shutterAlpha.snapTo(1f)
                                    shutterAlpha.animateTo(0f, tween(120, easing = LinearEasing))
                                }
                            },
                            onPhotoCaptured = { savedFile ->
                                state = state.copy(
                                    frameCounter = if (state.frameCounter < state.maxFrames) state.frameCounter + 1 else 1
                                )
                                Toast.makeText(
                                    context,
                                    "FRAME #${state.frameCounter} EXPOSED & SAVED",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                )

                // Mechanical Shutter Blackout Layer
                if (shutterAlpha.value > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = shutterAlpha.value))
                    )
                }

                // Flash Pop Overlay
                if (flashAlpha.value > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = flashAlpha.value))
                    )
                }
            }

            // 5. BOTTOM ANALOG BODY & SHUTTER DECK
            BottomControlsDeck(
                state = state,
                onShutterClick = {
                    hapticManager.shutterRelease()
                    coroutineScope.launch {
                        if (state.flashMode != VintageFlashMode.OFF) {
                            flashAlpha.snapTo(0.85f)
                            flashAlpha.animateTo(0f, tween(180, easing = FastOutSlowInEasing))
                        }
                        shutterAlpha.snapTo(1f)
                        shutterAlpha.animateTo(0f, tween(100, easing = LinearEasing))

                        val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                            ?: context.filesDir
                        controller.takePhoto(context, outputDir)
                    }
                },
                onSwitchCamera = {
                    hapticManager.dialClick()
                    val nextLens = if (state.lensFacing == CameraSelector.LENS_FACING_BACK) {
                        CameraSelector.LENS_FACING_FRONT
                    } else {
                        CameraSelector.LENS_FACING_BACK
                    }
                    state = state.copy(lensFacing = nextLens)
                },
                onFilmBadgeClick = {
                    hapticManager.dialClick()
                    showFilmTray = !showFilmTray
                }
            )
        }
    }
}

/**
 * Top control panel designed after analog rangefinder top plates.
 */
@Composable
private fun TopControlsDeck(
    state: CameraUiState,
    onFlashToggle: () -> Unit,
    onAspectRatioToggle: () -> Unit,
    onGridToggle: () -> Unit,
    onDateStampToggle: () -> Unit,
    onEvToggle: () -> Unit,
    onToggleFilmTray: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(VintageBodyDark)
            .border(width = 1.dp, color = VintageMetalDark)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Analog LCD Film Counter Window
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(VintageBlack)
                .border(1.dp, VintageMetalDark, RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "EXP",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp
                ),
                color = VintageTextMuted
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = String.format("%02d/%02d", state.frameCounter, state.maxFrames),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                ),
                color = VintageAmber
            )
        }

        // Active Film Stock Pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(VintageLeatherGrip)
                .border(1.dp, state.filmPreset.badgeColor, RoundedCornerShape(14.dp))
                .clickable { onToggleFilmTray() }
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(state.filmPreset.badgeColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = state.filmPreset.title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = VintageTextLight
                )
            }
        }

        // Hardware Function Controls (Aspect, EV, Date, Grid, Flash)
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Aspect Ratio Switcher (3:2, 4:3, 9:16)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(VintageLeatherGrip)
                    .border(1.dp, VintageMetalDark, RoundedCornerShape(6.dp))
                    .clickable { onAspectRatioToggle() }
                    .padding(horizontal = 7.dp, vertical = 5.dp)
            ) {
                Text(
                    text = state.aspectRatio.title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = VintageGold
                )
            }

            // EV Exposure Toggle Button
            IconButton(
                onClick = onEvToggle,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Exposure,
                    contentDescription = "EV Exposure Dial",
                    tint = if (state.showEvDial || state.evValue != 0f) VintageAmber else VintageMetalDark,
                    modifier = Modifier.size(17.dp)
                )
            }

            // 90s Quartz Date Stamp Toggle
            IconButton(
                onClick = onDateStampToggle,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "Quartz Datestamp",
                    tint = if (state.isDateStampEnabled) Color(0xFFFF7A00) else VintageMetalDark,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Grid Toggle
            IconButton(
                onClick = onGridToggle,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.GridOn,
                    contentDescription = "Toggle Grid",
                    tint = if (state.isGridVisible) VintageAmber else VintageMetalDark,
                    modifier = Modifier.size(17.dp)
                )
            }

            // Flash Mode Toggle
            IconButton(
                onClick = onFlashToggle,
                modifier = Modifier.size(34.dp)
            ) {
                val (flashIcon, flashTint) = when (state.flashMode) {
                    VintageFlashMode.AUTO -> Pair(Icons.Default.FlashAuto, VintageAmber)
                    VintageFlashMode.ON -> Pair(Icons.Default.FlashOn, VintageGold)
                    VintageFlashMode.OFF -> Pair(Icons.Default.FlashOff, VintageMetalDark)
                }
                Icon(
                    imageVector = flashIcon,
                    contentDescription = "Flash Mode",
                    tint = flashTint,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}

/**
 * Precision Viewfinder with letterbox/pillarbox masks and 90s Quartz Datestamp HUD.
 */
@Composable
private fun ViewfinderFrame(
    state: CameraUiState,
    quartzDateString: String,
    content: @Composable () -> Unit
) {
    // Outer Container providing matte camera body letterboxing / pillarboxing
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(VintageBlack),
        contentAlignment = Alignment.Center
    ) {
        // Precision Viewfinder Window constrained to active aspect ratio (3:2, 4:3, or 9:16)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(state.aspectRatio.floatRatio)
                .clip(RoundedCornerShape(8.dp))
                .border(2.5.dp, VintageLeatherGrip, RoundedCornerShape(8.dp))
                .background(Color.Black)
        ) {
            // Core OpenGL ES 3.0 Viewfinder
            content()

            // Viewfinder Reticle, Framing Guides & Center Rangefinder Circle
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidthPx = 1.5.dp.toPx()
                val bracketLen = 22.dp.toPx()
                val cornerOffset = 16.dp.toPx()
                val reticleColor = Color(0xFFFFB03B).copy(alpha = 0.85f)
                val gridColor = Color(0x33FFFFFF)

                // Rule-of-Thirds Grid
                if (state.isGridVisible) {
                    val oneThirdW = size.width / 3f
                    val oneThirdH = size.height / 3f

                    drawLine(gridColor, Offset(oneThirdW, 0f), Offset(oneThirdW, size.height), 1.dp.toPx())
                    drawLine(gridColor, Offset(oneThirdW * 2f, 0f), Offset(oneThirdW * 2f, size.height), 1.dp.toPx())
                    drawLine(gridColor, Offset(0f, oneThirdH), Offset(size.width, oneThirdH), 1.dp.toPx())
                    drawLine(gridColor, Offset(0f, oneThirdH * 2f), Offset(size.width, oneThirdH * 2f), 1.dp.toPx())
                }

                // 4 Corner 35mm Frame Brackets
                // Top-Left
                drawLine(reticleColor, Offset(cornerOffset, cornerOffset), Offset(cornerOffset + bracketLen, cornerOffset), strokeWidthPx)
                drawLine(reticleColor, Offset(cornerOffset, cornerOffset), Offset(cornerOffset, cornerOffset + bracketLen), strokeWidthPx)

                // Top-Right
                drawLine(reticleColor, Offset(size.width - cornerOffset, cornerOffset), Offset(size.width - cornerOffset - bracketLen, cornerOffset), strokeWidthPx)
                drawLine(reticleColor, Offset(size.width - cornerOffset, cornerOffset), Offset(size.width - cornerOffset, cornerOffset + bracketLen), strokeWidthPx)

                // Bottom-Left
                drawLine(reticleColor, Offset(cornerOffset, size.height - cornerOffset), Offset(cornerOffset + bracketLen, size.height - cornerOffset), strokeWidthPx)
                drawLine(reticleColor, Offset(cornerOffset, size.height - cornerOffset), Offset(cornerOffset, size.height - cornerOffset - bracketLen), strokeWidthPx)

                // Bottom-Right
                drawLine(reticleColor, Offset(size.width - cornerOffset, size.height - cornerOffset), Offset(size.width - cornerOffset - bracketLen, size.height - cornerOffset), strokeWidthPx)
                drawLine(reticleColor, Offset(size.width - cornerOffset, size.height - cornerOffset), Offset(size.width - cornerOffset, size.height - cornerOffset - bracketLen), strokeWidthPx)

                // Center Rangefinder Focus Circle
                drawCircle(
                    color = reticleColor.copy(alpha = 0.4f),
                    radius = 28.dp.toPx(),
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )

                // Center Crosshair
                drawLine(
                    color = reticleColor.copy(alpha = 0.5f),
                    start = Offset(center.x - 8.dp.toPx(), center.y),
                    end = Offset(center.x + 8.dp.toPx(), center.y),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = reticleColor.copy(alpha = 0.5f),
                    start = Offset(center.x, center.y - 8.dp.toPx()),
                    end = Offset(center.x, center.y + 8.dp.toPx()),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Viewfinder Telemetry Banner (ISO, Shutter, Aperture, Zoom, EV)
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xAA111111))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = state.filmPreset.isoDisplay,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = VintageAmber
                )
                Text(
                    text = "•",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = VintageMetalDark
                )
                Text(
                    text = "1/125s",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp
                    ),
                    color = VintageTextLight
                )
                Text(
                    text = "•",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = VintageMetalDark
                )
                Text(
                    text = "f/2.8",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp
                    ),
                    color = VintageTextLight
                )
                Text(
                    text = "•",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = VintageMetalDark
                )
                Text(
                    text = String.format("%.1fx", state.zoomRatio),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = VintageGold
                )
                Text(
                    text = "•",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = VintageMetalDark
                )
                // Live EV Exposure Telemetry
                Text(
                    text = if (state.evValue > 0f) String.format("+%.1f EV", state.evValue)
                           else if (state.evValue < 0f) String.format("%.1f EV", state.evValue)
                           else "±0.0 EV",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (state.evValue != 0f) VintageAmber else VintageTextLight
                )
            }

            // 90s Quartz Date Stamp OSD (Classic Incandescent Quartz Orange #FF7A00)
            if (state.isDateStampEnabled) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 14.dp, bottom = 12.dp)
                ) {
                    Text(
                        text = quartzDateString,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            letterSpacing = 2.5.sp
                        ),
                        color = Color(0xFFFF7A00),
                        modifier = Modifier.shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(2.dp),
                            spotColor = Color(0xFFFF7A00),
                            ambientColor = Color(0xFFFF7A00)
                        )
                    )
                }
            }
        }
    }
}

/**
 * Tactile Analog EV Exposure Dial Wheel (-2.0 EV to +2.0 EV in 1/3 stop increments).
 */
@Composable
private fun AnalogEvDialWheel(
    currentEv: Float,
    onEvChanged: (Float) -> Unit,
    onResetEv: () -> Unit
) {
    val stops = remember {
        listOf(-2.0f, -1.67f, -1.33f, -1.0f, -0.67f, -0.33f, 0.0f, 0.33f, 0.67f, 1.0f, 1.33f, 1.67f, 2.0f)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(VintageBodyDark)
            .border(1.dp, VintageMetalDark)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "EXPOSURE COMPENSATION",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = VintageAmber
            )

            // Current EV readout with reset click
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(VintageLeatherGrip)
                    .clickable { onResetEv() }
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (currentEv > 0f) String.format("+%.1f EV", currentEv)
                           else if (currentEv < 0f) String.format("%.1f EV", currentEv)
                           else "0.0 EV (RESET)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = VintageGold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tactile Draggable Vernier Scale Wheel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(VintageBlack)
                .border(1.dp, VintageMetalDark, RoundedCornerShape(6.dp))
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { _, dragAmount ->
                        val stepChange = if (dragAmount > 8f) 0.333f else if (dragAmount < -8f) -0.333f else 0f
                        if (stepChange != 0f) {
                            val newEv = (currentEv + stepChange).coerceIn(-2.0f, 2.0f)
                            // Find nearest stop
                            val nearest = stops.minByOrNull { kotlin.math.abs(it - newEv) } ?: newEv
                            onEvChanged(nearest)
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Horizontal calibrated ticks
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                stops.forEach { stop ->
                    val isInteger = stop == -2.0f || stop == -1.0f || stop == 0.0f || stop == 1.0f || stop == 2.0f
                    val isZero = stop == 0.0f
                    val isCurrent = kotlin.math.abs(stop - currentEv) < 0.15f

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .width(if (isCurrent) 2.5.dp else if (isInteger) 2.dp else 1.dp)
                                .height(if (isCurrent) 22.dp else if (isInteger) 16.dp else 10.dp)
                                .background(
                                    if (isCurrent) VintageAmber
                                    else if (isZero) VintageGold
                                    else if (isInteger) VintageMetalSilver
                                    else VintageMetalDark
                                )
                        )
                        if (isInteger) {
                            Text(
                                text = if (stop > 0f) String.format("+%.0f", stop) else String.format("%.0f", stop),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = if (isCurrent) VintageAmber else VintageTextMuted
                            )
                        }
                    }
                }
            }

            // Center calibrated amber cursor line
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height(30.dp)
                    .background(VintageAmber)
            )
        }
    }
}

/**
 * Expandable Film Simulation Selector
 */
@Composable
private fun FilmSimulationSelector(
    activePreset: VintageFilmPreset,
    onSelectPreset: (VintageFilmPreset) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(VintageLeatherGrip)
            .border(1.dp, VintageMetalDark)
            .padding(vertical = 10.dp)
    ) {
        Text(
            text = "SELECT 35mm FILM STOCK",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold
            ),
            color = VintageAmber,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(VintageFilmPreset.values()) { preset ->
                val isSelected = preset == activePreset
                Column(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) VintageSurfaceElevated else VintageBodyDark)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) preset.badgeColor else VintageMetalDark,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onSelectPreset(preset) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = preset.title,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (isSelected) preset.badgeColor else VintageTextLight
                    )
                    Text(
                        text = preset.subtitle,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp
                        ),
                        color = VintageTextMuted
                    )
                }
            }
        }
    }
}

/**
 * Bottom camera plate containing the grand chrome shutter button and tactile hardware dials.
 */
@Composable
private fun BottomControlsDeck(
    state: CameraUiState,
    onShutterClick: () -> Unit,
    onSwitchCamera: () -> Unit,
    onFilmBadgeClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val shutterScale = if (isPressed) 0.92f else 1.0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(VintageBodyDark)
            .border(1.dp, VintageMetalDark)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Film Brand Plate / Badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(VintageLeatherGrip)
                .border(1.dp, VintageMetalDark, RoundedCornerShape(4.dp))
                .clickable { onFilmBadgeClick() }
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Text(
                text = "VINTAGECAM 35mm • QUARTZ DATE SYSTEM",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                ),
                color = VintageTextLight
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Main Shutter Deck Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Film Emulsion Dial Button
            IconButton(
                onClick = onFilmBadgeClick,
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(VintageLeatherGrip)
                    .border(1.5.dp, VintageMetalSilver, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Film Emulsion Settings",
                    tint = VintageAmber,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Center: Tactile Mechanical Analog Shutter Button
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .scale(shutterScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFF0F0F0),
                                VintageMetalSilver,
                                Color(0xFF666666)
                            )
                        )
                    )
                    .border(3.dp, VintageMetalDark, CircleShape)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        onShutterClick()
                    },
                contentAlignment = Alignment.Center
            ) {
                // Inner concave red shutter trigger
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    VintageShutterRed,
                                    VintageShutterRedDark
                                )
                            )
                        )
                        .border(1.5.dp, Color(0xFF440000), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(0x33000000))
                    )
                }
            }

            // Right: Camera Lens Switcher (Front/Back)
            IconButton(
                onClick = onSwitchCamera,
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(VintageLeatherGrip)
                    .border(1.5.dp, VintageMetalSilver, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Cameraswitch,
                    contentDescription = "Switch Camera Facing",
                    tint = VintageGold,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
