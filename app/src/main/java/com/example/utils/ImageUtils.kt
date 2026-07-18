package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.min

object ImageUtils {
    /**
     * Loads a selected image URI, crops to a 1:1 square if requested,
     * resizes it to an optimized resolution, compresses it as PNG/JPEG,
     * and saves it to local app-specific private storage for offline-first rendering.
     */
    fun processAndSaveImage(
        context: Context,
        uri: Uri,
        fileName: String,
        cropToSquare: Boolean = false,
        compressQuality: Int = 85
    ): String? {
        return try {
            val contentResolver = context.contentResolver
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            if (inputStream == null) return null

            // Decode with bounds first to prevent Out Of Memory errors
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            // Open stream again to read the bitmap data
            val reOpenedStream = contentResolver.openInputStream(uri) ?: return null
            val originalBitmap = BitmapFactory.decodeStream(reOpenedStream)
            reOpenedStream.close()
            if (originalBitmap == null) return null

            // 1. Image Crop (1:1 aspect ratio)
            val processedBitmap = if (cropToSquare) {
                val size = min(originalBitmap.width, originalBitmap.height)
                val x = (originalBitmap.width - size) / 2
                val y = (originalBitmap.height - size) / 2
                Bitmap.createBitmap(originalBitmap, x, y, size, size)
            } else {
                originalBitmap
            }

            // 2. Auto Resize (Limit maximum resolution to 1024px for balanced quality/performance)
            val maxDimension = 1024
            val finalBitmap = if (processedBitmap.width > maxDimension || processedBitmap.height > maxDimension) {
                val ratio = processedBitmap.width.toFloat() / processedBitmap.height.toFloat()
                val targetWidth = if (ratio > 1) maxDimension else (maxDimension * ratio).toInt()
                val targetHeight = if (ratio > 1) (maxDimension / ratio).toInt() else maxDimension
                Bitmap.createScaledBitmap(processedBitmap, targetWidth, targetHeight, true)
            } else {
                processedBitmap
            }

            // 3. Save securely to private files directory (Offline-first / Fast loading cache)
            val file = File(context.filesDir, fileName)
            val outputStream = FileOutputStream(file)
            
            // Support PNG transparent format compression
            val compressFormat = if (fileName.endsWith(".png", true)) {
                Bitmap.CompressFormat.PNG
            } else {
                Bitmap.CompressFormat.JPEG
            }

            finalBitmap.compress(compressFormat, compressQuality, outputStream)
            outputStream.flush()
            outputStream.close()

            // Return file Uri string for standard Coil rendering
            Uri.fromFile(file).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
