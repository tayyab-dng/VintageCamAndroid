package com.example.vintagecam.ui

import android.content.Intent
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import com.example.vintagecam.camera.ProControlTab
import com.example.vintagecam.camera.VintageAspectRatio
import com.example.vintagecam.camera.VintageFlashMode
import com.example.vintagecam.camera.rememberVintageCameraController
import com.example.vintagecam.haptics.rememberHapticManager
import com.example.vintagecam.theme.VintageBlack
import com.example.vintagecam.ui.components.BottomShutterDeck
import com.example.vintagecam.ui.components.ProControlBar
import com.example.vintagecam.ui.components.TopUtilityHeader
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Minimalist, Leica/Halide-inspired analog camera screen.
 * Decluttered of all fake gauges and permanently visible rulers.
 * Features:
 * 1. Compact Top Utility Header (EXP counter, Aspect Ratio, Flash, Grid, Quartz Date).
 * 2. True fitted aspect ratio Viewfinder with 35mm optical reticle and incandescent Quartz Date HUD.
 * 3. Contextual Single-Dial Pro Control Bar (FILM, ISO, SS, FOCUS, EV) with smooth expandable vernier dial.
 * 4. Clean Bottom Shutter Deck (Gallery thumbnail, 72mm machined-aluminum & red Leica shutter, Camera Flip).
 */
