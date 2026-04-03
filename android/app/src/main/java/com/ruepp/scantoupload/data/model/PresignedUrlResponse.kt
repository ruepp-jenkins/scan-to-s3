package com.ruepp.scantoupload.data.model

data class PresignedUrlResponse(
    val uploadUrl: String,
    val key: String,
    val headers: Map<String, String>
)
