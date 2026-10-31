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

### 🎛️ 3. Concept 46 Tactile Industrial Interface
- **Matte Titanium Top Bezel (`#2C2D2F`):** Houses discrete monospace metadata (`EXP 03/36`, `PORTRA 400 • 3:2 • ±0.0 EV`), a 5-bar micro calibration meter, and quick toggles.
- **Embedded Exposure Slider:** Sleek horizontal vernier scale at the top of the viewfinder for smooth exposure compensation fine-tuning (-2.0 EV to +2.0 EV in 1/3-stop steps).
- **Asymmetric Camera Grip Panel:**
  - **FVL Glow Arc Dial:** Mechanical dial with sweeping incandescent orange glow gauge (`#FF7A00`).
  - **Multi-Colored Meter:** 6-segment LED progress meter (green, yellow, orange, red).
  - **Horizontal Ruler Gauge:** Precision millimeter tick scale with a bright red hairline indicator (`#FF3B30`).
  - **Dark Telemetry Readout Box:** Inset LCD display showing Shutter Speed (`SS`), Exposure (`EV`), Film Speed (`ISO`), and a vintage `S` single-shot mode stamp.
  - **Vertical 'P S' Switch:** Sliding knurled metal selector and machined aluminum screw button.
  - **Recessed Photo-Well:** 3D beveled square frame displaying recent capture thumbnails and launching the gallery via `Intent.ACTION_VIEW`.
  - **Pill-Shaped 'SK' Badge:** Crimson-red and dark metallic emblem toggling the film stock drawer.
  - **Grand Concentric Silver Shutter Button:** Machined lathe-turned metallic grain, specular highlights, and physical spring compression.

### 📐 4. Viewfinder Masking & Framing Guides
- **Aspect Ratio Masking:** Switch seamlessly between **3:2** (classic 35mm film negative standard), **4:3** (point-and-shoot), and **9:16** (full-height modern vertical frame).
- **35mm Reticle:** Minimalist corner brackets, center rangefinder focus circle, and toggleable rule-of-thirds grid.
- **'90s Quartz Date Stamp:** Incandescent orange datestamp (`#FF7A00`) displayed in the viewfinder HUD and burned cleanly into exported photos.

### 💾 5. High-Resolution Offline GPU Processing & MediaStore Export
- Full-sensor captures (`ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY`) are grabbed as in-memory buffers.
- Processed offscreen through an OpenGL ES 3.0 Framebuffer Object (`OffscreenFilmRenderer`) with active film shaders.
- Losslessly cropped to the active aspect ratio, stamped with the quartz date, and saved to `Pictures/VintageCam/` in Android MediaStore with standard EXIF metadata.

### 🌐 6. Universal Android & Modern GPU Compatibility
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
