package com.danilian.speakide.stt

import com.danilian.speakide.audio.AudioData
import com.intellij.openapi.Disposable

interface SttProvider : Disposable {
    val displayName: String
    val requiresNetwork: Boolean

    suspend fun transcribe(audio: AudioData, language: String?): SttResult

    fun postProcess(result: SttResult): SttResult = result

    override fun dispose() {}
}
