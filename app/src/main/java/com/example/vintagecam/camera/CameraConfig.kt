package com.example.vintagecam.camera

import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.compose.ui.graphics.Color

/**
 * Supported aspect ratios for vintage framing.
 * 3:2 is the iconic 35mm film negative standard (24x36mm).
 * 4:3 is the classic medium format and point-and-shoot standard.
 * 9:16 is modern vertical mobile framing.
 */
enum class VintageAspectRatio(val title: String, val cameraRatio: Int, val floatRatio: Float) {
    RATIO_3_2("3:2", AspectRatio.RATIO_4_3, 2f / 3f),
    RATIO_4_3("4:3", AspectRatio.RATIO_4_3, 3f / 4f),
    RATIO_9_16("9:16", AspectRatio.RATIO_16_9, 9f / 16f)
}

/**
 * Camera flash modes.
 */
enum class VintageFlashMode(val title: String, val imageCaptureFlashMode: Int) {
    AUTO("AUTO", ImageCapture.FLASH_MODE_AUTO),
    ON("ON", ImageCapture.FLASH_MODE_ON),
    OFF("OFF", ImageCapture.FLASH_MODE_OFF)
}

/**
 * Professional analog film emulation presets backed by real-time OpenGL ES 3.0
 * optical physics shaders (luminance-dependent grain, halation bloom, and film tone curves).
 */
enum class VintageFilmPreset(
    val filterIndex: Int,
    val title: String,
    val subtitle: String,
    val isoDisplay: String,
    val badgeColor: Color
) {
    STANDARD(
        filterIndex = 0,
        title = "STANDARD",
        subtitle = "Natural Glass",
        isoDisplay = "ISO 200",
        badgeColor = Color(0xFF888888)
    ),
    PORTRA_400(
        filterIndex = 1,
        title = "PORTRA 400",
        subtitle = "Warm Golden Pastels",
        isoDisplay = "ISO 400",
        badgeColor = Color(0xFFFFB03B)
    ),
    CINESTILL_800T(
        filterIndex = 2,
        title = "CINESTILL 800T",
        subtitle = "Tungsten & Halation",
        isoDisplay = "ISO 800",
        badgeColor = Color(0xFF00E5FF)
    ),
    FUJI_VELVIA(
        filterIndex = 3,
        title = "VELVIA 50",
        subtitle = "Vivid Emerald Contrast",
        isoDisplay = "ISO 50",
        badgeColor = Color(0xFF00C853)
    ),
    ILFORD_HP5(
        filterIndex = 4,
        title = "ILFORD HP5",
        subtitle = "Silver Gelatin B&W",
        isoDisplay = "ISO 400",
        badgeColor = Color(0xFFE0E0E0)
    ),
    POINT_AND_SHOOT(
        filterIndex = 5,
        title = "RETRO 1980s",
        subtitle = "Milky Faded Film",
        isoDisplay = "ISO 800",
        badgeColor = Color(0xFFFF7043)
    )
}

/**
 * Mutable state holding the viewfinder, optical HUD, and camera controls.
 */
data class CameraUiState(
    val lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    val aspectRatio: VintageAspectRatio = VintageAspectRatio.RATIO_3_2,
    val flashMode: VintageFlashMode = VintageFlashMode.AUTO,
    val filmPreset: VintageFilmPreset = VintageFilmPreset.PORTRA_400,
    val isGridVisible: Boolean = true,
    val frameCounter: Int = 1,
    val maxFrames: Int = 36,
    val zoomRatio: Float = 1.0f,
    val minZoom: Float = 1.0f,
    val maxZoom: Float = 8.0f,
    val evValue: Float = 0.0f,
    val evIndex: Int = 0,
    val minEvIndex: Int = -6,
    val maxEvIndex: Int = 6,
    val evStep: Float = 0.3333f,
    val isEvSupported: Boolean = true,
    val showEvDial: Boolean = false,
    val isDateStampEnabled: Boolean = true,
    val isFlashActive: Boolean = false,
    val isShutterAnimating: Boolean = false,
    val lastCapturedMessage: String? = null
)
