package com.danilian.speakide.stt

data class SttResult(
    val text: String,
    val confidence: Float? = null,
    val detectedLanguage: String? = null,
)
