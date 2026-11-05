package com.example.vintagecam.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vintagecam.camera.CameraUiState
import com.example.vintagecam.camera.ProControlTab
import com.example.vintagecam.camera.ShutterSpeedStop
import com.example.vintagecam.camera.VintageFilmPreset
import kotlin.math.roundToInt

private val BarBg = Color(0xFF121214)
private val DrawerBg = Color(0xFF16171A)
private val BorderDark = Color(0xFF222327)
private val PillActiveBg = Color(0xFFFFB03B)
private val PillInactiveBg = Color(0xFF1F2024)
private val AmberAccent = Color(0xFFFFB03B)
private val ActiveText = Color(0xFFEEEEEE)
private val MutedText = Color(0xFF888B92)

/**
 * Contextual Pro Control Bar featuring:
 * 1. Expandable single-dial pro drawer directly above the telemetry strip (smooth AnimatedVisibility).
 * 2. Matte-black 5-cell telemetry strip displaying FILM, ISO, SS, FOCUS, EV.
 */
@Composable
fun ProControlBar(
    state: CameraUiState,
    onTabSelected: (ProControlTab) -> Unit,
    onFilmPresetSelected: (VintageFilmPreset) -> Unit,
    onIsoSelected: (Int?) -> Unit, // null = Auto
    onShutterSpeedSelected: (String?, Long) -> Unit, // null = Auto
    onAutoFocusToggle: (Boolean) -> Unit,
    onManualFocusDistanceChanged: (Float) -> Unit,
    onEvChanged: (Float, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(BarBg)
    ) {
        // ==========================================
        // 1. CONTEXTUAL DRAWER (EXPANDS ABOVE TELEMETRY STRIP)
        // ==========================================
        AnimatedVisibility(
            visible = state.activeProTab != null,
            enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .background(DrawerBg)
                    .border(1.dp, BorderDark)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                when (state.activeProTab) {
                    ProControlTab.FILM -> {
                        FilmPresetDrawer(
                            activePreset = state.filmPreset,
                            onSelectPreset = onFilmPresetSelected
                        )
                    }
                    ProControlTab.ISO -> {
                        IsoVernierDrawer(
                            isAuto = state.isAutoExposure || state.selectedIso == null,
                            selectedIso = state.selectedIso,
                            supportedStops = state.supportedIsoStops,
                            onSelectIso = onIsoSelected
                        )
                    }
                    ProControlTab.SS -> {
                        ShutterSpeedDrawer(
                            isAuto = state.isAutoExposure || state.selectedShutterLabel == null,
                            selectedLabel = state.selectedShutterLabel,
                            supportedStops = state.supportedShutterStops,
                            onSelectShutter = onShutterSpeedSelected
                        )
                    }
                    ProControlTab.FOCUS -> {
                        ManualFocusDrawer(
                            isAuto = state.isAutoFocus,
                            currentDistance = state.manualFocusDistance,
                            maxDistance = state.maxFocusDistance,
                            onToggleAuto = onAutoFocusToggle,
                            onDistanceChanged = onManualFocusDistanceChanged
                        )
                    }
                    ProControlTab.EV -> {
                        EvExposureDrawer(
                            currentEv = state.evValue,
                            currentIndex = state.evIndex,
                            minEvIndex = state.minEvIndex,
                            maxEvIndex = state.maxEvIndex,
                            evStep = state.evStep,
                            onEvSelected = onEvChanged
                        )
                    }
                    null -> Unit
                }
            }
        }

        // ==========================================
        // 2. MATTE-BLACK 5-CELL TELEMETRY STRIP
        // ==========================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .border(1.dp, BorderDark),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tab 1: FILM
            val filmShortName = when (state.filmPreset) {
                VintageFilmPreset.PORTRA_400 -> "PORTRA"
                VintageFilmPreset.CINESTILL_800T -> "CINE800"
                VintageFilmPreset.FUJI_VELVIA -> "VELVIA"
                VintageFilmPreset.ILFORD_HP5 -> "HP5 400"
                VintageFilmPreset.POINT_AND_SHOOT -> "RETRO80"
                VintageFilmPreset.STANDARD -> "STANDARD"
            }
            TelemetryCell(
                modifier = Modifier.weight(1f),
                label = "FILM",
                value = filmShortName,
                isActive = state.activeProTab == ProControlTab.FILM,
                accentColor = state.filmPreset.badgeColor,
                onClick = { onTabSelected(ProControlTab.FILM) }
            )

            // Tab 2: ISO
            val isoValueText = if (state.isAutoExposure || state.selectedIso == null) "AUTO" else "${state.selectedIso}"
            TelemetryCell(
                modifier = Modifier.weight(1f),
                label = "ISO",
                value = isoValueText,
                isActive = state.activeProTab == ProControlTab.ISO,
                accentColor = AmberAccent,
                onClick = { onTabSelected(ProControlTab.ISO) }
            )

            // Tab 3: SS (Shutter Speed)
            val ssValueText = if (state.isAutoExposure || state.selectedShutterLabel == null) "AUTO" else state.selectedShutterLabel
            TelemetryCell(
                modifier = Modifier.weight(1f),
                label = "SS",
                value = ssValueText,
                isActive = state.activeProTab == ProControlTab.SS,
                accentColor = AmberAccent,
                onClick = { onTabSelected(ProControlTab.SS) }
            )

            // Tab 4: FOCUS
            val focusValueText = if (state.isAutoFocus) {
                "AF"
            } else if (state.manualFocusDistance <= 0.05f) {
                "∞"
            } else {
                "%.1fm".format(1f / state.manualFocusDistance.coerceAtLeast(0.1f))
            }
            TelemetryCell(
                modifier = Modifier.weight(1f),
                label = "FOCUS",
                value = focusValueText,
                isActive = state.activeProTab == ProControlTab.FOCUS,
                accentColor = AmberAccent,
                onClick = { onTabSelected(ProControlTab.FOCUS) }
            )

            // Tab 5: EV
            val evValueText = if (state.evValue == 0.0f) {
                "±0.0"
            } else if (state.evValue > 0.0f) {
                "+%.1f".format(state.evValue)
            } else {
                "%.1f".format(state.evValue)
            }
            TelemetryCell(
                modifier = Modifier.weight(1f),
                label = "EV",
                value = evValueText,
                isActive = state.activeProTab == ProControlTab.EV,
                accentColor = AmberAccent,
                onClick = { onTabSelected(ProControlTab.EV) }
            )
        }
    }
}

