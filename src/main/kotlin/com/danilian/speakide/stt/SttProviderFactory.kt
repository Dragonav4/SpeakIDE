package com.danilian.speakide.stt

import com.danilian.speakide.settings.SpeakIdeSettings
import com.danilian.speakide.settings.SttProviderOption
import com.danilian.speakide.stt.openai.OpenAiWhisperProvider
import com.danilian.speakide.stt.vosk.VoskProvider
import com.danilian.speakide.stt.whisperlocal.WhisperLocalProvider


class SttProviderFactory(private val settings: SpeakIdeSettings = SpeakIdeSettings.getInstance()) {
    fun create(): SttProvider = when (SttProviderOption.fromId(settings.state.sttProvider)) {
        SttProviderOption.OPENAI_WHISPER -> OpenAiWhisperProvider.create(settings)
        SttProviderOption.VOSK -> VoskProvider(settings.state.voskModelPath)
        SttProviderOption.WHISPER_LOCAL -> WhisperLocalProvider(settings.state.whisperLocalModelPath)
    }

    companion object {
        fun create(settings: SpeakIdeSettings = SpeakIdeSettings.getInstance()): SttProvider =
            SttProviderFactory(settings).create()
    }
}
