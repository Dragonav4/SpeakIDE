package com.danilian.speakide.stt

import com.danilian.speakide.audio.AudioData
import com.danilian.speakide.settings.SpeakIdeSettings
import com.danilian.speakide.settings.SttProviderOption
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger

private val LOG = logger<TranscriptionService>()

@Service(Service.Level.APP)
class TranscriptionService : Disposable {

    private var cachedProvider: SttProvider? = null
    private var cachedKey: ProviderCacheKey? = null

    suspend fun transcribe(audio: AudioData, language: String?): SttResult {
        if (audio.pcm.isEmpty()) return SttResult(text = "")
        val provider = getProvider()
        val raw = provider.transcribe(audio, language)
        return provider.postProcess(raw)
    }

    private fun getProvider(): SttProvider {
        val s = SpeakIdeSettings.getInstance().state
        val key = ProviderCacheKey.from(s)
        if (cachedKey != key) {
            LOG.info("TranscriptionService: provider settings changed, recreating provider")
            (cachedProvider as? Disposable)?.dispose()
            cachedProvider = SttProviderFactory.create()
            cachedKey = key
        }
        return cachedProvider!!
    }

    override fun dispose() {
        cachedProvider?.dispose()
        cachedProvider = null
    }

    private data class ProviderCacheKey(
        val provider: SttProviderOption,
        val baseUrl: String,
        val model: String,
        val voskPath: String,
        val whisperLocalPath: String,
    ) {
        companion object {
            fun from(s: SpeakIdeSettings.State) = ProviderCacheKey(
                provider = SttProviderOption.fromId(s.sttProvider),
                baseUrl = s.whisperBaseUrl,
                model = s.whisperModel,
                voskPath = s.voskModelPath,
                whisperLocalPath = s.whisperLocalModelPath,
            )
        }
    }
}
