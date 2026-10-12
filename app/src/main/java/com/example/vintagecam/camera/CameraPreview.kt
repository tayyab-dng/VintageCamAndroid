package com.example.vintagecam.camera

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.vintagecam.camera.gl.VintageGLSurfaceView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

private const val TAG = "VintageCameraPreview"

/**
 * Controller to trigger camera actions from parent composables.
 */
class VintageCameraController internal constructor() {
    internal var imageCapture: ImageCapture? = null
    internal var cameraControl: CameraControl? = null
    internal var onCaptureStarted: (() -> Unit)? = null
    internal var onCaptureSuccess: ((File) -> Unit)? = null
    internal var onCaptureError: ((Exception) -> Unit)? = null

    fun takePhoto(context: Context, outputDirectory: File) {
        val capture = imageCapture ?: run {
            Log.e(TAG, "ImageCapture not initialized yet")
            return
        }

        onCaptureStarted?.invoke()

        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs()
        }

        val photoFile = File(
            outputDirectory,
            "VINTAGE_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())}.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    Log.d(TAG, "Photo saved successfully: ${photoFile.absolutePath}")
                    onCaptureSuccess?.invoke(photoFile)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                    onCaptureError?.invoke(exception)
                }
            }
        )
    }

    fun setZoomRatio(ratio: Float) {
        cameraControl?.setZoomRatio(ratio)
    }
}

@Composable
fun rememberVintageCameraController(): VintageCameraController {
    return remember { VintageCameraController() }
}

