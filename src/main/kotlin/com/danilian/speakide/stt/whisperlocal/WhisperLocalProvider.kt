package com.danilian.speakide.stt.whisperlocal

import com.danilian.speakide.audio.AudioData
import com.danilian.speakide.audio.AudioResampler
import com.danilian.speakide.settings.SpeakIdeConstants
import com.danilian.speakide.stt.OfflineSttProvider
import com.danilian.speakide.stt.SttResult
import com.danilian.speakide.stt.WhisperHallucinations
import com.intellij.openapi.diagnostic.logger
import io.github.givimad.whisperjni.WhisperContext
import io.github.givimad.whisperjni.WhisperFullParams
import io.github.givimad.whisperjni.WhisperJNI
import io.github.givimad.whisperjni.WhisperSamplingStrategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.nio.file.Path

private val LOG = logger<WhisperLocalProvider>()

class WhisperLocalProvider(modelPath: String) : OfflineSttProvider<Pair<WhisperJNI, WhisperContext>>(modelPath) {

    override val displayName = "Whisper (Local)"
    override val requiresNetwork = false

    init {
        try {
            WhisperJNI.loadLibrary()
        } catch (e: Exception) {
            LOG.warn("Failed to load WhisperJNI native library", e)
        }
    }

    override fun validatePath(path: String) {
        val file = java.io.File(path)
        if (!file.exists() || !file.isFile || !file.name.endsWith(".bin")) {
            throw FileNotFoundException("Whisper model not found or invalid at: $path")
        }
    }

    override suspend fun loadResource(path: String): Pair<WhisperJNI, WhisperContext> {
        val w = WhisperJNI()
        val ctx = w.init(Path.of(path))
        return w to ctx
    }

    override fun releaseResource(resource: Pair<WhisperJNI, WhisperContext>) {
        resource.first.free(resource.second)
    }

    override suspend fun transcribe(audio: AudioData, language: String?): SttResult {
        if (audio.pcm.isEmpty()) return SttResult(text = "")

        val (w, ctx) = getOrLoad()

        val floatData = AudioResampler.resampleTo16kFloat(audio.pcm, audio.format.sampleRate)
        if (floatData.isEmpty()) return SttResult(text = "")

        LOG.debug("WhisperLocalProvider: transcribing ${floatData.size} samples")

        val text = withContext(Dispatchers.IO) {
            val params = WhisperFullParams(WhisperSamplingStrategy.GREEDY)
            params.printProgress = false
            params.printRealtime = false
            params.printTimestamps = false
            params.printSpecial = false
            params.suppressBlank = true
            params.suppressNonSpeechTokens = true
            params.language = language?.takeIf { it != SpeakIdeConstants.AUTO_LANGUAGE } ?: SpeakIdeConstants.AUTO_LANGUAGE

            val res = w.full(ctx, params, floatData, floatData.size)
            if (res != 0) {
                LOG.warn("Whisper JNI full() returned error code: $res")
                return@withContext ""
            }

            val numSegments = w.fullNSegments(ctx)
            (0 until numSegments).joinToString("") { w.fullGetSegmentText(ctx, it) }.trim()
        }

        LOG.debug("WhisperLocalProvider: result = $text")
        return SttResult(text = text)
    }

    override fun postProcess(result: SttResult): SttResult {
        if (WhisperHallucinations.isHallucination(result.text)) {
            LOG.info("WhisperLocalProvider: filtered out hallucination: ${result.text}")
            return result.copy(text = "")
        }
        return result
    }
}
