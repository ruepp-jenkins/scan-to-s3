package com.ruepp.scantoupload.data.api

import com.ruepp.scantoupload.data.model.PresignedUrlResponse
import com.ruepp.scantoupload.data.preferences.ServerConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApiClient(
    private val serverConfig: ServerConfig
) {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val requestBuilder = chain.request().newBuilder()
            val token = serverConfig.getAppToken()
            if (token.isNotBlank()) {
                requestBuilder.addHeader("X-App-Token", token)
            }
            chain.proceed(requestBuilder.build())
        }
        .build()

    // Separate client for S3 uploads — no auth interceptor, custom timeouts
    private val uploadClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(300, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun baseUrl(): String {
        return serverConfig.getServerUrl().trimEnd('/')
    }

    fun checkStatus(): Result<Unit> {
        val request = Request.Builder()
            .url("${baseUrl()}/api/app/status")
            .get()
            .build()

        return try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val error = try {
                    JSONObject(responseBody).optString("error", "Connection failed")
                } catch (_: Exception) {
                    "Connection failed (${response.code})"
                }
                Result.failure(ApiException(response.code, error))
            }
        } catch (e: IOException) {
            Result.failure(ApiException(0, "Connection failed: ${e.message}"))
        }
    }

    fun getPresignedUrl(filename: String): Result<PresignedUrlResponse> {
        val json = JSONObject().apply {
            put("filename", filename)
        }
        val body = json.toString().toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url("${baseUrl()}/api/app/upload/presigned-url")
            .post(body)
            .build()

        return try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val responseJson = JSONObject(responseBody)
                val headers = mutableMapOf<String, String>()
                val headersJson = responseJson.optJSONObject("headers")
                if (headersJson != null) {
                    for (key in headersJson.keys()) {
                        headers[key] = headersJson.getString(key)
                    }
                }
                Result.success(
                    PresignedUrlResponse(
                        uploadUrl = responseJson.getString("uploadUrl"),
                        key = responseJson.getString("key"),
                        headers = headers
                    )
                )
            } else {
                val error = try {
                    JSONObject(responseBody).optString("error", "Failed to get upload URL")
                } catch (_: Exception) {
                    "Failed to get upload URL (${response.code})"
                }
                Result.failure(ApiException(response.code, error))
            }
        } catch (e: IOException) {
            Result.failure(ApiException(0, "Connection failed: ${e.message}"))
        }
    }

    fun uploadToS3(
        uploadUrl: String,
        headers: Map<String, String>,
        requestBody: RequestBody
    ): Result<Unit> {
        val requestBuilder = Request.Builder()
            .url(uploadUrl)
            .put(requestBody)

        for ((key, value) in headers) {
            requestBuilder.addHeader(key, value)
        }

        return try {
            val response = uploadClient.newCall(requestBuilder.build()).execute()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(
                    ApiException(response.code, "Upload failed with status ${response.code}")
                )
            }
        } catch (e: IOException) {
            Result.failure(ApiException(0, "Upload failed: ${e.message}"))
        }
    }
}

class ApiException(val code: Int, message: String) : Exception(message) {
    val isUnauthorized: Boolean get() = code == 401 || code == 403
}
