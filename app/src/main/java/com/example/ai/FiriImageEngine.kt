package com.example.ai

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

sealed class FiriImageResult {
    data class Success(
        val filePath: String,
        val caption: String,
        val modelUsed: String,
        val wasEdit: Boolean
    ) : FiriImageResult()

    data class Error(
        val message: String,
        val isMissingKey: Boolean = false
    ) : FiriImageResult()
}

/**
 * Motor multimodal de Firi para crear y editar imágenes usando `gemini-nano-banana-2.1`
 * y sus variantes compatibles en Gemini API v1beta.
 */
class FiriImageEngine(private val context: Context) {

    // La generación de imágenes requiere ventanas realistas de 30-60s
    private val imageHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val imageModelCandidates = listOf(
        "gemini-nano-banana-2.1",
        "gemini-3.1-flash-image-preview",
        "gemini-2.5-flash-image",
        "gemini-3-pro-image-preview"
    )

    suspend fun generateOrEditImage(
        prompt: String,
        inputImageUriOrPath: String? = null,
        aspectRatio: String = "1:1",
        isSpanish: Boolean = true
    ): FiriImageResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            val msg = if (isSpanish) {
                "Falta configurar GEMINI_API_KEY en el panel Secrets de AI Studio para generar y editar imágenes con Nano Banana 2.1."
            } else {
                "GEMINI_API_KEY is missing in AI Studio's Secrets panel. Add it to generate and edit images with Nano Banana 2.1."
            }
            return@withContext FiriImageResult.Error(msg, isMissingKey = true)
        }

        val isEdit = !inputImageUriOrPath.isNullOrBlank()
        val encodedInput = if (isEdit) {
            encodeImageToBase64(inputImageUriOrPath!!)
        } else null

        if (isEdit && encodedInput == null) {
            val msg = if (isSpanish) {
                "No pude leer la imagen seleccionada para editarla. Prueba eligiendo otra imagen."
            } else {
                "Could not read the selected image for editing. Try picking another photo."
            }
            return@withContext FiriImageResult.Error(msg)
        }

        val partsArray = JSONArray().apply {
            if (encodedInput != null) {
                put(JSONObject().apply {
                    put("inlineData", JSONObject().apply {
                        put("mimeType", encodedInput.first)
                        put("data", encodedInput.second)
                    })
                })
            }
            put(JSONObject().put("text", prompt))
        }

        val contentsArray = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "user")
                put("parts", partsArray)
            })
        }

        var lastError = if (isSpanish) "No se recibió respuesta del modelo de imagen." else "No response from image model."

        try {
            withTimeout(65_000L) {
                for (model in imageModelCandidates) {
                    // Intentar primero con imageConfig (aspectRatio), y si el endpoint lo rechaza (HTTP 400), reintentar sin imageConfig
                    val configVariants = if (aspectRatio != "1:1") listOf(true, false) else listOf(false)
                    for (includeImageConfig in configVariants) {
                        val genConfig = JSONObject().apply {
                            put("responseModalities", JSONArray().apply {
                                put("TEXT")
                                put("IMAGE")
                            })
                            if (includeImageConfig) {
                                put("imageConfig", JSONObject().apply {
                                    put("aspectRatio", aspectRatio)
                                })
                            }
                        }

                        val bodyJson = JSONObject().apply {
                            put("contents", contentsArray)
                            put("generationConfig", genConfig)
                        }

                        val request = Request.Builder()
                            .url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey")
                            .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
                            .build()

                        try {
                            imageHttpClient.newCall(request).execute().use { response ->
                                val respStr = response.body?.string()
                                if (response.isSuccessful && !respStr.isNullOrBlank()) {
                                    val parsed = parseAndSaveImageResponse(respStr, isEdit, model, isSpanish)
                                    if (parsed != null) {
                                        return@withTimeout parsed
                                    } else {
                                        lastError = if (isSpanish) "El modelo ($model) no devolvió datos de imagen." else "Model ($model) returned no image data."
                                    }
                                } else {
                                    val errDetail = extractApiErrorMessage(respStr)
                                    lastError = "HTTP ${response.code}" + (if (errDetail.isNotBlank()) ": $errDetail" else "")
                                    Log.w("FiriImageEngine", "Model $model HTTP ${response.code}: ${respStr?.take(300)}")
                                    // Si es 404 (modelo no encontrado), saltar directo al siguiente modelo
                                    if (response.code == 404) break
                                }
                            }
                        } catch (e: Exception) {
                            lastError = e.message ?: e.javaClass.simpleName
                            Log.w("FiriImageEngine", "Request failed for $model: ${e.message}")
                        }
                    }
                }
                FiriImageResult.Error(
                    if (isSpanish) "No se pudo generar la imagen ($lastError)."
                    else "Image generation failed ($lastError)."
                )
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            FiriImageResult.Error(
                if (isSpanish) "La generación de imagen superó los 60 segundos. Inténtalo de nuevo."
                else "Image generation timed out after 60 seconds. Please try again."
            )
        }
    }

    private fun parseAndSaveImageResponse(
        respStr: String,
        isEdit: Boolean,
        modelUsed: String,
        isSpanish: Boolean
    ): FiriImageResult.Success? {
        val root = JSONObject(respStr)
        val candidates = root.optJSONArray("candidates") ?: return null
        val firstCandidate = candidates.optJSONObject(0) ?: return null
        val content = firstCandidate.optJSONObject("content") ?: return null
        val parts = content.optJSONArray("parts") ?: return null

        var imageBytes: ByteArray? = null
        val textNotes = StringBuilder()

        for (i in 0 until parts.length()) {
            val part = parts.optJSONObject(i) ?: continue
            if (part.optBoolean("thought", false)) continue

            val t = part.optString("text")
            if (!t.isNullOrBlank()) {
                textNotes.append(t).append(" ")
            }

            val inline = part.optJSONObject("inlineData") ?: part.optJSONObject("inline_data")
            if (inline != null && imageBytes == null) {
                val b64 = inline.optString("data")
                if (b64.isNotBlank()) {
                    imageBytes = Base64.decode(b64, Base64.DEFAULT)
                }
            }
        }

        val rawBytes = imageBytes ?: return null
        val bitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size) ?: return null
        val savedFile = saveBitmapToPrivateStorage(bitmap) ?: return null

        val cleanCaption = textNotes.toString()
            .replace(Regex("[*#`_~>]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
            .ifBlank {
                if (isEdit) {
                    if (isSpanish) "Aquí tienes la imagen editada con Nano Banana 2.1."
                    else "Here is your edited image from Nano Banana 2.1."
                } else {
                    if (isSpanish) "Aquí tienes la imagen generada con Nano Banana 2.1."
                    else "Here is your generated image from Nano Banana 2.1."
                }
            }

        return FiriImageResult.Success(
            filePath = savedFile.absolutePath,
            caption = cleanCaption,
            modelUsed = modelUsed,
            wasEdit = isEdit
        )
    }

    private fun saveBitmapToPrivateStorage(bitmap: Bitmap): File? {
        return try {
            val dir = File(context.filesDir, "firi_images")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "firi_img_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 96, out)
                out.flush()
            }
            file
        } catch (e: Exception) {
            Log.e("FiriImageEngine", "Failed saving generated bitmap", e)
            null
        }
    }

    private fun encodeImageToBase64(uriOrPath: String): Pair<String, String>? {
        return try {
            val rawBitmap = if (uriOrPath.startsWith("content://") || uriOrPath.startsWith("file://")) {
                val uri = Uri.parse(uriOrPath)
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            } else {
                BitmapFactory.decodeFile(uriOrPath)
            } ?: return null

            // Escalar a máximo 1440px para mantener el payload ligero y rápido
            val maxDim = 1440
            val w = rawBitmap.width
            val h = rawBitmap.height
            val scaledBitmap = if (w > maxDim || h > maxDim) {
                val scale = maxDim.toFloat() / maxOf(w, h).toFloat()
                Bitmap.createScaledBitmap(
                    rawBitmap,
                    (w * scale).toInt().coerceAtLeast(1),
                    (h * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else {
                rawBitmap
            }

            val baos = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos)
            val b64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
            "image/jpeg" to b64
        } catch (e: Exception) {
            Log.e("FiriImageEngine", "Failed encoding input image", e)
            null
        }
    }

    private fun extractApiErrorMessage(respStr: String?): String {
        if (respStr.isNullOrBlank()) return ""
        return try {
            val errObj = JSONObject(respStr).optJSONObject("error")
            errObj?.optString("message")?.take(120) ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    companion object {
        /**
         * Guarda una imagen generada en la galería del dispositivo usando MediaStore
         * sin requerir permisos amplios de almacenamiento en Android 10+.
         */
        fun saveToDeviceGallery(context: Context, filePath: String): Boolean {
            val sourceFile = File(filePath)
            if (!sourceFile.exists()) return false

            return try {
                val fileName = "Firi_${System.currentTimeMillis()}.png"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Firi AI")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                    val resolver = context.contentResolver
                    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
                    resolver.openOutputStream(uri)?.use { out ->
                        sourceFile.inputStream().use { input -> input.copyTo(out) }
                    }
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                    true
                } else {
                    @Suppress("DEPRECATION")
                    val bitmap = BitmapFactory.decodeFile(filePath) ?: return false
                    @Suppress("DEPRECATION")
                    val url = MediaStore.Images.Media.insertImage(
                        context.contentResolver,
                        bitmap,
                        fileName,
                        "Imagen creada con Firi AI"
                    )
                    !url.isNullOrBlank()
                }
            } catch (e: Exception) {
                Log.e("FiriImageEngine", "Failed saving to gallery", e)
                false
            }
        }
    }
}
