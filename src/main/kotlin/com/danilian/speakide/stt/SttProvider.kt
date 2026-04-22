package com.danilian.speakide.stt


interface SttProvider {
    val displayName: String
    val requiresNetwork: Boolean
    suspend fun transcribe(audioData: ByteArray, language: String?): SttResult
}
