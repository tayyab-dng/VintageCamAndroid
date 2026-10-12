package com.example.vintagecam.media

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private const val TAG = "MediaSaver"

/**
 * Utility for aspect-ratio cropping, quartz datestamp burning, and saving full-resolution
 * vintage still captures into the public Android MediaStore gallery (Pictures/VintageCam/).
 */
object MediaSaver {

    /**
     * Crops a bitmap to match the requested aspect ratio while centering content cleanly.
     */
    fun cropToAspectRatio(source: Bitmap, targetRatio: Float): Bitmap {
        val currentRatio = source.width.toFloat() / source.height.toFloat()
        // If image is landscape (width > height), invert target ratio for proportional landscape crop
        val effectiveRatio = if (source.width >= source.height) {
            1f / targetRatio
        } else {
            targetRatio
        }

        val cropWidth: Int
        val cropHeight: Int

        if (currentRatio > effectiveRatio) {
            // Wider than target: crop horizontal edges
            cropHeight = source.height
            cropWidth = (cropHeight * effectiveRatio).roundToInt().coerceAtMost(source.width)
        } else if (currentRatio < effectiveRatio) {
            // Taller than target: crop vertical edges
            cropWidth = source.width
            cropHeight = (cropWidth / effectiveRatio).roundToInt().coerceAtMost(source.height)
        } else {
            return source
        }

        val cropX = ((source.width - cropWidth) / 2).coerceAtLeast(0)
        val cropY = ((source.height - cropHeight) / 2).coerceAtLeast(0)

        val cropped = Bitmap.createBitmap(source, cropX, cropY, cropWidth, cropHeight)
        if (cropped != source && !source.isRecycled) {
            source.recycle()
        }
        return cropped
    }

    /**
     * Burns an incandescent orange (#FF7A00) 90s quartz datestamp directly into
     * the bottom-right corner of the film negative.
     */
    fun burnQuartzDateStamp(source: Bitmap, dateString: String): Bitmap {
        val mutableBitmap = if (source.isMutable) {
            source
        } else {
            val copy = source.copy(Bitmap.Config.ARGB_8888, true)
            if (copy != source && !source.isRecycled) {
                source.recycle()
            }
            copy
        }

        val canvas = Canvas(mutableBitmap)

        // Scale text size proportionally to bitmap resolution (approx 2.4% of height)
        val textSize = (mutableBitmap.height * 0.024f).coerceIn(24f, 120f)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 122, 0) // Incandescent quartz orange #FF7A00
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            this.textSize = textSize
            letterSpacing = 0.18f
            // Vintage glowing diode backlight effect
            setShadowLayer(textSize * 0.28f, 0f, 0f, Color.argb(200, 255, 100, 0))
        }

        val textWidth = paint.measureText(dateString)
        val rightMargin = mutableBitmap.width * 0.045f
        val bottomMargin = mutableBitmap.height * 0.04f

        val x = (mutableBitmap.width - rightMargin - textWidth).coerceAtLeast(0f)
        val y = (mutableBitmap.height - bottomMargin).coerceAtLeast(textSize)

        canvas.drawText(dateString, x, y, paint)
        return mutableBitmap
    }

    /**
     * Downscales the full-resolution bitmap to a lightweight thumbnail for the UI preview.
     */
    fun createThumbnail(source: Bitmap, maxDim: Int = 160): Bitmap {
        val aspect = source.width.toFloat() / source.height.toFloat()
        val thumbW = if (aspect >= 1f) maxDim else (maxDim * aspect).roundToInt().coerceAtLeast(1)
        val thumbH = if (aspect >= 1f) (maxDim / aspect).roundToInt().coerceAtLeast(1) else maxDim
        return Bitmap.createScaledBitmap(source, thumbW, thumbH, true)
    }

    /**
     * Saves the final processed bitmap as a lossless 100% quality JPEG into the public
     * Android MediaStore gallery (Pictures/VintageCam/) and writes standard EXIF metadata.
     */
    suspend fun saveToGallery(
        context: Context,
        bitmap: Bitmap,
        filmStock: String,
        isDateStamped: Boolean
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val filename = "VINTAGE_${timeStamp}.jpg"
            val resolver = context.contentResolver

            val uri: Uri
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/VintageCam")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }

                val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return@withContext Result.failure(IOException("Failed to create MediaStore entry"))
                uri = imageUri

                resolver.openOutputStream(uri)?.use { stream ->
                    val success = bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                    if (!success) throw IOException("Bitmap compression to JPEG failed")
                    stream.flush()
                } ?: throw IOException("Could not open output stream for $uri")

                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            } else {
                // Android 8.0 - 9.0 (API 26-28)
                val picturesDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "VintageCam"
                )
                if (!picturesDir.exists()) {
                    picturesDir.mkdirs()
                }
                val photoFile = File(picturesDir, filename)
                FileOutputStream(photoFile).use { stream ->
                    val success = bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                    if (!success) throw IOException("Bitmap compression failed")
                    stream.flush()
                }

                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.DATA, photoFile.absolutePath)
                }
                uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    ?: Uri.fromFile(photoFile)
            }

            // Write standard EXIF metadata
            try {
                resolver.openFileDescriptor(uri, "rw")?.use { pfd ->
                    val exif = ExifInterface(pfd.fileDescriptor)
                    exif.setAttribute(ExifInterface.TAG_MAKE, "VintageCam Analog")
                    exif.setAttribute(ExifInterface.TAG_MODEL, "${Build.MODEL} (35mm Emulation)")
                    val exifDate = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).format(Date())
                    exif.setAttribute(ExifInterface.TAG_DATETIME, exifDate)
                    exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, exifDate)
                    exif.setAttribute(ExifInterface.TAG_SOFTWARE, "VintageCam Optical Physics Engine")
                    exif.setAttribute(ExifInterface.TAG_USER_COMMENT, "Film: $filmStock, QuartzDate: $isDateStamped")
                    exif.saveAttributes()
                }
            } catch (e: Exception) {
                Log.w(TAG, "EXIF attributes write failed: ${e.message}")
            }

            Log.d(TAG, "Photo successfully saved to MediaStore: $uri")
            Result.success(uri)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save photo to MediaStore", e)
            Result.failure(e)
        }
    }
}
