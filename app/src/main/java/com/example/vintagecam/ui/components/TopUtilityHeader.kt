package com.example.vintagecam.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FlashAuto
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.GridOff
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vintagecam.camera.CameraUiState
import com.example.vintagecam.camera.VintageAspectRatio
import com.example.vintagecam.camera.VintageFlashMode

private val HeaderBg = Color(0xFF121214)
private val PillBg = Color(0xFF1B1C20)
private val PillBorder = Color(0xFF2C2D32)
private val AmberAccent = Color(0xFFFFB03B)
private val QuartzOrange = Color(0xFFFF7A00)
private val MutedText = Color(0xFF75787F)
private val ActiveText = Color(0xFFE5E7EB)

/**
 * Sleek, single-row minimalist utility header.
 * Left: EXP XX/36 frame counter badge.
 * Right: Clean icon/pill toggles for Aspect Ratio, Flash mode, Framing Grid, and Quartz Date stamp.
 */
@Composable
fun TopUtilityHeader(
    state: CameraUiState,
    onAspectRatioToggle: () -> Unit,
    onFlashToggle: () -> Unit,
    onGridToggle: () -> Unit,
    onDateStampToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .background(HeaderBg)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: 35mm Mechanical Frame Counter Badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(PillBg)
                .border(1.dp, PillBorder, RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(
                text = "EXP ${state.frameCounter.toString().padStart(2, '0')}/${state.maxFrames}",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                ),
                color = AmberAccent
            )
        }

        // Right: Functional Quick-Toggle Utility Controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Aspect Ratio Pill (4:3, 3:2, 9:16)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(PillBg)
                    .border(1.dp, PillBorder, RoundedCornerShape(6.dp))
                    .clickable { onAspectRatioToggle() }
                    .padding(horizontal = 9.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.aspectRatio.title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    color = ActiveText
                )
            }

            // 2. Flash Mode Pill (AUTO, ON, OFF)
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(PillBg)
                    .border(1.dp, PillBorder, RoundedCornerShape(6.dp))
                    .clickable { onFlashToggle() },
                contentAlignment = Alignment.Center
            ) {
                val (flashIcon, flashColor) = when (state.flashMode) {
                    VintageFlashMode.AUTO -> Icons.Rounded.FlashAuto to AmberAccent
                    VintageFlashMode.ON -> Icons.Rounded.FlashOn to AmberAccent
                    VintageFlashMode.OFF -> Icons.Rounded.FlashOff to MutedText
                }
                Icon(
                    imageVector = flashIcon,
                    contentDescription = "Flash Mode",
                    tint = flashColor,
                    modifier = Modifier.size(17.dp)
                )
            }

            // 3. Framing Grid Toggle
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(PillBg)
                    .border(1.dp, PillBorder, RoundedCornerShape(6.dp))
                    .clickable { onGridToggle() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (state.isGridVisible) Icons.Rounded.GridOn else Icons.Rounded.GridOff,
                    contentDescription = "Framing Grid",
                    tint = if (state.isGridVisible) ActiveText else MutedText,
                    modifier = Modifier.size(17.dp)
                )
            }

            // 4. Quartz Datestamp Toggle
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(PillBg)
                    .border(
                        width = 1.dp,
                        color = if (state.isDateStampEnabled) QuartzOrange.copy(alpha = 0.5f) else PillBorder,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .clickable { onDateStampToggle() }
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "'26",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    color = if (state.isDateStampEnabled) QuartzOrange else MutedText,
                    modifier = if (state.isDateStampEnabled) {
                        Modifier.shadow(2.dp, spotColor = QuartzOrange, ambientColor = QuartzOrange)
                    } else Modifier
                )
            }
        }
    }
}

