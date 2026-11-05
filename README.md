# VintageCam Android 📷

A modern, high-performance vintage analog camera application for Android built with **Kotlin**, **Jetpack Compose**, **CameraX**, and an **OpenGL ES 3.0 Optical Physics Shader Engine**.

Inspired by 35mm mechanical rangefinders and point-and-shoot compacts, VintageCam combines authentic photochemical film emulation with a tactile industrial design ("Concept 46").

---

## ✨ Key Features

### 🎞️ 1. OpenGL ES 3.0 Optical Physics Shader Engine
- **Real-Time Viewfinder Rendering:** Bypasses standard camera previews with a custom `GLSurfaceView` and `VintageGLRenderer` rendering via `SurfaceTexture` at a smooth 60 FPS (`RENDERMODE_WHEN_DIRTY`).
- **Luminance-Responsive Emulsion Grain:** Procedural silver-halide grain generator modulated by Rec.709 luminance (heavy in shadows and midtones, soft roll-off in highlights).
- **Anti-Halation Bloom:** Multi-tap radial scatter kernel detecting high-contrast specular highlights to simulate red-orange tungsten halation.
- **Optical Lens Vignetting:** Cosine-fourth law optical falloff towards corners.

### 🎨 2. Curated Film Emulation Stocks
| Film Stock | Character & Profile |
| :--- | :--- |
| **Kodak Portra 400** | Warm golden pastels, gentle shadow roll-off, fine grain, subtle halation |
| **CineStill 800T** | Cool cyan/tungsten color cast, deep night blacks, intense vermillion halation |
| **Fuji Velvia 50** | Saturated emerald greens, rich vivid skies, deep slide contrast, micro-grain |
| **Ilford HP5 Plus 400** | Timeless silver-gelatin black-and-white, authentic photochemical grain |
| **Retro 1980s Point-and-Shoot** | Milky faded blacks, warm color cast, aggressive corner vignetting |
| **Standard** | Natural pass-through color science |

### 🎛️ 3. Minimalist Leica/Halide-Inspired Interface
- **Top Utility Header (`TopUtilityHeader.kt`):** A sleek, 46.dp single-row matte header housing essential status and quick controls:
  - Amber monospace frame counter (`EXP XX/36`).
  - Tactile Aspect Ratio selector pill (`3:2`, `4:3`, `9:16`).
  - Flash mode toggle (`Auto`, `On`, `Off`).
  - Rule-of-thirds grid overlay toggle.
  - Incandescent orange `'90s Quartz Date` stamp toggle (`'26`).
- **Contextual Pro Telemetry Bar (`ProControlBar.kt`):** A 48.dp matte-black strip with 5 telemetry tabs (`FILM`, `ISO`, `SS`, `FOCUS`, `EV`) displaying real-time camera parameters:
  - Tapping any tab expands a unified precision vernier dial drawer directly above the telemetry strip with smooth animated transitions.
  - Dedicated contextual `AUTO`, `AF`, or `0.0` reset button on the left edge for instant one-touch return to automatic modes.
  - Horizontal vernier stop selector on the right for fine manual adjustment.
  - Tapping the active tab or the viewfinder collapses the drawer immediately to keep the screen uncluttered.
- **Machined Aluminum Shutter Deck (`BottomShutterDeck.kt`):**
  - **Recessed Gallery Thumbnail (48.dp):** Displays latest captured image with subtle rounded corners and launches Android gallery on tap.
  - **Leica Shutter Button (72.dp):** Machined concentric aluminum bezel with specular highlights, physical spring-physics haptic compression, and a signature red center dot.
  - **Camera Lens Flip Button (48.dp):** Minimalist circular dark button toggling front and rear cameras.

### ⚙️ 4. Real Hardware Manual Controls (Camera2Interop)
- **Direct HAL Control via Camera2Interop:** Leverages `@androidx.annotation.OptIn(ExperimentalCamera2Interop::class)` via `Camera2CameraControl` and `Camera2CameraInfo`.
- **Dynamic Hardware Capability Discovery:**
  - Queries `CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE` and clamps discrete ISO stops (`ISO 50` to `ISO 3200`) to sensor limits.
  - Queries `CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE` and generates supported shutter stops from `1/4000s` to `1/4s` mapped to nanoseconds.
  - Queries `CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE` to detect manual focus motor capabilities.
