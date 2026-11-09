package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.annotation.DrawableRes
import java.io.File
import java.io.FileOutputStream

object WallpaperStorageHelper {
    private const val TAG = "WallpaperStorageHelper"

    /**
     * Copies an image from any Content URI (from Gallery, Photo Picker, Files, Drive)
     * into the app's permanent private storage directory so read permissions never expire.
     */
    fun saveUriToInternalStorage(context: Context, uri: Uri, prefix: String = "custom_wp"): String? {
        return try {
            val dir = File(context.filesDir, "wallpapers").apply { mkdirs() }
            val destFile = File(dir, "${prefix}_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            if (destFile.exists() && destFile.length() > 0) {
                Log.d(TAG, "Saved custom wallpaper to ${destFile.absolutePath} (${destFile.length()} bytes)")
                destFile.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving URI to internal storage", e)
            null
        }
    }

    /**
     * Saves a camera snapshot or generated Bitmap to internal storage.
     */
    fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap, prefix: String = "camera_wp"): String? {
        return try {
            val dir = File(context.filesDir, "wallpapers").apply { mkdirs() }
            val destFile = File(dir, "${prefix}_${System.currentTimeMillis()}.jpg")
            FileOutputStream(destFile).use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)
            }
            if (destFile.exists() && destFile.length() > 0) {
                destFile.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving bitmap to internal storage", e)
            null
        }
    }

    /**
     * Copies a bundled sample device photo into internal storage, behaving exactly
     * as if the user had that photo file in their device gallery.
     */
    fun saveDrawableToInternalStorage(context: Context, @DrawableRes drawableResId: Int, prefix: String = "device_photo"): String? {
        return try {
            val dir = File(context.filesDir, "wallpapers").apply { mkdirs() }
            val destFile = File(dir, "${prefix}_${System.currentTimeMillis()}.jpg")
            context.resources.openRawResource(drawableResId).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            if (destFile.exists() && destFile.length() > 0) {
                destFile.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error copying drawable to internal storage", e)
            null
        }
    }

    /**
     * Safely converts a path or URI string to an object Coil can load reliably.
     */
    fun toCoilModel(customUri: String): Any {
        return when {
            customUri.startsWith("/") -> File(customUri)
            customUri.startsWith("file://") -> {
                val parsed = Uri.parse(customUri)
                parsed.path?.let { File(it) } ?: parsed
            }
            customUri.startsWith("content://") -> Uri.parse(customUri)
            customUri.startsWith("http://") || customUri.startsWith("https://") -> Uri.parse(customUri)
            else -> File(customUri)
        }
    }
}
