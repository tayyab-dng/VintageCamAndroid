package com.example.vintagecam.camera.gl

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import androidx.camera.core.Preview

/**
 * Dedicated OpenGL ES 3.0 SurfaceView for vintage viewfinder rendering.
 * Configured with EGL Context 3.0 and RENDERMODE_WHEN_DIRTY for smooth 30-60 FPS
 * processing with optimal battery and thermal efficiency.
 */
class VintageGLSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {

    private val renderer: VintageGLRenderer

    val surfaceProvider: Preview.SurfaceProvider
        get() = renderer

    init {
        // Configure OpenGL ES 3.0
        setEGLContextClientVersion(3)

        // Preserve EGL context across pauses to avoid recreating textures
        preserveEGLContextOnPause = true

        renderer = VintageGLRenderer(this)
        setRenderer(renderer)

        // Render only when dirty (on-demand when camera produces a frame or uniforms change)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    /**
     * Updates the active GLSL fragment shader film simulation filter index.
     * 0: Standard, 1: Kodak Gold, 2: Fuji Velvia, 3: Mono 1974, 4: Sepia 1940
     */
    fun setFilterIndex(index: Int) {
        renderer.setFilterIndex(index)
    }
}
