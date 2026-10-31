package com.example.vintagecam.camera.gl

import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Handler
import android.os.Looper
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
 * OpenGL ES 3.0 Optical Physics Shader Engine for real-time CameraX viewfinder frame processing.
 * Renders an external OES texture through custom GLSL shaders with optical halation bloom,
 * luminance-responsive silver-halide grain, lens vignetting, and authentic film stock science.
 */
class VintageGLRenderer(
    private val surfaceView: GLSurfaceView
) : GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener, Preview.SurfaceProvider {

    // Vertex shader: transforms full-screen quad UVs by camera matrix
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

    // Fragment shader: 35mm optical physics engine
    private val fragmentShaderSource = """
        #version 300 es
        #extension GL_OES_EGL_image_external_essl3 : require

        precision highp float;

        in vec2 v_TexCoord;
        out vec4 fragColor;

        uniform samplerExternalOES u_Texture;
        uniform float u_Time;
        uniform vec2 u_Resolution;
        uniform int u_FilterIndex;

        // Rec.709 Photometric Luminance
        float getLuminance(vec3 col) {
            return dot(col, vec3(0.2126, 0.7152, 0.0722));
        }

        // Fast hash for organic silver halide crystals
        float hash21(vec2 p) {
            vec3 p3 = fract(vec3(p.xyx) * vec3(443.897, 441.423, 437.195));
            p3 += dot(p3, p3.yzx + 19.19);
            return fract((p3.x + p3.y) * p3.z);
        }

        // Emulsion grain response: peaks in shadows and midtones, soft roll-off in highlights
        float grainLuminanceResponse(float lum) {
            float shadow = smoothstep(0.01, 0.22, lum);
            float highlight = 1.0 - smoothstep(0.55, 0.98, lum);
            return shadow * highlight;
        }

        // Multi-frequency procedural silver halide grain generator
        float calculateFilmGrain(vec2 uv, float time, vec2 resolution, float lum, float grainScale, float intensity) {
            if (intensity <= 0.0) return 0.0;
            // Quantize to 24 FPS film motion intervals
            float filmTime = floor(time * 24.0) / 24.0;
            vec2 coord = uv * resolution * grainScale + vec2(filmTime * 37.17, filmTime * 59.23);

            // Blend two spatial octaves for organic crystal clumping
            float g1 = hash21(coord);
            float g2 = hash21(coord * 0.45 + vec2(17.3, 31.8));
            float rawGrain = (g1 * 0.65 + g2 * 0.35 - 0.5) * 2.0;

            float response = grainLuminanceResponse(lum);
            return rawGrain * response * intensity;
        }

        // Multi-tap highlight halation scatter (signature anti-halation backing removal glow)
        // Accesses global u_Texture directly for strict ESSL 3.00 compliance across all GPUs
        vec3 sampleHalation(vec2 uv, vec2 resolution, float threshold, vec3 halationTint, float intensity) {
            if (intensity <= 0.0) return vec3(0.0);

            vec2 texel = 1.0 / resolution;
            vec3 bloomSum = vec3(0.0);

            // 8-point radial scatter kernel
            const vec2 kernel[8] = vec2[](
                vec2( 0.0,  4.2),
                vec2( 0.0, -4.2),
                vec2( 4.2,  0.0),
                vec2(-4.2,  0.0),
                vec2( 3.0,  3.0),
                vec2(-3.0,  3.0),
                vec2( 3.0, -3.0),
                vec2(-3.0, -3.0)
            );

            for (int i = 0; i < 8; i++) {
                vec2 offsetUv = uv + kernel[i] * texel * 1.6;
                vec3 col = texture(u_Texture, offsetUv).rgb;
                float lum = getLuminance(col);

                // Soft threshold extraction
                float overExposure = max(0.0, lum - threshold) / (1.0 - threshold + 0.001);
                float boost = pow(overExposure, 1.6);
                bloomSum += col * boost;
            }

            bloomSum = (bloomSum / 8.0) * halationTint * intensity;
            return bloomSum;
        }

        // Aspect-ratio proportional optical lens vignetting
        float opticalVignette(vec2 uv, float strength, float falloffStart) {
            vec2 coord = (uv - 0.5) * 2.0;
            float dist = dot(coord, coord);
            float vig = smoothstep(2.0, falloffStart, dist);
            return mix(1.0 - strength, 1.0, vig);
        }

        // Analog film characteristic S-curve (toe lift and shoulder roll-off)
        vec3 applyFilmCurve(vec3 c, float toeLift, float shoulderRoll) {
            vec3 lifted = mix(vec3(toeLift), c, smoothstep(0.0, 0.35, c));
            vec3 scurve = lifted * lifted * (3.0 - 2.0 * lifted);
            vec3 shoulder = mix(scurve, vec3(1.0 - shoulderRoll + shoulderRoll * scurve), smoothstep(0.65, 1.0, scurve));
            return clamp(shoulder, 0.0, 1.0);
        }

        // ==========================================
        // Curated Film Emulation Profiles
        // ==========================================

        // Index 0: STANDARD (Natural pass-through)
        vec3 applyStandard(vec3 raw) {
            return raw;
        }

        // Index 1: KODAK PORTRA 400 (Warm golden pastels, gentle shadows, fine grain)
        vec3 applyPortra400(vec3 raw, vec2 uv, float t, vec2 res) {
            float lum = getLuminance(raw);
            vec3 halation = sampleHalation(uv, res, 0.72, vec3(1.0, 0.75, 0.45), 0.35);

            vec3 col = raw;
            col.r = clamp(col.r * 1.10 + 0.03, 0.0, 1.0);
            col.g = clamp(col.g * 1.04 + 0.015, 0.0, 1.0);
            col.b = clamp(col.b * 0.92 - 0.01, 0.0, 1.0);

            col = applyFilmCurve(col, 0.03, 0.04);
            col += halation * (1.0 - col);
            col *= opticalVignette(uv, 0.22, 0.6);

            float grain = calculateFilmGrain(uv, t, res, lum, 0.85, 0.048);
            col += grain * (1.0 - col * 0.5);

            return clamp(col, 0.0, 1.0);
        }

        // Index 2: CINESTILL 800T (Cool cyan/tungsten color cast, contrasty night blacks, intense red-orange halation)
        vec3 applyCineStill800T(vec3 raw, vec2 uv, float t, vec2 res) {
            float lum = getLuminance(raw);
            // Vermillion red-orange halation glow
            vec3 halationTint = vec3(1.0, 0.20, 0.05);
            vec3 halation = sampleHalation(uv, res, 0.62, halationTint, 1.45);

            vec3 col = raw;
            col.r = clamp(col.r * 0.94 - 0.01, 0.0, 1.0);
            col.g = clamp(col.g * 1.05 + 0.02, 0.0, 1.0);
            col.b = clamp(col.b * 1.18 + 0.04, 0.0, 1.0);

            col = pow(col, vec3(1.15));
            col = applyFilmCurve(col, 0.015, 0.08);
            col += halation * (1.2 - col * 0.3);
            col *= opticalVignette(uv, 0.32, 0.5);

            float grain = calculateFilmGrain(uv, t, res, lum, 0.70, 0.072);
            col += grain * (1.0 - col * 0.5);

            return clamp(col, 0.0, 1.0);
        }

        // Index 3: FUJI VELVIA 50 (Punchy saturated emerald greens, rich skies, deep contrast, micro-grain)
        vec3 applyFujiVelvia(vec3 raw, vec2 uv, float t, vec2 res) {
            float lum = getLuminance(raw);
            vec3 col = raw;
            col.r = clamp(pow(col.r, 1.12) * 1.08, 0.0, 1.0);
            col.g = clamp(pow(col.g, 1.02) * 1.22 + 0.01, 0.0, 1.0);
            col.b = clamp(pow(col.b, 1.05) * 1.15, 0.0, 1.0);

            col = applyFilmCurve(col, 0.01, 0.02);
            col *= opticalVignette(uv, 0.28, 0.55);

            float grain = calculateFilmGrain(uv, t, res, lum, 1.2, 0.028);
            col += grain;

            return clamp(col, 0.0, 1.0);
        }

        // Index 4: ILFORD HP5 400 (Timeless silver gelatin black-and-white, silver-halide organic grain)
        vec3 applyIlfordHP5(vec3 raw, vec2 uv, float t, vec2 res) {
            float lum = getLuminance(raw);
            lum = clamp((lum - 0.44) * 1.38 + 0.44, 0.0, 1.0);
            lum = lum * lum * (3.0 - 2.0 * lum);
            lum *= opticalVignette(uv, 0.38, 0.45);

            float grain = calculateFilmGrain(uv, t, res, lum, 0.75, 0.095);
            lum += grain * (0.85 - abs(lum - 0.45));

            return vec3(clamp(lum, 0.0, 1.0));
        }

        // Index 5: RETRO 1980s POINT-AND-SHOOT (Milky faded blacks, warm color cast, aggressive corner vignette)
        vec3 applyPointAndShoot(vec3 raw, vec2 uv, float t, vec2 res) {
            float lum = getLuminance(raw);
            vec3 flashBloom = sampleHalation(uv, res, 0.75, vec3(1.0, 0.85, 0.50), 0.45);

            vec3 col = raw;
            col = mix(vec3(0.08, 0.06, 0.05), col, 0.92);
            col.r = clamp(col.r * 1.12 + 0.05, 0.0, 1.0);
            col.g = clamp(col.g * 1.04 + 0.03, 0.0, 1.0);
            col.b = clamp(col.b * 0.88, 0.0, 1.0);

            col += flashBloom * 0.5;
            col *= opticalVignette(uv, 0.50, 0.35);

            float grain = calculateFilmGrain(uv, t, res, lum, 0.65, 0.08);
            col += grain * (1.0 - col * 0.4);

            return clamp(col, 0.0, 1.0);
        }

        void main() {
            vec4 raw = texture(u_Texture, v_TexCoord);
            vec3 result;

            switch (u_FilterIndex) {
                case 1:
                    result = applyPortra400(raw.rgb, v_TexCoord, u_Time, u_Resolution);
                    break;
                case 2:
                    result = applyCineStill800T(raw.rgb, v_TexCoord, u_Time, u_Resolution);
                    break;
                case 3:
                    result = applyFujiVelvia(raw.rgb, v_TexCoord, u_Time, u_Resolution);
                    break;
                case 4:
                    result = applyIlfordHP5(raw.rgb, v_TexCoord, u_Time, u_Resolution);
                    break;
                case 5:
                    result = applyPointAndShoot(raw.rgb, v_TexCoord, u_Time, u_Resolution);
                    break;
                default:
                    result = applyStandard(raw.rgb);
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
    private var uTextureLoc = -1
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

    private val filterIndex = AtomicInteger(1) // Default to Kodak Portra 400 (1)
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
        st.setOnFrameAvailableListener(this, Handler(Looper.getMainLooper()))
        surfaceTexture = st

        // 3. Compile Shaders and link Program
        programId = createProgram(vertexShaderSource, fragmentShaderSource)
        if (programId != 0) {
            uTexMatrixLoc = GLES30.glGetUniformLocation(programId, "u_TexMatrix")
            uTextureLoc = GLES30.glGetUniformLocation(programId, "u_Texture")
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

        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 16, 0)

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

        computeFinalTransformMatrix()

        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)

        if (programId == 0) return

        GLES30.glUseProgram(programId)

        // Bind OES Texture to Unit 0
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId)
        if (uTextureLoc != -1) {
            GLES30.glUniform1i(uTextureLoc, 0)
        }

        // Pass Uniforms
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

    override fun onSurfaceRequested(request: SurfaceRequest) {
        val st = surfaceTexture
        if (st != null) {
            provideSurfaceToRequest(request, st)
        } else {
            pendingSurfaceRequest = request
        }
    }

    fun setCameraTransformation(rotationDegrees: Int, isMirroring: Boolean) {
        cameraRotationDegrees = rotationDegrees
        cameraIsMirroring = isMirroring
        Log.d(TAG, "setCameraTransformation: rot=$rotationDegrees, mirror=$isMirroring")
        surfaceView.requestRender()
    }

    private fun provideSurfaceToRequest(request: SurfaceRequest, st: SurfaceTexture) {
        cameraWidth = request.resolution.width
        cameraHeight = request.resolution.height
        st.setDefaultBufferSize(cameraWidth, cameraHeight)
        Log.d(TAG, "CameraX Surface provided: ${cameraWidth}x${cameraHeight}")

        try {
            request.setTransformationInfoListener(ContextCompat.getMainExecutor(surfaceView.context)) { info ->
                setCameraTransformation(info.rotationDegrees, info.isMirroring)
            }
        } catch (e: Exception) {
            Log.d(TAG, "TransformationInfoListener already handled by caller: ${e.message}")
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

    private fun computeFinalTransformMatrix() {
        Matrix.setIdentityM(customMatrix, 0)

        // 1. Shift to center (0.5, 0.5)
        Matrix.translateM(customMatrix, 0, 0.5f, 0.5f, 0.0f)

        // 2. Aspect Ratio Scaling (FILL_CENTER to avoid stretching) in DISPLAY coordinate space
        val isRotated = cameraRotationDegrees == 90 || cameraRotationDegrees == 270
        val effectiveCamWidth = if (isRotated) cameraHeight else cameraWidth
        val effectiveCamHeight = if (isRotated) cameraWidth else cameraHeight

        val camAspect = effectiveCamWidth.toFloat() / effectiveCamHeight.toFloat().coerceAtLeast(1f)
        val viewAspect = viewportWidth.toFloat() / viewportHeight.toFloat().coerceAtLeast(1f)

        var scaleX = 1.0f
        var scaleY = 1.0f

        if (viewAspect > camAspect) {
            scaleY = camAspect / viewAspect
        } else if (viewAspect < camAspect) {
            scaleX = viewAspect / camAspect
        }

        Matrix.scaleM(customMatrix, 0, scaleX, scaleY, 1.0f)

        // 3. Mirroring for front camera
        if (cameraIsMirroring) {
            Matrix.scaleM(customMatrix, 0, -1.0f, 1.0f, 1.0f)
        }

        // 4. Shift back from origin to (0.5, 0.5)
        Matrix.translateM(customMatrix, 0, -0.5f, -0.5f, 0.0f)

        // 5. SurfaceTexture.getTransformMatrix already incorporates the hardware buffer transform
        // aligning the camera stream with the portrait display. Multiply raw * custom to apply
        // aspect ratio crop and front-camera mirroring onto quad texture coordinates.
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