/**
 * Clean telemetry cell with discrete label and value, plus active indicator line.
 */
@Composable
private fun TelemetryCell(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    isActive: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                letterSpacing = 1.sp
            ),
            color = if (isActive) accentColor else MutedText
        )

        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            ),
            color = if (isActive) accentColor else ActiveText,
            maxLines = 1
        )

        // Bottom active accent line
        Box(
            modifier = Modifier
                .height(2.dp)
                .fillMaxWidth(0.6f)
                .background(if (isActive) accentColor else Color.Transparent)
        )
    }
}

/**
 * Contextual Drawer 1: Film Simulation Stocks
 */
@Composable
private fun FilmPresetDrawer(
    activePreset: VintageFilmPreset,
    onSelectPreset: (VintageFilmPreset) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(VintageFilmPreset.values()) { preset ->
            val isSelected = preset == activePreset
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) PillInactiveBg else Color(0xFF1A1B1E))
                    .border(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) preset.badgeColor else BorderDark,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .clickable { onSelectPreset(preset) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = preset.title,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = if (isSelected) preset.badgeColor else ActiveText
                    )
                    Text(
                        text = preset.subtitle,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        color = MutedText
                    )
                }
            }
        }
    }
}

/**
 * Contextual Drawer 2: ISO Precision Vernier Dial
 */
@Composable
private fun IsoVernierDrawer(
    isAuto: Boolean,
    selectedIso: Int?,
    supportedStops: List<Int>,
    onSelectIso: (Int?) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Left: Dedicated AUTO Pill Button
        Box(
            modifier = Modifier
                .size(width = 54.dp, height = 38.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (isAuto) PillActiveBg else PillInactiveBg)
                .border(1.dp, if (isAuto) AmberAccent else BorderDark, RoundedCornerShape(6.dp))
                .clickable { onSelectIso(null) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "AUTO",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                ),
                color = if (isAuto) Color.Black else MutedText
            )
        }

        // Right: Scrollable Discrete Hardware ISO Stops
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(supportedStops) { iso ->
                val isSelected = !isAuto && selectedIso == iso
                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) PillInactiveBg else Color(0xFF18191C))
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) AmberAccent else BorderDark,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable { onSelectIso(iso) }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$iso",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        ),
                        color = if (isSelected) AmberAccent else ActiveText
                    )
                }
            }
        }
    }
}

