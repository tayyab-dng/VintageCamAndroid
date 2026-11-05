package com.example.vintagecam.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vintagecam.camera.CameraUiState
import com.example.vintagecam.camera.ProControlTab
import com.example.vintagecam.camera.VintageFilmPreset
import com.example.vintagecam.haptics.rememberHapticManager
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val BarBg = Color(0xFF121214)
private val BorderDark = Color(0xFF222327)
private val PillInactiveBg = Color(0xFF1F2024)
private val AmberAccent = Color(0xFFFFB300)
private val ActiveText = Color(0xFFEEEEEE)
private val MutedText = Color(0xFF888B92)

/**
 * Contextual Pro Control Bar featuring:
 * 1. Samsung Pro-style smooth draggable tick ruler in a sleek rounded pill (#1A1A1C).
 * 2. Matte-black 5-cell telemetry strip displaying FILM, ISO, SS, FOCUS, EV.
 * 3. Automatic EV locking and dimming (alpha=0.35f) when ISO or SS is manually set.
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
    onCloseDrawer: () -> Unit,
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
            visible = state.activeProTab != null && (state.activeProTab != ProControlTab.EV || !state.isEvLocked),
            enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                when (state.activeProTab) {
                    ProControlTab.FILM -> {
                        FilmPresetDrawer(
                            activePreset = state.filmPreset,
                            onSelectPreset = onFilmPresetSelected,
                            onClose = onCloseDrawer
                        )
                    }
                    ProControlTab.ISO -> {
                        val isoStops = state.supportedIsoStops.map { "$it" }
                        val selectedIdx = if (state.selectedIso != null) {
                            state.supportedIsoStops.indexOf(state.selectedIso).coerceAtLeast(0)
                        } else {
                            state.supportedIsoStops.indexOf(400).coerceAtLeast(0)
                        }

                        SamsungProTickRuler(
                            isAuto = state.isIsoAuto,
                            autoLabel = "AUTO",
                            manualLabel = "MANUAL",
                            stops = isoStops,
                            selectedIndex = selectedIdx,
                            onToggleAuto = {
                                if (state.isIsoAuto) {
                                    onIsoSelected(state.supportedIsoStops[selectedIdx])
                                } else {
                                    onIsoSelected(null)
                                }
                            },
                            onSelectIndex = { idx ->
                                onIsoSelected(state.supportedIsoStops[idx])
                            },
                            onClose = onCloseDrawer
                        )
                    }
                    ProControlTab.SS -> {
                        val ssStops = state.supportedShutterStops.map { it.label }
                        val selectedIdx = if (state.selectedShutterLabel != null) {
                            state.supportedShutterStops.indexOfFirst { it.label == state.selectedShutterLabel }.coerceAtLeast(0)
                        } else {
                            state.supportedShutterStops.indexOfFirst { it.label == "1/125" }.coerceAtLeast(0)
                        }

                        SamsungProTickRuler(
                            isAuto = state.isShutterAuto,
                            autoLabel = "AUTO",
                            manualLabel = "MANUAL",
                            stops = ssStops,
                            selectedIndex = selectedIdx,
                            onToggleAuto = {
                                if (state.isShutterAuto) {
                                    val stop = state.supportedShutterStops[selectedIdx]
                                    onShutterSpeedSelected(stop.label, stop.nanos)
                                } else {
                                    onShutterSpeedSelected(null, 0L)
                                }
                            },
                            onSelectIndex = { idx ->
                                val stop = state.supportedShutterStops[idx]
                                onShutterSpeedSelected(stop.label, stop.nanos)
                            },
                            onClose = onCloseDrawer
                        )
                    }
                    ProControlTab.FOCUS -> {
                        val focusSteps = listOf(
                            "Macro" to state.maxFocusDistance.coerceAtLeast(10f),
                            "0.2m" to 5.0f,
                            "0.5m" to 2.0f,
                            "1.0m" to 1.0f,
                            "2.0m" to 0.5f,
                            "5.0m" to 0.2f,
                            "∞" to 0.0f
                        )
                        val focusStops = focusSteps.map { it.first }
                        val selectedIdx = focusSteps.indexOfFirst { kotlin.math.abs(state.manualFocusDistance - it.second) < 0.25f }.let {
                            if (it >= 0) it else focusSteps.indexOfFirst { step -> step.first == "∞" }.coerceAtLeast(0)
                        }

                        SamsungProTickRuler(
                            isAuto = state.isAutoFocus,
                            autoLabel = "AF",
                            manualLabel = "MF",
                            stops = focusStops,
                            selectedIndex = selectedIdx,
                            onToggleAuto = {
                                onAutoFocusToggle(!state.isAutoFocus)
                            },
                            onSelectIndex = { idx ->
                                onManualFocusDistanceChanged(focusSteps[idx].second)
                            },
                            onClose = onCloseDrawer
                        )
                    }
                    ProControlTab.EV -> {
                        val evIndices = (state.minEvIndex..state.maxEvIndex).toList()
                        val evStops = evIndices.map { idx ->
                            val evVal = idx * state.evStep
                            if (idx == 0) "0.0" else if (idx > 0) "+%.1f".format(evVal) else "%.1f".format(evVal)
                        }
                        val selectedIdx = evIndices.indexOf(state.evIndex).coerceAtLeast(0)

                        SamsungProTickRuler(
                            isAuto = state.evIndex == 0,
                            autoLabel = "0.0",
                            manualLabel = "EV",
                            stops = evStops,
                            selectedIndex = selectedIdx,
                            onToggleAuto = {
                                onEvChanged(0.0f, 0)
                            },
                            onSelectIndex = { idx ->
                                val actualIdx = evIndices[idx]
                                val evVal = actualIdx * state.evStep
                                onEvChanged(evVal, actualIdx)
                            },
                            onClose = onCloseDrawer
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
            val isoValueText = if (state.isIsoAuto) "AUTO" else "${state.selectedIso}"
            TelemetryCell(
                modifier = Modifier.weight(1f),
                label = "ISO",
                value = isoValueText,
                isActive = state.activeProTab == ProControlTab.ISO,
                accentColor = AmberAccent,
                onClick = { onTabSelected(ProControlTab.ISO) }
            )

            // Tab 3: SS (Shutter Speed)
            val ssValueText = if (state.isShutterAuto) "AUTO" else (state.selectedShutterLabel ?: "AUTO")
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

            // Tab 5: EV (Locked & dimmed when ISO or SS is manually set)
            val isEvLocked = state.isEvLocked
            val evValueText = if (isEvLocked) {
                "LOCK"
            } else if (state.evValue == 0.0f) {
                "±0.0"
            } else if (state.evValue > 0.0f) {
                "+%.1f".format(state.evValue)
            } else {
                "%.1f".format(state.evValue)
            }
            TelemetryCell(
                modifier = Modifier
                    .weight(1f)
                    .alpha(if (isEvLocked) 0.35f else 1.0f),
                label = "EV",
                value = evValueText,
                isActive = !isEvLocked && state.activeProTab == ProControlTab.EV,
                accentColor = AmberAccent,
                onClick = {
                    if (!isEvLocked) {
                        onTabSelected(ProControlTab.EV)
                    }
                }
            )
        }
    }
}

/**
 * Samsung One UI Pro Camera inspired horizontal draggable tick ruler.
 * Renders inside a sleek dark rounded pill container (#1A1A1C) with:
 * - Left: Compact MANUAL/AUTO toggle pill button.
 * - Center: Canvas tick-mark scale with fixed center needle, active value, and smooth haptic feedback.
 * - Right: Circular ✕ close button.
 */
