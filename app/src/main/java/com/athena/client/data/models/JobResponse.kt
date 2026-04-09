package com.athena.client.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JobSubmitResponse(
    @SerialName("job_id") val jobId: String,
    val status: String
)

@Serializable
data class JobStatusResponse(
    @SerialName("job_id") val jobId: String,
    val status: String,
    val response: String? = null,
    val audio: String? = null,
    val error: String? = null
)
