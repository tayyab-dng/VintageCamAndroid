package com.example.vintagecam

import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import com.example.vintagecam.camera.CameraUiState
import com.example.vintagecam.camera.VintageAspectRatio
import com.example.vintagecam.camera.VintageFilmPreset
import com.example.vintagecam.camera.VintageFlashMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VintageCameraConfigTest {

    @Test
    fun defaultCameraUiState_hasSensibleDefaults() {
        val state = CameraUiState()
        assertEquals(CameraSelector.LENS_FACING_BACK, state.lensFacing)
        assertEquals(VintageAspectRatio.RATIO_3_2, state.aspectRatio)
        assertEquals(VintageFlashMode.AUTO, state.flashMode)
        assertEquals(VintageFilmPreset.PORTRA_400, state.filmPreset)
        assertTrue(state.isGridVisible)
        assertEquals(1, state.frameCounter)
        assertEquals(36, state.maxFrames)
        assertEquals(0.0f, state.evValue, 0.001f)
        assertEquals(0, state.evIndex)
        assertTrue(state.isDateStampEnabled)
        org.junit.Assert.assertFalse(state.isCapturing)
        org.junit.Assert.assertNull(state.lastCapturedUri)
        org.junit.Assert.assertNull(state.lastThumbnail)
    }

    @Test
    fun vintageAspectRatios_mapCorrectlyToCameraX() {
        assertEquals(AspectRatio.RATIO_4_3, VintageAspectRatio.RATIO_3_2.cameraRatio)
        assertEquals(AspectRatio.RATIO_4_3, VintageAspectRatio.RATIO_4_3.cameraRatio)
        assertEquals(AspectRatio.RATIO_16_9, VintageAspectRatio.RATIO_9_16.cameraRatio)
        assertEquals(2f / 3f, VintageAspectRatio.RATIO_3_2.floatRatio, 0.001f)
        assertEquals(3f / 4f, VintageAspectRatio.RATIO_4_3.floatRatio, 0.001f)
        assertEquals(9f / 16f, VintageAspectRatio.RATIO_9_16.floatRatio, 0.001f)
    }

    @Test
    fun vintageFlashModes_mapCorrectlyToImageCapture() {
        assertEquals(ImageCapture.FLASH_MODE_AUTO, VintageFlashMode.AUTO.imageCaptureFlashMode)
        assertEquals(ImageCapture.FLASH_MODE_ON, VintageFlashMode.ON.imageCaptureFlashMode)
        assertEquals(ImageCapture.FLASH_MODE_OFF, VintageFlashMode.OFF.imageCaptureFlashMode)
    }

    @Test
    fun vintageFilmPresets_haveConfiguredBadgesAndTitles() {
        assertEquals(6, VintageFilmPreset.values().size)

        VintageFilmPreset.values().forEach { preset ->
            assertNotNull(preset.title)
            assertNotNull(preset.subtitle)
            assertNotNull(preset.isoDisplay)
            assertNotNull(preset.badgeColor)
            assertTrue(preset.filterIndex in 0..5)
        }

        // Verify filterIndex maps correctly to GLSL fragment shader switch cases
        assertEquals(0, VintageFilmPreset.STANDARD.filterIndex)
        assertEquals(1, VintageFilmPreset.PORTRA_400.filterIndex)
        assertEquals(2, VintageFilmPreset.CINESTILL_800T.filterIndex)
        assertEquals(3, VintageFilmPreset.FUJI_VELVIA.filterIndex)
        assertEquals(4, VintageFilmPreset.ILFORD_HP5.filterIndex)
        assertEquals(5, VintageFilmPreset.POINT_AND_SHOOT.filterIndex)
    }
}