@Composable
fun VintageCameraScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val controller = rememberVintageCameraController()
    val hapticManager = rememberHapticManager()

    var state by remember { mutableStateOf(CameraUiState()) }

    // Shutter animation and flash pop states
    val shutterAlpha = remember { Animatable(0f) }
    val flashAlpha = remember { Animatable(0f) }

    // 90s Quartz Date format: 'YY MM DD
    val quartzDateString = remember {
        val dateFormat = SimpleDateFormat("''yy MM dd", Locale.US)
        dateFormat.format(System.currentTimeMillis())
    }

    fun openGallery() {
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
            // 1. SLEEK TOP UTILITY HEADER (SINGLE COMPACT ROW)
            // ==========================================
            TopUtilityHeader(
                state = state,
                onAspectRatioToggle = {
                    hapticManager.dialClick()
                    val nextRatio = when (state.aspectRatio) {
                        VintageAspectRatio.RATIO_3_2 -> VintageAspectRatio.RATIO_4_3
                        VintageAspectRatio.RATIO_4_3 -> VintageAspectRatio.RATIO_9_16
                        VintageAspectRatio.RATIO_9_16 -> VintageAspectRatio.RATIO_3_2
                    }
                    state = state.copy(aspectRatio = nextRatio)
                },
                onFlashToggle = {
                    hapticManager.dialClick()
                    val nextFlash = when (state.flashMode) {
                        VintageFlashMode.AUTO -> VintageFlashMode.ON
                        VintageFlashMode.ON -> VintageFlashMode.OFF
                        VintageFlashMode.OFF -> VintageFlashMode.AUTO
                    }
                    state = state.copy(flashMode = nextFlash)
                },
                onGridToggle = {
                    hapticManager.dialClick()
                    state = state.copy(isGridVisible = !state.isGridVisible)
                },
                onDateStampToggle = {
                    hapticManager.dialClick()
                    state = state.copy(isDateStampEnabled = !state.isDateStampEnabled)
                }
            )

            // ==========================================
            // 2. CENTRAL VIEWFINDER WITH DYNAMIC ASPECT RATIO RESIZING
            // ==========================================
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(8.dp)
                    .background(Color(0xFF080808)),
                contentAlignment = Alignment.Center
            ) {
                val targetRatio = state.aspectRatio.floatRatio // 4:3 -> 0.75f, 3:2 -> 0.6667f, 9:16 -> 0.5625f
                val containerRatio = maxWidth / maxHeight
                val (frameWidth, frameHeight) = if (containerRatio > targetRatio) {
                    (maxHeight * targetRatio) to maxHeight
                } else {
                    maxWidth to (maxWidth / targetRatio)
                }

                Box(
                    modifier = Modifier
                        .size(frameWidth, frameHeight)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(8.dp))
                        .clickable {
                            // Tapping the viewfinder dismisses any open pro drawer for clean shooting
                            if (state.activeProTab != null) {
                                state = state.copy(activeProTab = null)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    ViewfinderFrame(
                        modifier = Modifier.fillMaxSize(),
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
                                onHardwareCapabilitiesChanged = { isos, shutters, minFocusDist, isManualFocusSupported ->
                                    state = state.copy(
                                        supportedIsoStops = isos,
                                        supportedShutterStops = shutters,
                                        maxFocusDistance = if (minFocusDist > 0f) minFocusDist else 10.0f,
                                        isManualFocusSupported = isManualFocusSupported
                                    )
                                },
                                onAutoFocusReset = {
                                    state = state.copy(isAutoFocus = true)
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
            // 3. CONTEXTUAL SINGLE-DIAL PRO CONTROL BAR
            // ==========================================
            ProControlBar(
                state = state,
                onTabSelected = { tab ->
                    hapticManager.dialClick()
                    if (tab == ProControlTab.EV && state.isEvLocked) {
                        // Ignore EV selection when locked
                    } else {
                        state = state.copy(activeProTab = if (state.activeProTab == tab) null else tab)
                    }
                },
                onFilmPresetSelected = { preset ->
                    hapticManager.ratchetTick()
                    state = state.copy(filmPreset = preset)
                },
                onIsoSelected = { iso ->
                    hapticManager.dialClick()
                    if (iso == null) {
                        controller.setManualIso(null)
                        val isStillManual = state.selectedShutterLabel != null
                        state = state.copy(
                            selectedIso = null,
                            isAutoExposure = !isStillManual
                        )
                    } else {
                        controller.setExposureCompensation(0)
                        controller.setManualIso(iso)
                        state = state.copy(
                            selectedIso = iso,
                            isAutoExposure = false,
                            evIndex = 0,
                            evValue = 0f,
                            activeProTab = if (state.activeProTab == ProControlTab.EV) null else state.activeProTab
                        )
                    }
                },
                onShutterSpeedSelected = { label, nanos ->
                    hapticManager.dialClick()
                    if (label == null) {
                        controller.setManualShutterSpeed(null)
                        val isStillManual = state.selectedIso != null
                        state = state.copy(
                            selectedShutterLabel = null,
                            isAutoExposure = !isStillManual
                        )
                    } else {
                        controller.setExposureCompensation(0)
                        controller.setManualShutterSpeed(nanos)
                        state = state.copy(
                            selectedShutterLabel = label,
                            selectedShutterNanos = nanos,
                            isAutoExposure = false,
                            evIndex = 0,
                            evValue = 0f,
                            activeProTab = if (state.activeProTab == ProControlTab.EV) null else state.activeProTab
                        )
                    }
                },
                onAutoFocusToggle = { isAuto ->
                    hapticManager.dialClick()
                    controller.setAutoFocus(isAuto)
                    state = state.copy(isAutoFocus = isAuto)
                },
                onManualFocusDistanceChanged = { diopters ->
                    hapticManager.ratchetTick()
                    controller.setManualFocus(diopters)
                    state = state.copy(isAutoFocus = false, manualFocusDistance = diopters)
                },
                onEvChanged = { evVal, evIdx ->
                    if (!state.isEvLocked) {
                        hapticManager.ratchetTick()
                        controller.setExposureCompensation(evIdx)
                        state = state.copy(evValue = evVal, evIndex = evIdx)
                    }
                },
                onCloseDrawer = {
                    hapticManager.dialClick()
                    state = state.copy(activeProTab = null)
                }
            )

            // ==========================================
            // 4. CLEAN BOTTOM SHUTTER DECK
            // ==========================================
            BottomShutterDeck(
                state = state,
                onShutterClick = {
                    if (state.isCapturing) return@BottomShutterDeck
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
                            flashMode = state.flashMode,
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
                onSwitchCamera = {
                    hapticManager.dialClick()
                    val nextLens = if (state.lensFacing == CameraSelector.LENS_FACING_BACK) {
                        CameraSelector.LENS_FACING_FRONT
                    } else {
                        CameraSelector.LENS_FACING_BACK
                    }
                    state = state.copy(lensFacing = nextLens)
                }
            )
        }
    }
}

/**
 * Precision Viewfinder Window with custom aspect ratio cropping, framing reticle, and 90s Quartz Datestamp HUD.
 */
@Composable
private fun ViewfinderFrame(
    modifier: Modifier = Modifier,
    state: CameraUiState,
    quartzDateString: String,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.background(Color.Black)
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
