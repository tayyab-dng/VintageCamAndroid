package com.example.vintagecam.ui

import android.content.Intent
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.example.vintagecam.theme.Concept46RubberDark
import com.example.vintagecam.theme.VintageAmber
import com.example.vintagecam.theme.VintageBlack
import com.example.vintagecam.theme.VintageBodyDark
import com.example.vintagecam.theme.VintageLeatherGrip
import com.example.vintagecam.theme.VintageMetalDark
import com.example.vintagecam.theme.VintageSurfaceElevated
import com.example.vintagecam.theme.VintageTextLight
import com.example.vintagecam.theme.VintageTextMuted
import com.example.vintagecam.ui.components.Concept46BottomGrip
import com.example.vintagecam.ui.components.TopTitaniumBar
import com.example.vintagecam.ui.components.ViewfinderExposureSlider
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

    val openGallery = {
        val uri = state.lastCapturedUri
        val intent = if (uri != null) {
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "image/jpeg")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_VIEW).apply {
                type = "image/*"
            }
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No gallery application found", Toast.LENGTH_SHORT).show()
        }
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
            // ==========================================
            // 1. TOP TITANIUM / METALLIC BEZEL BAR
            // ==========================================
            TopTitaniumBar(
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
                onFilmPresetClick = {
                    hapticManager.dialClick()
                    showFilmTray = !showFilmTray
                }
            )

            // ==========================================
            // 2. FILM TRAY POPUP (Expandable film stocks)
            // ==========================================
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

            // ==========================================
            // 3. CENTRAL VIEWFINDER WITH ASPECT RATIO MASKING & EXPOSURE SLIDER
            // ==========================================
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Subtle horizontal exposure slider right above viewfinder
                ViewfinderExposureSlider(
                    currentEv = state.evValue,
                    onEvChanged = { newEv ->
                        val newIndex = (newEv / state.evStep).roundToInt().coerceIn(state.minEvIndex, state.maxEvIndex)
                        if (newIndex != state.evIndex) {
                            controller.setExposureCompensation(newIndex)
                            state = state.copy(evValue = newEv, evIndex = newIndex)
                        }
                    },
                    onTickHaptic = {
                        hapticManager.ratchetTick()
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
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
            }

            // ==========================================
            // 4. ASYMMETRIC 'CONCEPT 46' BOTTOM GRIP PANEL
            // ==========================================
            Concept46BottomGrip(
                state = state,
                onShutterClick = {
                    if (state.isCapturing) return@Concept46BottomGrip
                    hapticManager.shutterRelease()
                    state = state.copy(isCapturing = true)
                    coroutineScope.launch {
                        if (state.flashMode != VintageFlashMode.OFF) {
                            flashAlpha.snapTo(0.85f)
                            flashAlpha.animateTo(0f, tween(180, easing = FastOutSlowInEasing))
                        }
                        shutterAlpha.snapTo(1f)
                        shutterAlpha.animateTo(0f, tween(100, easing = LinearEasing))

                        controller.captureStillPhoto(
                            context = context,
                            coroutineScope = coroutineScope,
                            aspectRatio = state.aspectRatio,
                            filmPreset = state.filmPreset,
                            isDateStampEnabled = state.isDateStampEnabled,
                            dateStampText = quartzDateString,
                            isMirroring = (state.lensFacing == CameraSelector.LENS_FACING_FRONT),
                            onSuccess = { uri, thumbnail ->
                                val nextCounter = if (state.frameCounter < state.maxFrames) state.frameCounter + 1 else 1
                                state = state.copy(
                                    frameCounter = nextCounter,
                                    lastCapturedUri = uri,
                                    lastThumbnail = thumbnail,
                                    isCapturing = false
                                )
                                Toast.makeText(
                                    context,
                                    "FRAME #${nextCounter} EXPOSED & SAVED TO GALLERY",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onError = { ex ->
                                state = state.copy(isCapturing = false)
                                Toast.makeText(
                                    context,
                                    "CAPTURE FAILED: ${ex.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                },
                onGalleryClick = {
                    hapticManager.dialClick()
                    openGallery()
                },
                onFilmTrayToggle = {
                    hapticManager.ratchetTick()
                    showFilmTray = !showFilmTray
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
                onEvChanged = { newEv ->
                    val newIndex = (newEv / state.evStep).roundToInt().coerceIn(state.minEvIndex, state.maxEvIndex)
                    if (newIndex != state.evIndex) {
                        controller.setExposureCompensation(newIndex)
                        state = state.copy(evValue = newEv, evIndex = newIndex)
                    }
                },
                onTickHaptic = {
                    hapticManager.ratchetTick()
                },
                onDialHaptic = {
                    hapticManager.dialClick()
                }
            )
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
            .clip(RoundedCornerShape(10.dp))
            .background(VintageBlack),
        contentAlignment = Alignment.Center
    ) {
        // Precision Viewfinder Window constrained to active aspect ratio (3:2, 4:3, or 9:16)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(state.aspectRatio.floatRatio)
                .clip(RoundedCornerShape(6.dp))
                .border(2.dp, Color(0xFF2E3034), RoundedCornerShape(6.dp))
        ) {
            // Core OpenGL ES 3.0 Viewfinder stream
            content()

            // Viewfinder Reticle, Framing Guides & Center Rangefinder Circle
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidthPx = 1.5.dp.toPx()
                val bracketLen = 20.dp.toPx()
                val cornerOffset = 14.dp.toPx()
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
                    color = reticleColor.copy(alpha = 0.35f),
                    radius = 24.dp.toPx(),
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )

                // Center Crosshair
                drawLine(
                    color = reticleColor.copy(alpha = 0.45f),
                    start = Offset(center.x - 7.dp.toPx(), center.y),
                    end = Offset(center.x + 7.dp.toPx(), center.y),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = reticleColor.copy(alpha = 0.45f),
                    start = Offset(center.x, center.y - 7.dp.toPx()),
                    end = Offset(center.x, center.y + 7.dp.toPx()),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // 90s Quartz Date Stamp OSD (Classic Incandescent Quartz Orange #FF7A00)
            if (state.isDateStampEnabled) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp, bottom = 10.dp)
                ) {
                    Text(
                        text = quartzDateString,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            letterSpacing = 2.sp
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
            .background(Color(0xFF222326))
            .border(1.dp, Color(0xFF383A3E))
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
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) VintageSurfaceElevated else VintageBodyDark)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) preset.badgeColor else VintageMetalDark,
                            shape = RoundedCornerShape(6.dp)
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
                            fontSize = 8.5.sp
                        ),
                        color = VintageTextMuted
                    )
                }
            }
        }
    }
}