@Composable
private fun SamsungProTickRuler(
    isAuto: Boolean,
    autoLabel: String,
    manualLabel: String,
    stops: List<String>,
    selectedIndex: Int,
    onToggleAuto: () -> Unit,
    onSelectIndex: (Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticManager = rememberHapticManager()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val spacingPx = with(density) { 46.dp.toPx() }
    val textMeasurer = rememberTextMeasurer()

    val safeIndex = selectedIndex.coerceIn(0, (stops.size - 1).coerceAtLeast(0))
    val scrollAnim = remember { Animatable(safeIndex.toFloat()) }
    var isDragging by remember { mutableStateOf(false) }
    var lastReportedIndex by remember { mutableIntStateOf(safeIndex) }

    LaunchedEffect(safeIndex) {
        if (!isDragging) {
            scrollAnim.animateTo(
                safeIndex.toFloat(),
                spring(stiffness = Spring.StiffnessMediumLow)
            )
            lastReportedIndex = safeIndex
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFF1A1A1C))
            .border(1.dp, Color(0xFF2E2F34), RoundedCornerShape(28.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: Compact MANUAL / AUTO toggle pill button
        Box(
            modifier = Modifier
                .width(60.dp)
                .height(34.dp)
                .clip(RoundedCornerShape(17.dp))
                .background(if (isAuto) AmberAccent else Color(0xFF26272B))
                .border(
                    1.dp,
                    if (isAuto) AmberAccent else Color(0xFF383A40),
                    RoundedCornerShape(17.dp)
                )
                .clickable { onToggleAuto() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isAuto) autoLabel else manualLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.5.sp,
                    letterSpacing = 0.5.sp
                ),
                color = if (isAuto) Color(0xFF121214) else AmberAccent
            )
        }

        // Center: Horizontally draggable tick-mark scale
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 4.dp)
                .pointerInput(stops.size) {
                    detectTapGestures { tapOffset ->
                        val centerX = size.width / 2f
                        val offsetFromCenter = tapOffset.x - centerX
                        val tappedIndex = (scrollAnim.value + (offsetFromCenter / spacingPx))
                            .roundToInt()
                            .coerceIn(0, stops.size - 1)
                        if (tappedIndex != lastReportedIndex) {
                            hapticManager.dialClick()
                            lastReportedIndex = tappedIndex
                            onSelectIndex(tappedIndex)
                        }
                        coroutineScope.launch {
                            scrollAnim.animateTo(
                                tappedIndex.toFloat(),
                                spring(stiffness = Spring.StiffnessMediumLow)
                            )
                        }
                    }
                }
                .pointerInput(stops.size) {
                    detectHorizontalDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = {
                            isDragging = false
                            val snapTarget = scrollAnim.value.roundToInt().coerceIn(0, stops.size - 1)
                            coroutineScope.launch {
                                scrollAnim.animateTo(
                                    snapTarget.toFloat(),
                                    spring(stiffness = Spring.StiffnessMediumLow)
                                )
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                            val snapTarget = scrollAnim.value.roundToInt().coerceIn(0, stops.size - 1)
                            coroutineScope.launch {
                                scrollAnim.animateTo(
                                    snapTarget.toFloat(),
                                    spring(stiffness = Spring.StiffnessMediumLow)
                                )
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val deltaIndex = -dragAmount / spacingPx
                            val newTarget = (scrollAnim.value + deltaIndex)
                                .coerceIn(0f, (stops.size - 1).toFloat())
                            coroutineScope.launch {
                                scrollAnim.snapTo(newTarget)
                            }
                            val nearestIdx = newTarget.roundToInt().coerceIn(0, stops.size - 1)
                            if (nearestIdx != lastReportedIndex) {
                                hapticManager.dialClick()
                                lastReportedIndex = nearestIdx
                                onSelectIndex(nearestIdx)
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val centerX = canvasWidth / 2f
                val currentFloat = scrollAnim.value

                // Draw stops and tick marks
                stops.forEachIndexed { i, stopLabel ->
                    val stopX = centerX + (i - currentFloat) * spacingPx
                    if (stopX in -spacingPx..(canvasWidth + spacingPx)) {
                        val distToCenter = kotlin.math.abs(stopX - centerX)
                        val visibilityAlpha = (1f - (distToCenter / (canvasWidth / 2f))).coerceIn(0f, 1f)
                        val isClosest = distToCenter < (spacingPx * 0.48f)

                        // Major tick line
                        val tickTop = canvasHeight * 0.48f
                        val tickBottom = canvasHeight - 3.dp.toPx()
                        val tickColor = if (isClosest) {
                            AmberAccent
                        } else {
                            Color(0xFF72757D).copy(alpha = visibilityAlpha)
                        }

                        drawLine(
                            color = tickColor,
                            start = Offset(stopX, tickTop),
                            end = Offset(stopX, tickBottom),
                            strokeWidth = if (isClosest) 2.2.dp.toPx() else 1.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )

                        // Minor ticks between stops
                        if (i < stops.size - 1) {
                            for (step in 1..2) {
                                val minorX = stopX + step * (spacingPx / 3f)
                                val minorDist = kotlin.math.abs(minorX - centerX)
                                val minorAlpha = (1f - (minorDist / (canvasWidth / 2f))).coerceIn(0f, 1f) * 0.5f
                                drawLine(
                                    color = Color(0xFF45474E).copy(alpha = minorAlpha),
                                    start = Offset(minorX, canvasHeight * 0.65f),
                                    end = Offset(minorX, tickBottom),
                                    strokeWidth = 1.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            }
                        }

                        // Stop labels
                        if (isClosest) {
                            // Active value bolded in amber above the center needle
                            val textLayout = textMeasurer.measure(
                                text = stopLabel,
                                style = TextStyle(
                                    color = AmberAccent,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                            drawText(
                                textLayoutResult = textLayout,
                                topLeft = Offset(
                                    stopX - (textLayout.size.width / 2f),
                                    0f
                                )
                            )
                        } else if (visibilityAlpha > 0.25f) {
                            // Neighbor stop labels in subtle gray above their respective major ticks
                            val textLayout = textMeasurer.measure(
                                text = stopLabel,
                                style = TextStyle(
                                    color = Color(0xFF8E9199).copy(alpha = visibilityAlpha * 0.85f),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Normal,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                            drawText(
                                textLayoutResult = textLayout,
                                topLeft = Offset(
                                    stopX - (textLayout.size.width / 2f),
                                    2.dp.toPx()
                                )
                            )
                        }
                    }
                }

                // Fixed vertical amber/gold center indicator needle (#FFB300)
                drawLine(
                    color = AmberAccent,
                    start = Offset(centerX, 15.dp.toPx()),
                    end = Offset(centerX, canvasHeight - 2.dp.toPx()),
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        // Right: Circular ✕ close button (28.dp) that collapses the active drawer
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color(0xFF26272B))
                .border(1.dp, Color(0xFF383A40), CircleShape)
                .clickable { onClose() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "✕",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                ),
                color = Color(0xFFB4B7BF)
            )
        }
    }
}

/**
 * Contextual Drawer 1: Film Simulation Stocks in clean rounded container with close button.
 */
@Composable
private fun FilmPresetDrawer(
    activePreset: VintageFilmPreset,
    onSelectPreset: (VintageFilmPreset) -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFF1A1A1C))
            .border(1.dp, Color(0xFF2E2F34), RoundedCornerShape(28.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(VintageFilmPreset.values()) { preset ->
                val isSelected = preset == activePreset
                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(19.dp))
                        .background(if (isSelected) PillInactiveBg else Color(0xFF222327))
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) preset.badgeColor else Color(0xFF34353B),
                            shape = RoundedCornerShape(19.dp)
                        )
                        .clickable { onSelectPreset(preset) }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = preset.title,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp
                            ),
                            color = if (isSelected) preset.badgeColor else ActiveText
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Circular ✕ close button
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color(0xFF26272B))
                .border(1.dp, Color(0xFF383A40), CircleShape)
                .clickable { onClose() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "✕",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                ),
                color = Color(0xFFB4B7BF)
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