/**
 * Contextual Drawer 3: Shutter Speed Precision Vernier Dial
 */
@Composable
private fun ShutterSpeedDrawer(
    isAuto: Boolean,
    selectedLabel: String?,
    supportedStops: List<ShutterSpeedStop>,
    onSelectShutter: (String?, Long) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Left: Dedicated AUTO Pill Button
        Box(
            modifier = Modifier
                .size(width = 54.dp, height = 38.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (isAuto) PillActiveBg else PillInactiveBg)
                .border(1.dp, if (isAuto) AmberAccent else BorderDark, RoundedCornerShape(6.dp))
                .clickable { onSelectShutter(null, 0L) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "AUTO",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                ),
                color = if (isAuto) Color.Black else MutedText
            )
        }

        // Right: Scrollable Discrete Hardware Shutter Stops
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(supportedStops) { stop ->
                val isSelected = !isAuto && selectedLabel == stop.label
                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) PillInactiveBg else Color(0xFF18191C))
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) AmberAccent else BorderDark,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable { onSelectShutter(stop.label, stop.nanos) }
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stop.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp
                        ),
                        color = if (isSelected) AmberAccent else ActiveText
                    )
                }
            }
        }
    }
}

/**
 * Contextual Drawer 4: Manual Focus Precision Dial
 */
@Composable
private fun ManualFocusDrawer(
    isAuto: Boolean,
    currentDistance: Float,
    maxDistance: Float,
    onToggleAuto: (Boolean) -> Unit,
    onDistanceChanged: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Left: Dedicated AF (Auto Focus) Pill Button
        Box(
            modifier = Modifier
                .size(width = 54.dp, height = 38.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (isAuto) PillActiveBg else PillInactiveBg)
                .border(1.dp, if (isAuto) AmberAccent else BorderDark, RoundedCornerShape(6.dp))
                .clickable { onToggleAuto(true) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "AF",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                ),
                color = if (isAuto) Color.Black else MutedText
            )
        }

        // Discrete Focus Distance Marks (Macro to Infinity)
        val focusSteps = listOf(
            "Macro" to maxDistance.coerceAtLeast(10f),
            "0.2m" to 5.0f,
            "0.5m" to 2.0f,
            "1.0m" to 1.0f,
            "2.0m" to 0.5f,
            "5.0m" to 0.2f,
            "∞" to 0.0f
        )

        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(focusSteps) { (label, diopters) ->
                val isSelected = !isAuto && kotlin.math.abs(currentDistance - diopters) < 0.25f
                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) PillInactiveBg else Color(0xFF18191C))
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) AmberAccent else BorderDark,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable { onDistanceChanged(diopters) }
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        ),
                        color = if (isSelected) AmberAccent else ActiveText
                    )
                }
            }
        }
    }
}

/**
 * Contextual Drawer 5: EV Exposure Compensation Vernier Dial
 */
@Composable
private fun EvExposureDrawer(
    currentEv: Float,
    currentIndex: Int,
    minEvIndex: Int,
    maxEvIndex: Int,
    evStep: Float,
    onEvSelected: (Float, Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Left: Dedicated 0.0 Center Reset Button
        val isZero = currentIndex == 0
        Box(
            modifier = Modifier
                .size(width = 54.dp, height = 38.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (isZero) PillActiveBg else PillInactiveBg)
                .border(1.dp, if (isZero) AmberAccent else BorderDark, RoundedCornerShape(6.dp))
                .clickable { onEvSelected(0f, 0) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "±0.0",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                ),
                color = if (isZero) Color.Black else MutedText
            )
        }

        // Discrete 1/3-Stop EV Steps
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items((minEvIndex..maxEvIndex).toList()) { index ->
                val evVal = index * evStep
                val isSelected = index == currentIndex
                val label = if (index == 0) "0.0" else if (index > 0) "+%.1f".format(evVal) else "%.1f".format(evVal)

                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) PillInactiveBg else Color(0xFF18191C))
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) AmberAccent else BorderDark,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable { onEvSelected(evVal, index) }
                        .padding(horizontal = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp
                        ),
                        color = if (isSelected) AmberAccent else ActiveText
                    )
                }
            }
        }
    }
}

