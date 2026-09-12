package com.example.util

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

object CloudinaryUploader {
    private const val TAG = "CloudinaryUploader"
    const val DEFAULT_CLOUD_NAME = "jg8dnjho"
    const val DEFAULT_UPLOAD_PRESET = "anu_tools_payment_proofs"
    const val PAYMENT_PROOFS_UPLOAD_PRESET = "anu_tools_payment_proofs"
    const val FOLDER_PAYMENT_PROOFS = "anu_tools/payment-proofs"
    const val FOLDER_RETAILER_PROFILES = "anu_tools/retailers"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun uploadImageFromUri(
        context: Context,
        imageUri: Uri,
        folder: String,
        cloudName: String = DEFAULT_CLOUD_NAME,
        uploadPreset: String = PAYMENT_PROOFS_UPLOAD_PRESET
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val inputStream: InputStream = contentResolver.openInputStream(imageUri)
                ?: return@withContext Result.failure(Exception("Unable to open selected image file."))

            val buffer = ByteArrayOutputStream()
            val chunk = ByteArray(8192)
            var bytesRead: Int
            inputStream.use { input ->
                while (input.read(chunk).also { bytesRead = it } != -1) {
                    buffer.write(chunk, 0, bytesRead)
                }
            }
            val imageBytes = buffer.toByteArray()
            if (imageBytes.isEmpty()) {
                return@withContext Result.failure(Exception("Selected image is empty."))
            }

            val mimeType = contentResolver.getType(imageUri) ?: "image/jpeg"
            val effectiveCloudName = cloudName.ifBlank { DEFAULT_CLOUD_NAME }
            val effectivePreset = uploadPreset.ifBlank { PAYMENT_PROOFS_UPLOAD_PRESET }

            uploadBytes(
                imageBytes = imageBytes,
                mimeType = mimeType,
                folder = folder,
                cloudName = effectiveCloudName,
                uploadPreset = effectivePreset
            )
        } catch (e: Exception) {
            Log.e(TAG, "uploadImageFromUri failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun uploadBytes(
        imageBytes: ByteArray,
        mimeType: String = "image/jpeg",
        folder: String,
        cloudName: String = DEFAULT_CLOUD_NAME,
        uploadPreset: String = PAYMENT_PROOFS_UPLOAD_PRESET
    ): Result<String> = withContext(Dispatchers.IO) {
        val effectiveCloud = cloudName.ifBlank { DEFAULT_CLOUD_NAME }
        val effectivePreset = uploadPreset.ifBlank { PAYMENT_PROOFS_UPLOAD_PRESET }

        try {
            val url = "https://api.cloudinary.com/v1_1/$effectiveCloud/image/upload"
            val mediaType = mimeType.toMediaTypeOrNull() ?: "image/jpeg".toMediaTypeOrNull()

            val bodyBuilder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    "payment_proof_${System.currentTimeMillis()}.jpg",
                    imageBytes.toRequestBody(mediaType)
                )
                .addFormDataPart("upload_preset", effectivePreset)

            if (folder.isNotBlank()) {
                bodyBuilder.addFormDataPart("folder", folder)
            }

            val request = Request.Builder()
                .url(url)
                .post(bodyBuilder.build())
                .build()

            val response = client.newCall(request).execute()
            val responseString = response.body?.string().orEmpty()

            if (response.isSuccessful && responseString.isNotBlank()) {
                val json = JSONObject(responseString)
                val secureUrl = json.optString("secure_url").ifBlank { json.optString("url") }
                if (secureUrl.isNotBlank()) {
                    Log.d(TAG, "Cloudinary upload success: $secureUrl (preset: $effectivePreset, folder: $folder)")
                    return@withContext Result.success(secureUrl)
                }
                Result.failure(Exception("Upload succeeded but no image URL returned by Cloudinary."))
            } else {
                val parsedErrorMsg = try {
                    val json = JSONObject(responseString)
                    val errorObj = json.optJSONObject("error")
                    errorObj?.optString("message") ?: responseString
                } catch (e: Exception) {
                    responseString
                }
                Log.w(TAG, "Cloudinary upload failed with code ${response.code}: $parsedErrorMsg")
                val formattedMsg = if (parsedErrorMsg.contains("Upload preset not found", ignoreCase = true) || parsedErrorMsg.contains("whitelisted for unsigned", ignoreCase = true)) {
                    "Cloudinary upload preset '$effectivePreset' not found or not set to 'Unsigned'. In Cloudinary Settings > Upload > Upload Presets, ensure '$effectivePreset' has Signing Mode set to 'Unsigned'."
                } else {
                    "Cloudinary upload error (${response.code}): $parsedErrorMsg"
                }
                Result.failure(Exception(formattedMsg))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cloudinary connection error: ${e.message}", e)
            Result.failure(e)
        }
    }
}