- **Unified CaptureRequest Options:**
  - **Manual Exposure:** Setting ISO or Shutter switches `CONTROL_AE_MODE` to `CONTROL_AE_MODE_OFF` and applies `SENSOR_SENSITIVITY` and `SENSOR_EXPOSURE_TIME` simultaneously without clobbering.
  - **Manual Focus:** Adjusting the focus dial switches `CONTROL_AF_MODE` to `CONTROL_AF_MODE_OFF` and sets `LENS_FOCUS_DISTANCE` from `0.0f` (Infinity ∞) to macro limit.
  - **Auto Exposure & EV Compensation:** When in Auto AE mode, supports exposure compensation from -2.0 EV to +2.0 EV in 1/3-stop steps.
  - **Viewfinder Tap-to-Focus:** Tapping anywhere on the viewfinder triggers CameraX 3A tap-to-focus and metering, and automatically restores auto-focus mode if previously locked in manual focus.

### 📐 5. Viewfinder Masking & Framing Guides
- **True Fitted Aspect Ratio Masking:** Switch instantly between **3:2** (classic 35mm film negative standard), **4:3** (vintage compact point-and-shoot), and **9:16** (full-height modern vertical frame).
  - Uses dynamic `BoxWithConstraints` geometry that strictly prevents vertical overflow on ultra-tall modern displays (e.g., Galaxy S24 Ultra, Pixel 8/9).
  - Constrains height in 9:16 to preserve side pillarboxing without pushing framing brackets or the quartz datestamp off-screen.
  - Constrains width in 4:3 and 3:2 with uniform letterboxing, giving each mode a distinct visual silhouette.
- **Zero-Latency Native 4:3 Sensor Lock:** CameraX hardware stream is locked strictly to 4:3 (native mobile camera sensor aspect ratio), eliminating stream renegotiation, frame freezes, and autofocus re-locking when cycling aspect ratios.
- **Optical Sensor Crop Framing (`u_AspectCrop`):** Vertex shader applies centered optical crop framing in screen-aligned UV space before sensor rotation, guaranteeing 100% full vertical sensor height (`cropY = 1.0f`) with zero digital zoom across 4:3, 3:2, and 9:16 modes.
- **Dynamic Viewfinder Window Resizing:** Viewfinder frame physically changes shape on screen with smooth letterboxing and pillarboxing, matching the exact aspect ratio with an inset border and framing reticle.
- **35mm Reticle:** Minimalist corner brackets, center rangefinder focus circle, and toggleable rule-of-thirds grid.
- **'90s Quartz Date Stamp:** Incandescent orange datestamp (`#FF7A00`) displayed in the viewfinder HUD and burned cleanly into exported photos.

### 💾 6. High-Resolution Offline GPU Processing & MediaStore Export
- Full-sensor captures (`ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY`) are grabbed as in-memory buffers.
- Processed offscreen through an OpenGL ES 3.0 Framebuffer Object (`OffscreenFilmRenderer`) with active film shaders.
- Losslessly cropped to the active aspect ratio, stamped with the quartz date, and saved to `Pictures/VintageCam/` in Android MediaStore with standard EXIF metadata.

### 🌐 7. Universal Android & Modern GPU Compatibility
- **Universal GPU Architecture Support:** Strict GLSL ES 3.00 compliance across Qualcomm Adreno (Adreno 640/730/740/750 on Snapdragon 8 Gen 1/2/3, including Galaxy S24 Ultra), ARM Mali (Mali-G76/G78/G715/Immortalis), and PowerVR.
- **Opaque Sampler Scoping:** Samplers are bound globally with explicit `glUniform1i(uTextureLoc, 0)` texture unit mapping, avoiding driver-level function parameter rejections.
- **Android 14+ Lifecycle & Looper Synchronization:** `SurfaceTexture` frame listener explicitly dispatches to `Looper.getMainLooper()` to guarantee reliable frame pumping across 120Hz/240Hz camera HALs.
- **Hardware Layering:** `setZOrderMediaOverlay(true)` enables clean punch-through rendering without occlusion from Compose RenderNodes.

---

## 🛠️ Architecture & Tech Stack

- **Target SDK:** 35 | **Min SDK:** 26 (Android 8.0+)
- **Language:** Kotlin 2.0+ (Kotlin Coroutines)
- **UI Framework:** Jetpack Compose (Single-Activity Architecture, Material 3)
- **Camera Core:** AndroidX CameraX (`camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-view`)
- **Graphics Pipeline:** OpenGL ES 3.0, GLSL (`GLES30`, `GL_OES_EGL_image_external_essl3`)
- **Haptic Engine:** Android `Vibrator` / `VibratorManager` (`VibrationEffect`)

---

## 🚀 Building & Running

### Prerequisites
- JDK 17 (e.g. Eclipse Adoptium OpenJDK 17)
- Android SDK Platform 35

### Commands
```bash
# Set JDK 17 environment (if not default)
export JAVA_HOME="/path/to/jdk-17"

# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew testDebugUnitTest
```

The compiled debug APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 📄 License
This project is licensed under the Apache License 2.0.
