package com.ruepp.scantoupload.util

import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.InputStream

/**
 * OkHttp RequestBody that streams from an InputStream and reports upload progress.
 * Avoids loading the entire file into memory.
 */
class ProgressRequestBody(
    private val inputStream: InputStream,
    private val contentLength: Long,
    private val onProgress: (progress: Int) -> Unit
) : RequestBody() {

    override fun contentType(): MediaType = "application/pdf".toMediaType()

    override fun contentLength(): Long = contentLength

    override fun writeTo(sink: BufferedSink) {
        val buffer = ByteArray(8192)
        var uploaded = 0L
        var lastReportedProgress = -1

        inputStream.use { stream ->
            var read: Int
            while (stream.read(buffer).also { read = it } != -1) {
                sink.write(buffer, 0, read)
                uploaded += read
                val progress = if (contentLength > 0) {
                    ((uploaded * 100) / contentLength).toInt().coerceIn(0, 100)
                } else {
                    0
                }
                if (progress != lastReportedProgress) {
                    lastReportedProgress = progress
                    onProgress(progress)
                }
            }
        }
    }
}
