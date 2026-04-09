package com.athena.client.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SpeakRequest(
    val text: String,
    @SerialName("speaker_voice") val speakerVoice: String? = null
)
