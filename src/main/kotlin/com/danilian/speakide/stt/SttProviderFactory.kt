package com.danilian.speakide.stt

import com.danilian.speakide.settings.SpeakIdeSettings
import com.danilian.speakide.settings.SttProviderOption
import com.danilian.speakide.stt.openai.OpenAiWhisperProvider
import com.danilian.speakide.stt.vosk.VoskProvider

object SttProviderFactory {

    fun create(settings: SpeakIdeSettings = SpeakIdeSettings.getInstance()): SttProvider {
        return when (settings.state.sttProvider) {
            SttProviderOption.VOSK.id -> VoskProvider(settings.state.voskModelPath)
            else -> OpenAiWhisperProvider()   // default: OpenAI Whisper
        }
    }
}
