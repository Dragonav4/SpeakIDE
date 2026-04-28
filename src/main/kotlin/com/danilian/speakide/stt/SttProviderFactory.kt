package com.danilian.speakide.stt

import com.danilian.speakide.settings.SpeakIdeSettings
import com.danilian.speakide.settings.SttProviderOption
import com.danilian.speakide.stt.openai.OpenAiWhisperProvider
import com.danilian.speakide.stt.vosk.VoskProvider
import com.danilian.speakide.stt.whisperlocal.WhisperLocalProvider


class SttProviderFactory(private val settings: SpeakIdeSettings = SpeakIdeSettings.getInstance()) {
    fun create(): SttProvider = when (settings.state.sttProvider) {
        SttProviderOption.OPENAI_WHISPER.id -> OpenAiWhisperProvider.create(settings)
        SttProviderOption.VOSK.id -> VoskProvider(settings.state.voskModelPath)
        SttProviderOption.WHISPER_LOCAL.id -> WhisperLocalProvider(settings.state.whisperLocalModelPath)
        else -> OpenAiWhisperProvider.create(settings)
    }

    companion object {
        fun create(settings: SpeakIdeSettings = SpeakIdeSettings.getInstance()): SttProvider =
            SttProviderFactory(settings).create()
    }
}
