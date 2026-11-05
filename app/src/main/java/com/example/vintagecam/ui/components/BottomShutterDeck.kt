package com.example.vintagecam.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FlipCameraAndroid
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.vintagecam.camera.CameraUiState

private val DeckBg = Color(0xFF121214)
private val ElementBg = Color(0xFF1A1B1E)
private val ElementBorder = Color(0xFF2C2D32)
private val ShutterRed = Color(0xFFE53935)

/**
 * Clean, minimal bottom shutter deck matching classic Leica/Halide industrial design.
 * Left: 48.dp square Gallery thumbnail.
 * Center: 72.dp machined concentric aluminum & subtle red concave shutter button.
 * Right: 48.dp circular Camera Flip button.
 */
@Composable
fun BottomShutterDeck(
    state: CameraUiState,
    onShutterClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onSwitchCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp)
            .background(DeckBg)
            .padding(horizontal = 28.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Left: 48.dp Rounded Square Gallery Thumbnail
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(ElementBg)
                .border(1.dp, ElementBorder, RoundedCornerShape(10.dp))
                .clickable { onGalleryClick() },
            contentAlignment = Alignment.Center
        ) {
            val thumbnail = state.lastThumbnail
            if (thumbnail != null && !thumbnail.isRecycled) {
                Image(
                    bitmap = thumbnail.asImageBitmap(),
                    contentDescription = "Gallery Thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.PhotoLibrary,
                    contentDescription = "Open Gallery",
                    tint = Color(0xFF75787F),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // 2. Center: 72.dp Machined Aluminum & Red Leica Shutter Button
        var isShutterPressed by remember { mutableStateOf(false) }
        val shutterScale by animateFloatAsState(
            targetValue = if (isShutterPressed || state.isCapturing) 0.92f else 1.0f,
            animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f),
            label = "ShutterScale"
        )

        Box(
            modifier = Modifier
                .size(74.dp)
                .scale(shutterScale)
                .shadow(elevation = 8.dp, shape = CircleShape, spotColor = Color.Black)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF42444A), Color(0xFF242528), Color(0xFF141517))
                    )
                )
                .border(2.dp, Brush.linearGradient(listOf(Color(0xFF5E6068), Color(0xFF2B2C30))), CircleShape)
                .pointerInput(state.isCapturing) {
                    detectTapGestures(
                        onPress = {
                            if (!state.isCapturing) {
                                isShutterPressed = true
                                tryAwaitRelease()
                                isShutterPressed = false
                                onShutterClick()
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // Inner Concave Button (56.dp)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFF28292D), Color(0xFF161719))
                        )
                    )
                    .border(1.dp, Color(0xFF383A40), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Subtle Leica-inspired Concave Red Core
                Box(
                    modifier = Modifier
                        .size(15.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(ShutterRed, Color(0xFFB71C1C))
                            )
                        )
                        .border(1.dp, Color(0xFFFF5252).copy(alpha = 0.5f), CircleShape)
                )
            }
        }

        // 3. Right: 48.dp Circular Camera Flip Button
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(ElementBg)
                .border(1.dp, ElementBorder, CircleShape)
                .clickable { onSwitchCamera() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.FlipCameraAndroid,
                contentDescription = "Switch Camera Lens",
                tint = Color(0xFFC0C2C8),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

