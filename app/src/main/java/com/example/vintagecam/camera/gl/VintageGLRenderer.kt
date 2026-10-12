package com.example.vintagecam.camera.gl

import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.SystemClock
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.core.content.ContextCompat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

private const val TAG = "VintageGLRenderer"

/**
 * OpenGL ES 3.0 Renderer for real-time CameraX viewfinder frame processing.
 * Renders an external OES texture through custom GLSL shaders with zero latency.
 */
class VintageGLRenderer(
    private val surfaceView: GLSurfaceView
) : GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener, Preview.SurfaceProvider {

    // Shader source code
    private val vertexShaderSource = """
        #version 300 es
        layout(location = 0) in vec2 a_Position;
        layout(location = 1) in vec2 a_TexCoord;

        uniform mat4 u_TexMatrix;
        out vec2 v_TexCoord;

        void main() {
            gl_Position = vec4(a_Position, 0.0, 1.0);
            v_TexCoord = (u_TexMatrix * vec4(a_TexCoord, 0.0, 1.0)).xy;
        }
    """.trimIndent()

    private val fragmentShaderSource = """
        #version 300 es
        #extension GL_OES_EGL_image_external_essl3 : enable
        #extension GL_OES_EGL_image_external : enable

        precision highp float;

        in vec2 v_TexCoord;
        out vec4 fragColor;

        uniform samplerExternalOES u_Texture;
        uniform float u_Time;
        uniform vec2 u_Resolution;
        uniform int u_FilterIndex;

        // Fast hash for procedural analog grain
        float hash(vec2 p) {
            p = fract(p * vec2(123.34, 456.21));
            p += dot(p, p + 45.32);
            return fract(p.x * p.y);
        }

        // Film grain simulation
        float filmGrain(vec2 uv, float t) {
            float seed = hash(uv * 1200.0 + fract(t * 18.0));
            return (seed - 0.5) * 2.0;
        }

        // Optical lens vignette
        float vignette(vec2 uv) {
            vec2 coord = (uv - 0.5) * 2.0;
            return clamp(1.0 - dot(coord, coord) * 0.40, 0.0, 1.0);
        }

        // Kodak Gold 400 emulation
        vec3 applyKodakGold(vec3 col, vec2 uv, float t) {
            // Analog warm contrast curve
            col = clamp((col - 0.5) * 1.15 + 0.5, 0.0, 1.0);
            col.r = clamp(col.r * 1.14 + 0.04, 0.0, 1.0);
            col.g = clamp(col.g * 1.06 + 0.02, 0.0, 1.0);
            col.b = clamp(col.b * 0.88 - 0.02, 0.0, 1.0);
            col *= vignette(uv);
            float grain = filmGrain(uv, t) * 0.055;
            col += grain * (1.0 - col);
            return clamp(col, 0.0, 1.0);
        }

        // Fuji Velvia 50 emulation
        vec3 applyFujiVelvia(vec3 col, vec2 uv, float t) {
            col = clamp((col - 0.5) * 1.25 + 0.5, 0.0, 1.0);
            col.r = clamp(col.r * 1.12, 0.0, 1.0);
            col.g = clamp(col.g * 1.20 + 0.02, 0.0, 1.0);
            col.b = clamp(col.b * 1.12, 0.0, 1.0);
            col *= mix(1.0, vignette(uv), 0.65);
            float grain = filmGrain(uv, t) * 0.035;
            col += grain;
            return clamp(col, 0.0, 1.0);
        }

        // Mono 1974 silver gelatin emulation
        vec3 applyMono1974(vec3 col, vec2 uv, float t) {
            float lum = dot(col, vec3(0.2126, 0.7152, 0.0722));
            lum = clamp((lum - 0.45) * 1.35 + 0.45, 0.0, 1.0);
            float grain = filmGrain(uv, t) * 0.085;
            lum += grain * (0.8 - abs(lum - 0.5));
            lum *= vignette(uv);
            return vec3(clamp(lum, 0.0, 1.0));
        }

        // Sepia 1940 antique patina emulation
        vec3 applySepiaAmber(vec3 col, vec2 uv, float t) {
            float lum = dot(col, vec3(0.299, 0.587, 0.114));
            vec3 sepia = vec3(
                lum * 1.22 + 0.10,
                lum * 1.02 + 0.05,
                lum * 0.72
            );
            sepia *= clamp(vignette(uv) * 0.92 + 0.08, 0.0, 1.0);
            float grain = filmGrain(uv, t) * 0.065;
            sepia += grain;
            return clamp(sepia, 0.0, 1.0);
        }

        void main() {
            vec4 raw = texture(u_Texture, v_TexCoord);
            vec3 result;

            switch (u_FilterIndex) {
                case 1:
                    result = applyKodakGold(raw.rgb, v_TexCoord, u_Time);
                    break;
                case 2:
                    result = applyFujiVelvia(raw.rgb, v_TexCoord, u_Time);
                    break;
                case 3:
                    result = applyMono1974(raw.rgb, v_TexCoord, u_Time);
                    break;
                case 4:
                    result = applySepiaAmber(raw.rgb, v_TexCoord, u_Time);
                    break;
                default:
                    result = raw.rgb;
                    break;
            }

            fragColor = vec4(result, raw.a);
        }
    """.trimIndent()

    // Full-screen Quad (NDC Coordinates + Texture Coordinates)
    // 4 vertices: (x, y, u, v)
    private val quadData = floatArrayOf(
        -1.0f, -1.0f, 0.0f, 0.0f,
         1.0f, -1.0f, 1.0f, 0.0f,
        -1.0f,  1.0f, 0.0f, 1.0f,
         1.0f,  1.0f, 1.0f, 1.0f
    )

    private val quadBuffer: FloatBuffer = ByteBuffer.allocateDirect(quadData.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(quadData)
            position(0)
        }

    // OpenGL State
    private var programId = 0
    private var vaoId = 0
    private var vboId = 0
    private var oesTextureId = 0

    // Uniform locations
    private var uTexMatrixLoc = -1
    private var uTimeLoc = -1
    private var uResolutionLoc = -1
    private var uFilterIndexLoc = -1

    // Camera SurfaceTexture state
    private var surfaceTexture: SurfaceTexture? = null
    private var pendingSurfaceRequest: SurfaceRequest? = null
    private var activeSurface: Surface? = null
    private val isNewFrameAvailable = AtomicBoolean(false)

    // Transformation & Viewport state
    private val rawTexMatrix = FloatArray(16)
    private val finalTexMatrix = FloatArray(16)
    private val customMatrix = FloatArray(16)

    private var viewportWidth = 1
    private var viewportHeight = 1
    private var cameraWidth = 1920
    private var cameraHeight = 1080
    private var cameraRotationDegrees = 0
    private var cameraIsMirroring = false

    private val filterIndex = AtomicInteger(1) // Default to Kodak Gold (1)
    private val startTimeMs = SystemClock.elapsedRealtime()

    init {
        Matrix.setIdentityM(rawTexMatrix, 0)
        Matrix.setIdentityM(finalTexMatrix, 0)
    }

    fun setFilterIndex(index: Int) {
        if (filterIndex.getAndSet(index) != index) {
            surfaceView.requestRender()
        }
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.d(TAG, "onSurfaceCreated on GL thread")

        // Black background for analog viewfinder border
        GLES30.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        // 1. Generate OES external texture
        val textures = IntArray(1)
        GLES30.glGenTextures(1, textures, 0)
        oesTextureId = textures[0]

        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId)
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)

        // 2. Create SurfaceTexture
        val st = SurfaceTexture(oesTextureId)
        st.setOnFrameAvailableListener(this)
        surfaceTexture = st

        // 3. Compile Shaders and build Program
        programId = createProgram(vertexShaderSource, fragmentShaderSource)
        if (programId != 0) {
            uTexMatrixLoc = GLES30.glGetUniformLocation(programId, "u_TexMatrix")
            uTimeLoc = GLES30.glGetUniformLocation(programId, "u_Time")
            uResolutionLoc = GLES30.glGetUniformLocation(programId, "u_Resolution")
            uFilterIndexLoc = GLES30.glGetUniformLocation(programId, "u_FilterIndex")
        }

        // 4. Set up VAO / VBO
        val vao = IntArray(1)
        val vbo = IntArray(1)
        GLES30.glGenVertexArrays(1, vao, 0)
        GLES30.glGenBuffers(1, vbo, 0)
        vaoId = vao[0]
        vboId = vbo[0]

        GLES30.glBindVertexArray(vaoId)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, quadBuffer.capacity() * 4, quadBuffer, GLES30.GL_STATIC_DRAW)

        // Stride: 4 floats * 4 bytes = 16 bytes
        // a_Position (x, y)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 16, 0)

        // a_TexCoord (u, v)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 16, 8)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
        GLES30.glBindVertexArray(0)

        // 5. Connect any pending CameraX surface request
        pendingSurfaceRequest?.let { request ->
            provideSurfaceToRequest(request, st)
            pendingSurfaceRequest = null
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewportWidth = width
        viewportHeight = height
        GLES30.glViewport(0, 0, width, height)
        Log.d(TAG, "onSurfaceChanged: viewport=${width}x${height}")
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        isNewFrameAvailable.set(true)
        // Request frame rendering (RENDERMODE_WHEN_DIRTY)
        surfaceView.requestRender()
    }

    override fun onDrawFrame(gl: GL10?) {
        val st = surfaceTexture ?: return

        if (isNewFrameAvailable.getAndSet(false)) {
            try {
                st.updateTexImage()
                st.getTransformMatrix(rawTexMatrix)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating TexImage", e)
            }
        }

        // Calculate combined orientation and aspect-ratio transform matrix
        computeFinalTransformMatrix()

        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)

        if (programId == 0) return

        GLES30.glUseProgram(programId)

        // Bind OES Texture to Unit 0
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId)

        // Update Uniforms
        GLES30.glUniformMatrix4fv(uTexMatrixLoc, 1, false, finalTexMatrix, 0)

        val elapsedSeconds = (SystemClock.elapsedRealtime() - startTimeMs) / 1000.0f
        GLES30.glUniform1f(uTimeLoc, elapsedSeconds)
        GLES30.glUniform2f(uResolutionLoc, viewportWidth.toFloat(), viewportHeight.toFloat())
        GLES30.glUniform1i(uFilterIndexLoc, filterIndex.get())

        // Render Quad
        GLES30.glBindVertexArray(vaoId)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glBindVertexArray(0)
    }

    /**
     * CameraX Preview.SurfaceProvider callback.
     */
    override fun onSurfaceRequested(request: SurfaceRequest) {
        val st = surfaceTexture
        if (st != null) {
            provideSurfaceToRequest(request, st)
        } else {
            // Buffer the request until onSurfaceCreated executes on GL thread
            pendingSurfaceRequest = request
        }
    }

    private fun provideSurfaceToRequest(request: SurfaceRequest, st: SurfaceTexture) {
        cameraWidth = request.resolution.width
        cameraHeight = request.resolution.height
        st.setDefaultBufferSize(cameraWidth, cameraHeight)
        Log.d(TAG, "CameraX Surface provided: ${cameraWidth}x${cameraHeight}")

        request.setTransformationInfoListener(ContextCompat.getMainExecutor(surfaceView.context)) { info ->
            cameraRotationDegrees = info.rotationDegrees
            cameraIsMirroring = info.isMirroring
            Log.d(TAG, "TransformationInfo updated: rot=${cameraRotationDegrees}, mirror=${cameraIsMirroring}")
            surfaceView.requestRender()
        }

        val surface = Surface(st)
        activeSurface?.release()
        activeSurface = surface

        request.provideSurface(surface, ContextCompat.getMainExecutor(surfaceView.context)) { result ->
            surface.release()
            if (activeSurface == surface) {
                activeSurface = null
            }
        }
    }

    /**
     * Computes the orientation compensation and fill-center aspect ratio scaling matrix.
     */
    private fun computeFinalTransformMatrix() {
        Matrix.setIdentityM(customMatrix, 0)

        // 1. Center coordinates around (0.5, 0.5)
        Matrix.translateM(customMatrix, 0, 0.5f, 0.5f, 0.0f)

        // 2. Rotate to compensate for camera sensor angle (portrait is 90° or 270°)
        val counterRotation = (360 - cameraRotationDegrees) % 360
        Matrix.rotateM(customMatrix, 0, counterRotation.toFloat(), 0.0f, 0.0f, 1.0f)

        // 3. Mirroring for front camera
        if (cameraIsMirroring) {
            Matrix.scaleM(customMatrix, 0, -1.0f, 1.0f, 1.0f)
        }

        // 4. Aspect Ratio Scaling (FILL_CENTER to avoid stretching)
        val isRotated = cameraRotationDegrees == 90 || cameraRotationDegrees == 270
        val effectiveCamWidth = if (isRotated) cameraHeight else cameraWidth
        val effectiveCamHeight = if (isRotated) cameraWidth else cameraHeight

        val camAspect = effectiveCamWidth.toFloat() / effectiveCamHeight.toFloat().coerceAtLeast(1f)
        val viewAspect = viewportWidth.toFloat() / viewportHeight.toFloat().coerceAtLeast(1f)

        var scaleX = 1.0f
        var scaleY = 1.0f

        if (viewAspect > camAspect) {
            // Viewport is wider than camera: crop top/bottom
            scaleY = camAspect / viewAspect
        } else if (viewAspect < camAspect) {
            // Viewport is taller than camera: crop left/right
            scaleX = viewAspect / camAspect
        }

        Matrix.scaleM(customMatrix, 0, scaleX, scaleY, 1.0f)

        // 5. Shift back from origin to (0.5, 0.5)
        Matrix.translateM(customMatrix, 0, -0.5f, -0.5f, 0.0f)

        // Multiply with SurfaceTexture's driver matrix: final = raw * custom
        Matrix.multiplyMM(finalTexMatrix, 0, rawTexMatrix, 0, customMatrix, 0)
    }

    private fun createProgram(vertexSrc: String, fragmentSrc: String): Int {
        val vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, vertexSrc)
        if (vertexShader == 0) return 0

        val fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentSrc)
        if (fragmentShader == 0) {
            GLES30.glDeleteShader(vertexShader)
            return 0
        }

        val program = GLES30.glCreateProgram()
        if (program == 0) return 0

        GLES30.glAttachShader(program, vertexShader)
        GLES30.glAttachShader(program, fragmentShader)
        GLES30.glLinkProgram(program)

        val linkStatus = IntArray(1)
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] != GLES30.GL_TRUE) {
            Log.e(TAG, "GL Program Link Error: ${GLES30.glGetProgramInfoLog(program)}")
            GLES30.glDeleteProgram(program)
            return 0
        }

        GLES30.glDeleteShader(vertexShader)
        GLES30.glDeleteShader(fragmentShader)
        return program
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES30.glCreateShader(type)
        if (shader == 0) return 0

        GLES30.glShaderSource(shader, shaderCode)
        GLES30.glCompileShader(shader)

        val compiled = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            Log.e(TAG, "Shader Compile Error (${type}): ${GLES30.glGetShaderInfoLog(shader)}")
            GLES30.glDeleteShader(shader)
            return 0
        }
        return shader
    }
}
