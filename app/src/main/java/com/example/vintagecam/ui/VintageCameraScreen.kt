package com.example.vintagecam.ui

import android.os.Environment
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.Cameraswitch
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
import com.example.vintagecam.theme.VintageAmber
import com.example.vintagecam.theme.VintageBlack
import com.example.vintagecam.theme.VintageBodyDark
import com.example.vintagecam.theme.VintageGold
import com.example.vintagecam.theme.VintageLeatherGrip
import com.example.vintagecam.theme.VintageMetalDark
import com.example.vintagecam.theme.VintageMetalSilver
import com.example.vintagecam.theme.VintageShutterRed
import com.example.vintagecam.theme.VintageShutterRedDark
import com.example.vintagecam.theme.VintageSurfaceElevated
import com.example.vintagecam.theme.VintageTextLight
import com.example.vintagecam.theme.VintageTextMuted
import kotlinx.coroutines.launch

@Composable
fun VintageCameraScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val controller = rememberVintageCameraController()

    var state by remember { mutableStateOf(CameraUiState()) }
    var showFilmTray by remember { mutableStateOf(false) }

    // Shutter animation states
    val shutterAlpha = remember { Animatable(0f) }
    val flashAlpha = remember { Animatable(0f) }

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
            // 1. TOP ANALOG TELEMETRY & CONTROLS DECK
            TopControlsDeck(
                state = state,
                onFlashToggle = {
                    val nextFlash = when (state.flashMode) {
                        VintageFlashMode.AUTO -> VintageFlashMode.ON
                        VintageFlashMode.ON -> VintageFlashMode.OFF
                        VintageFlashMode.OFF -> VintageFlashMode.AUTO
                    }
                    state = state.copy(flashMode = nextFlash)
                },
                onAspectRatioToggle = {
                    val nextRatio = when (state.aspectRatio) {
                        VintageAspectRatio.RATIO_4_3 -> VintageAspectRatio.RATIO_16_9
                        VintageAspectRatio.RATIO_16_9 -> VintageAspectRatio.RATIO_4_3
                    }
                    state = state.copy(aspectRatio = nextRatio)
                },
                onGridToggle = {
                    state = state.copy(isGridVisible = !state.isGridVisible)
                },
                onToggleFilmTray = {
                    showFilmTray = !showFilmTray
                }
            )

            // 2. FILM TRAY POPUP (Expandable film simulations)
            AnimatedVisibility(
                visible = showFilmTray,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                FilmSimulationSelector(
                    activePreset = state.filmPreset,
                    onSelectPreset = { preset ->
                        state = state.copy(filmPreset = preset)
                        showFilmTray = false
                    }
                )
            }

            // 3. CENTRAL VIEWFINDER (With aspect ratio framing & vintage markings)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                ViewfinderFrame(
                    state = state,
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
                            onCaptureStarted = {
                                coroutineScope.launch {
                                    // Trigger realistic leaf-shutter wipe
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
                                    "FRAME #${state.frameCounter} SAVED TO FILM ROLL",
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

            // 4. BOTTOM ANALOG BODY & SHUTTER DECK
            BottomControlsDeck(
                state = state,
                onShutterClick = {
                    coroutineScope.launch {
                        // If flash is enabled, flash pop animation
                        if (state.flashMode != VintageFlashMode.OFF) {
                            flashAlpha.snapTo(0.85f)
                            flashAlpha.animateTo(0f, tween(180, easing = FastOutSlowInEasing))
                        }

                        // Shutter blackout animation
                        shutterAlpha.snapTo(1f)
                        shutterAlpha.animateTo(0f, tween(100, easing = LinearEasing))

                        val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                            ?: context.filesDir
                        controller.takePhoto(context, outputDir)
                    }
                },
                onSwitchCamera = {
                    val nextLens = if (state.lensFacing == CameraSelector.LENS_FACING_BACK) {
                        CameraSelector.LENS_FACING_FRONT
                    } else {
                        CameraSelector.LENS_FACING_BACK
                    }
                    state = state.copy(lensFacing = nextLens)
                },
                onFilmBadgeClick = {
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
    onToggleFilmTray: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(VintageBodyDark)
            .border(width = 1.dp, color = VintageMetalDark)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Analog LCD Film Counter Window
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(VintageBlack)
                .border(1.dp, VintageMetalDark, RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "EXP",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                ),
                color = VintageTextMuted
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = String.format("%02d/%02d", state.frameCounter, state.maxFrames),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
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
                .padding(horizontal = 12.dp, vertical = 6.dp)
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

        // Quick Controls (Aspect ratio, Grid, Flash)
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Aspect Ratio Switcher
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(VintageLeatherGrip)
                    .border(1.dp, VintageMetalDark, RoundedCornerShape(6.dp))
                    .clickable { onAspectRatioToggle() }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
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

            // Grid Toggle
            IconButton(
                onClick = onGridToggle,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.GridOn,
                    contentDescription = "Toggle Grid",
                    tint = if (state.isGridVisible) VintageAmber else VintageMetalDark,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Flash Mode Toggle
            IconButton(
                onClick = onFlashToggle,
                modifier = Modifier.size(36.dp)
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
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Viewfinder frame with tactile vintage corner crop brackets and optical rangefinder markings.
 */
@Composable
private fun ViewfinderFrame(
    state: CameraUiState,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(state.aspectRatio.floatRatio)
            .clip(RoundedCornerShape(12.dp))
            .border(2.dp, VintageLeatherGrip, RoundedCornerShape(12.dp))
            .background(Color.Black)
    ) {
        // Core CameraX Viewfinder with OpenGL ES 3.0 Shader Processing
        content()

        // Viewfinder Reticle & Rangefinder Markings Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidthPx = 1.5.dp.toPx()
            val bracketLen = 22.dp.toPx()
            val cornerOffset = 16.dp.toPx()
            val reticleColor = Color(0xFFFFB03B).copy(alpha = 0.85f)
            val gridColor = Color(0x33FFFFFF)

            // Rule-of-Thirds Grid
            if (state.isGridVisible) {
                // Vertical lines
                val oneThirdW = size.width / 3f
                drawLine(gridColor, Offset(oneThirdW, 0f), Offset(oneThirdW, size.height), 1.dp.toPx())
                drawLine(gridColor, Offset(oneThirdW * 2f, 0f), Offset(oneThirdW * 2f, size.height), 1.dp.toPx())

                // Horizontal lines
                val oneThirdH = size.height / 3f
                drawLine(gridColor, Offset(0f, oneThirdH), Offset(size.width, oneThirdH), 1.dp.toPx())
                drawLine(gridColor, Offset(0f, oneThirdH * 2f), Offset(size.width, oneThirdH * 2f), 1.dp.toPx())
            }

            // Top-Left Corner Bracket
            drawLine(reticleColor, Offset(cornerOffset, cornerOffset), Offset(cornerOffset + bracketLen, cornerOffset), strokeWidthPx)
            drawLine(reticleColor, Offset(cornerOffset, cornerOffset), Offset(cornerOffset, cornerOffset + bracketLen), strokeWidthPx)

            // Top-Right Corner Bracket
            drawLine(reticleColor, Offset(size.width - cornerOffset, cornerOffset), Offset(size.width - cornerOffset - bracketLen, cornerOffset), strokeWidthPx)
            drawLine(reticleColor, Offset(size.width - cornerOffset, cornerOffset), Offset(size.width - cornerOffset, cornerOffset + bracketLen), strokeWidthPx)

            // Bottom-Left Corner Bracket
            drawLine(reticleColor, Offset(cornerOffset, size.height - cornerOffset), Offset(cornerOffset + bracketLen, size.height - cornerOffset), strokeWidthPx)
            drawLine(reticleColor, Offset(cornerOffset, size.height - cornerOffset), Offset(cornerOffset, size.height - cornerOffset - bracketLen), strokeWidthPx)

            // Bottom-Right Corner Bracket
            drawLine(reticleColor, Offset(size.width - cornerOffset, size.height - cornerOffset), Offset(size.width - cornerOffset - bracketLen, size.height - cornerOffset), strokeWidthPx)
            drawLine(reticleColor, Offset(size.width - cornerOffset, size.height - cornerOffset), Offset(size.width - cornerOffset, size.height - cornerOffset - bracketLen), strokeWidthPx)

            // Center Rangefinder Microprism Circle
            drawCircle(
                color = reticleColor.copy(alpha = 0.4f),
                radius = 28.dp.toPx(),
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // Viewfinder Analog Telemetry Header
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0x99111111))
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = "SELECT FILM EMULSION",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold
            ),
            color = VintageAmber,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
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
                        .padding(horizontal = 14.dp, vertical = 10.dp),
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
            .padding(horizontal = 24.dp, vertical = 16.dp),
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
                text = "VINTAGECAM 35mm • OPTICS SYSTEM",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                ),
                color = VintageTextLight
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

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
                    // Micro center dimple
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
