package com.example.vintagecam.camera.gl

import android.graphics.Bitmap
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES30
import android.opengl.GLUtils
import android.opengl.Matrix
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

private const val TAG = "OffscreenFilmRenderer"

/**
 * Headless OpenGL ES 3.0 Framebuffer Object (FBO) Renderer.
 * Applies the authentic 35mm optical physics film simulation shaders (luminance grain,
 * halation bloom, and film tone curves) to full-sensor still captures offscreen.
 */
class OffscreenFilmRenderer {

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
        precision highp float;

        in vec2 v_TexCoord;
        out vec4 fragColor;

        uniform sampler2D u_Texture;
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
            float filmTime = floor(time * 24.0) / 24.0;
            vec2 coord = uv * resolution * grainScale + vec2(filmTime * 37.17, filmTime * 59.23);

            float g1 = hash21(coord);
            float g2 = hash21(coord * 0.45 + vec2(17.3, 31.8));
            float rawGrain = (g1 * 0.65 + g2 * 0.35 - 0.5) * 2.0;

            float response = grainLuminanceResponse(lum);
            return rawGrain * response * intensity;
        }

        // Multi-tap highlight halation scatter
        vec3 sampleHalation(vec2 uv, vec2 resolution, float threshold, vec3 halationTint, float intensity) {
            if (intensity <= 0.0) return vec3(0.0);

            vec2 texel = 1.0 / resolution;
            vec3 bloomSum = vec3(0.0);

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

                float overExposure = max(0.0, lum - threshold) / (1.0 - threshold + 0.001);
                float boost = pow(overExposure, 1.6);
                bloomSum += col * boost;
            }

            bloomSum = (bloomSum / 8.0) * halationTint * intensity;
            return bloomSum;
        }

        // Optical lens vignetting
        float opticalVignette(vec2 uv, float strength, float falloffStart) {
            vec2 coord = (uv - 0.5) * 2.0;
            float dist = dot(coord, coord);
            float vig = smoothstep(2.0, falloffStart, dist);
            return mix(1.0 - strength, 1.0, vig);
        }

        // Analog film characteristic S-curve
        vec3 applyFilmCurve(vec3 c, float toeLift, float shoulderRoll) {
            vec3 lifted = mix(vec3(toeLift), c, smoothstep(0.0, 0.35, c));
            vec3 scurve = lifted * lifted * (3.0 - 2.0 * lifted);
            vec3 shoulder = mix(scurve, vec3(1.0 - shoulderRoll + shoulderRoll * scurve), smoothstep(0.65, 1.0, scurve));
            return clamp(shoulder, 0.0, 1.0);
        }

        vec3 applyStandard(vec3 raw) {
            return raw;
        }

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

        vec3 applyCineStill800T(vec3 raw, vec2 uv, float t, vec2 res) {
            float lum = getLuminance(raw);
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

        vec3 applyIlfordHP5(vec3 raw, vec2 uv, float t, vec2 res) {
            float lum = getLuminance(raw);
            lum = clamp((lum - 0.44) * 1.38 + 0.44, 0.0, 1.0);
            lum = lum * lum * (3.0 - 2.0 * lum);
            lum *= opticalVignette(uv, 0.38, 0.45);

            float grain = calculateFilmGrain(uv, t, res, lum, 0.75, 0.095);
            lum += grain * (0.85 - abs(lum - 0.45));

            return vec3(clamp(lum, 0.0, 1.0));
        }

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
    // Vertices match glReadPixels output mapping: (x, y, u, v)
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

    /**
     * Processes a full-resolution bitmap through the active film stock optical physics shader
     * using a headless EGL 1.4 context and Framebuffer Object (FBO).
     *
     * @param sourceBitmap The input image bitmap (oriented correctly).
     * @param filterIndex Active film stock filter index (0..5).
     * @param timeSeed Time parameter for procedural silver-halide crystal seed.
     * @return Processed high-resolution Bitmap.
     */
    fun processBitmap(sourceBitmap: Bitmap, filterIndex: Int, timeSeed: Float = 0.5f): Bitmap {
        val width = sourceBitmap.width
        val height = sourceBitmap.height

        var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
        var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
        var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE

        var programId = 0
        var vaoId = 0
        var vboId = 0
        var inTexId = 0
        var outTexId = 0
        var fboId = 0

        try {
            // 1. Initialize EGL 1.4 Display
            eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
                throw RuntimeException("EGL Display initialization failed")
            }

            val version = IntArray(2)
            if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
                throw RuntimeException("EGL Initialize failed")
            }

            // 2. Choose Config
            val configAttribs = intArrayOf(
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_NONE
            )
            val configs = arrayOfNulls<EGLConfig>(1)
            val numConfigs = IntArray(1)
            EGL14.eglChooseConfig(eglDisplay, configAttribs, 0, configs, 0, 1, numConfigs, 0)
            val config = configs[0] ?: throw RuntimeException("No suitable EGLConfig found")

            // 3. Create EGL Context (Client Version 3)
            val contextAttribs = intArrayOf(
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 3,
                EGL14.EGL_NONE
            )
            eglContext = EGL14.eglCreateContext(eglDisplay, config, EGL14.EGL_NO_CONTEXT, contextAttribs, 0)
            if (eglContext == EGL14.EGL_NO_CONTEXT) {
                throw RuntimeException("EGL Context creation failed")
            }

            // 4. Create 1x1 Pbuffer Surface
            val pbufferAttribs = intArrayOf(
                EGL14.EGL_WIDTH, 1,
                EGL14.EGL_HEIGHT, 1,
                EGL14.EGL_NONE
            )
            eglSurface = EGL14.eglCreatePbufferSurface(eglDisplay, config, pbufferAttribs, 0)
            if (eglSurface == EGL14.EGL_NO_SURFACE) {
                throw RuntimeException("EGL Pbuffer Surface creation failed")
            }

            if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
                throw RuntimeException("EGL MakeCurrent failed")
            }

            // 5. Create GLSL Program
            programId = createProgram(vertexShaderSource, fragmentShaderSource)
            if (programId == 0) {
                throw RuntimeException("Failed to compile offscreen film shader program")
            }

            val uTexMatrixLoc = GLES30.glGetUniformLocation(programId, "u_TexMatrix")
            val uTextureLoc = GLES30.glGetUniformLocation(programId, "u_Texture")
            val uTimeLoc = GLES30.glGetUniformLocation(programId, "u_Time")
            val uResolutionLoc = GLES30.glGetUniformLocation(programId, "u_Resolution")
            val uFilterIndexLoc = GLES30.glGetUniformLocation(programId, "u_FilterIndex")

            // 6. Setup VAO & VBO
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

            // 7. Upload Input Bitmap Texture
            val textures = IntArray(2)
            GLES30.glGenTextures(2, textures, 0)
            inTexId = textures[0]
            outTexId = textures[1]

            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, inTexId)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, sourceBitmap, 0)

            // 8. Create Output Texture and attach to FBO
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, outTexId)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
            GLES30.glTexImage2D(
                GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA,
                width, height, 0,
                GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null
            )

            val fbos = IntArray(1)
            GLES30.glGenFramebuffers(1, fbos, 0)
            fboId = fbos[0]
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fboId)
            GLES30.glFramebufferTexture2D(
                GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0,
                GLES30.GL_TEXTURE_2D, outTexId, 0
            )

            val status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)
            if (status != GLES30.GL_FRAMEBUFFER_COMPLETE) {
                throw RuntimeException("Framebuffer incomplete: $status")
            }

            // 9. Render offscreen
            GLES30.glViewport(0, 0, width, height)
            GLES30.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)

            GLES30.glUseProgram(programId)

            val identityMatrix = FloatArray(16)
            Matrix.setIdentityM(identityMatrix, 0)
            GLES30.glUniformMatrix4fv(uTexMatrixLoc, 1, false, identityMatrix, 0)

            GLES30.glUniform1f(uTimeLoc, timeSeed)
            GLES30.glUniform2f(uResolutionLoc, width.toFloat(), height.toFloat())
            GLES30.glUniform1i(uFilterIndexLoc, filterIndex)

            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, inTexId)
            if (uTextureLoc != -1) {
                GLES30.glUniform1i(uTextureLoc, 0)
            }

            GLES30.glBindVertexArray(vaoId)
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
            GLES30.glBindVertexArray(0)

            // 10. Read back pixels from FBO into Output Bitmap
            val pixelBuffer = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder())
            GLES30.glReadPixels(0, 0, width, height, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, pixelBuffer)
            pixelBuffer.position(0)

            val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            resultBitmap.copyPixelsFromBuffer(pixelBuffer)

            return resultBitmap
        } finally {
            // Resource cleanup
            if (fboId != 0) GLES30.glDeleteFramebuffers(1, intArrayOf(fboId), 0)
            if (inTexId != 0) GLES30.glDeleteTextures(1, intArrayOf(inTexId), 0)
            if (outTexId != 0) GLES30.glDeleteTextures(1, intArrayOf(outTexId), 0)
            if (vboId != 0) GLES30.glDeleteBuffers(1, intArrayOf(vboId), 0)
            if (vaoId != 0) GLES30.glDeleteVertexArrays(1, intArrayOf(vaoId), 0)
            if (programId != 0) GLES30.glDeleteProgram(programId)

            if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
                EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                if (eglSurface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(eglDisplay, eglSurface)
                if (eglContext != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(eglDisplay, eglContext)
                EGL14.eglTerminate(eglDisplay)
            }
        }
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
            Log.e(TAG, "Shader Compile Error ($type): ${GLES30.glGetShaderInfoLog(shader)}")
            GLES30.glDeleteShader(shader)
            return 0
        }
        return shader
    }
}
