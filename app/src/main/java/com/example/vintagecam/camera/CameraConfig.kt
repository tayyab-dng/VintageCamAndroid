package com.example.vintagecam.camera

import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix

/**
 * Supported aspect ratios for vintage framing.
 */
enum class VintageAspectRatio(val title: String, val cameraRatio: Int, val floatRatio: Float) {
    RATIO_4_3("4:3", AspectRatio.RATIO_4_3, 3f / 4f),
    RATIO_16_9("16:9", AspectRatio.RATIO_16_9, 9f / 16f)
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
 * Vintage film simulation presets applying authentic analog color science.
 */
enum class VintageFilmPreset(
    val title: String,
    val subtitle: String,
    val isoDisplay: String,
    val badgeColor: Color,
    val colorFilter: ColorFilter?
) {
    STANDARD(
        title = "STANDARD",
        subtitle = "Natural Glass",
        isoDisplay = "ISO 200",
        badgeColor = Color(0xFF888888),
        colorFilter = null
    ),
    KODAK_GOLD(
        title = "GOLD 400",
        subtitle = "Warm Nostalgia",
        isoDisplay = "ISO 400",
        badgeColor = Color(0xFFFFB300),
        colorFilter = ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    1.12f, 0.05f, 0.00f, 0f, 15f,
                    0.05f, 1.05f, 0.00f, 0f, 10f,
                    0.00f, 0.02f, 0.88f, 0f, -5f,
                    0.00f, 0.00f, 0.00f, 1f, 0f
                )
            )
        )
    ),
    FUJI_VELVIA(
        title = "VELVIA 50",
        subtitle = "Vivid Contrast",
        isoDisplay = "ISO 50",
        badgeColor = Color(0xFF00C853),
        colorFilter = ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    1.18f, -0.05f, 0.00f, 0f, 5f,
                    -0.02f, 1.20f, 0.02f, 0f, 10f,
                    0.00f, -0.05f, 1.22f, 0f, 15f,
                    0.00f, 0.00f, 0.00f, 1f, 0f
                )
            )
        )
    ),
    MONO_1974(
        title = "MONO '74",
        subtitle = "Silver Gelatin",
        isoDisplay = "ISO 800",
        badgeColor = Color(0xFFEEEEEE),
        colorFilter = ColorFilter.colorMatrix(
            ColorMatrix().apply {
                setToSaturation(0f)
            }
        )
    ),
    SEPIA_AMBER(
        title = "SEPIA '40",
        subtitle = "Antique Patina",
        isoDisplay = "ISO 100",
        badgeColor = Color(0xFFD78E4C),
        colorFilter = ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    0.393f * 1.1f, 0.769f * 1.1f, 0.189f * 1.1f, 0f, 25f,
                    0.349f * 1.0f, 0.686f * 1.0f, 0.168f * 1.0f, 0f, 15f,
                    0.272f * 0.9f, 0.534f * 0.9f, 0.131f * 0.9f, 0f, -10f,
                    0.0f,          0.0f,          0.0f,          1f, 0f
                )
            )
        )
    )
}

/**
 * Mutable state holding the viewfinder and camera controls.
 */
data class CameraUiState(
    val lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    val aspectRatio: VintageAspectRatio = VintageAspectRatio.RATIO_4_3,
    val flashMode: VintageFlashMode = VintageFlashMode.AUTO,
    val filmPreset: VintageFilmPreset = VintageFilmPreset.KODAK_GOLD,
    val isGridVisible: Boolean = true,
    val frameCounter: Int = 1,
    val maxFrames: Int = 36,
    val zoomRatio: Float = 1.0f,
    val minZoom: Float = 1.0f,
    val maxZoom: Float = 8.0f,
    val isFlashActive: Boolean = false,
    val isShutterAnimating: Boolean = false,
    val lastCapturedMessage: String? = null
)