/**
 * Lifecycle-aware CameraPreview composable backed by an OpenGL ES 3.0 SurfaceView
 * (VintageGLSurfaceView) with real-time per-frame GLSL shader processing.
 */
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    aspectRatio: VintageAspectRatio = VintageAspectRatio.RATIO_4_3,
    flashMode: VintageFlashMode = VintageFlashMode.AUTO,
    filmPreset: VintageFilmPreset = VintageFilmPreset.KODAK_GOLD,
    controller: VintageCameraController = rememberVintageCameraController(),
    onZoomChanged: (Float, Float, Float) -> Unit = { _, _, _ -> },
    onPhotoCaptured: (File) -> Unit = {},
    onCaptureStarted: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var activeCamera by remember { mutableStateOf<Camera?>(null) }
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var isFocusing by remember { mutableStateOf(false) }

    var currentZoom by remember { mutableFloatStateOf(1.0f) }
    var minZoom by remember { mutableFloatStateOf(1.0f) }
    var maxZoom by remember { mutableFloatStateOf(8.0f) }

    // Pre-create the VintageGLSurfaceView
    val glSurfaceView = remember {
        VintageGLSurfaceView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    // Manage GLSurfaceView pause/resume with Compose lifecycle
    DisposableEffect(glSurfaceView) {
        glSurfaceView.onResume()
        onDispose {
            glSurfaceView.onPause()
        }
    }

    // Dynamically update the GLSL shader filter index when the film preset changes
    LaunchedEffect(filmPreset) {
        glSurfaceView.setFilterIndex(filmPreset.filterIndex)
    }

    // Connect controller callbacks
    LaunchedEffect(controller, onPhotoCaptured, onCaptureStarted) {
        controller.onCaptureStarted = onCaptureStarted
        controller.onCaptureSuccess = onPhotoCaptured
        controller.onCaptureError = { ex ->
            Log.e(TAG, "Capture error in preview controller: ${ex.message}")
        }
    }

    // Initialize CameraProvider
    LaunchedEffect(Unit) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener(
            {
                try {
                    cameraProvider = providerFuture.get()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to get ProcessCameraProvider", e)
                }
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    // Rebind CameraX whenever lifecycle, lensFacing, aspectRatio, or flashMode change
    LaunchedEffect(cameraProvider, lifecycleOwner, lensFacing, aspectRatio, flashMode) {
        val provider = cameraProvider ?: return@LaunchedEffect

        try {
            provider.unbindAll()

            val resolutionSelector = ResolutionSelector.Builder()
                .setAspectRatioStrategy(
                    AspectRatioStrategy(
                        aspectRatio.cameraRatio,
                        AspectRatioStrategy.FALLBACK_RULE_AUTO
                    )
                )
                .build()

            val preview = Preview.Builder()
                .setResolutionSelector(resolutionSelector)
                .build()

            val imageCapture = ImageCapture.Builder()
                .setResolutionSelector(resolutionSelector)
                .setFlashMode(flashMode.imageCaptureFlashMode)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            // Verify if camera selector is available
            if (!provider.hasCamera(cameraSelector)) {
                Log.w(TAG, "Selected camera facing $lensFacing not found on this device")
                return@LaunchedEffect
            }

            // Bind CameraX Preview to our OpenGL ES 3.0 Surface Provider!
            preview.setSurfaceProvider(glSurfaceView.surfaceProvider)

            val camera = provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )

            activeCamera = camera
            controller.imageCapture = imageCapture
            controller.cameraControl = camera.cameraControl

            // Listen to zoom state
            camera.cameraInfo.zoomState.observe(lifecycleOwner) { zoomState ->
                zoomState?.let {
                    currentZoom = it.zoomRatio
                    minZoom = it.minZoomRatio
                    maxZoom = it.maxZoomRatio
                    onZoomChanged(it.zoomRatio, it.minZoomRatio, it.maxZoomRatio)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Camera binding failure", e)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraProvider?.unbindAll()
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(activeCamera) {
                detectTapGestures { offset ->
                    val camera = activeCamera ?: return@detectTapGestures
                    val widthPx = size.width.toFloat()
                    val heightPx = size.height.toFloat()
                    if (widthPx <= 0f || heightPx <= 0f) return@detectTapGestures

                    val factory = SurfaceOrientedMeteringPointFactory(widthPx, heightPx)
                    val point = factory.createPoint(offset.x, offset.y)
                    val action = FocusMeteringAction.Builder(
                        point,
                        FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
                    )
                        .setAutoCancelDuration(3, TimeUnit.SECONDS)
                        .build()

                    camera.cameraControl.startFocusAndMetering(action)

                    focusPoint = offset
                    isFocusing = true
                    coroutineScope.launch {
                        delay(2500)
                        isFocusing = false
                    }
                }
            }
            .pointerInput(activeCamera, minZoom, maxZoom) {
                detectTransformGestures { _, _, zoomChange, _ ->
                    val camera = activeCamera ?: return@detectTransformGestures
                    if (zoomChange != 1.0f) {
                        val newZoom = (currentZoom * zoomChange).coerceIn(minZoom, maxZoom)
                        camera.cameraControl.setZoomRatio(newZoom)
                    }
                }
            }
    ) {
        // OpenGL ES 3.0 Surface View
        AndroidView(
            factory = { glSurfaceView },
            modifier = Modifier.fillMaxSize()
        )

        // Tap-to-focus vintage target ring
        focusPoint?.let { point ->
            AnimatedVisibility(
                visible = isFocusing,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.offset {
                    IntOffset((point.x - 32.dp.toPx()).roundToInt(), (point.y - 32.dp.toPx()).roundToInt())
                }
            ) {
                Canvas(modifier = Modifier.size(64.dp)) {
                    // Vintage square focus bracket with crosshairs
                    drawRect(
                        color = Color(0xFFFFB03B),
                        style = Stroke(width = 2.dp.toPx())
                    )
                    // Top crosshair
                    drawLine(
                        color = Color(0xFFFFB03B),
                        start = Offset(size.width / 2f, 0f),
                        end = Offset(size.width / 2f, 6.dp.toPx()),
                        strokeWidth = 2.dp.toPx()
                    )
                    // Bottom crosshair
                    drawLine(
                        color = Color(0xFFFFB03B),
                        start = Offset(size.width / 2f, size.height),
                        end = Offset(size.width / 2f, size.height - 6.dp.toPx()),
                        strokeWidth = 2.dp.toPx()
                    )
                    // Left crosshair
                    drawLine(
                        color = Color(0xFFFFB03B),
                        start = Offset(0f, size.height / 2f),
                        end = Offset(6.dp.toPx(), size.height / 2f),
                        strokeWidth = 2.dp.toPx()
                    )
                    // Right crosshair
                    drawLine(
                        color = Color(0xFFFFB03B),
                        start = Offset(size.width, size.height / 2f),
                        end = Offset(size.width - 6.dp.toPx(), size.height / 2f),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
        }
    }
}
