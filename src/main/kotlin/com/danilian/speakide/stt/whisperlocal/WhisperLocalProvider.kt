package com.danilian.speakide.stt.whisperlocal

import com.danilian.speakide.audio.AudioResampler
import com.danilian.speakide.stt.OfflineSttProvider
import com.danilian.speakide.stt.SttResult
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

    override suspend fun transcribe(audioData: ByteArray, language: String?): SttResult {
        if (audioData.isEmpty()) return SttResult(text = "")

        val (w, ctx) = getOrLoad()

        // Whisper requires 16000Hz float32
        val floatData = AudioResampler.resampleTo16kFloat(audioData)
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
            params.language = language?.takeIf { it != "auto" } ?: "auto"

            val res = w.full(ctx, params, floatData, floatData.size)
            if (res != 0) {
                LOG.warn("Whisper JNI full() returned error code: $res")
                return@withContext ""
            }

            val numSegments = w.fullNSegments(ctx)
            val sb = StringBuilder()
            for (i in 0 until numSegments) {
                sb.append(w.fullGetSegmentText(ctx, i))
            }

            val rawText = sb.toString().trim()
            if (WhisperHallucinations.isHallucination(rawText)) {
                LOG.info("WhisperLocalProvider: Filtered out hallucination: $rawText")
                ""
            } else {
                rawText
            }
        }

        LOG.debug("WhisperLocalProvider: result = $text")
        return SttResult(text = text)
    }

    override fun dispose() {
        disposed = true
        try {
            val (w, ctx) = getLoadedResource() ?: return
            w.free(ctx)
        } catch (e: Exception) {
            LOG.warn("Error freeing Whisper context", e)
        } finally {
            clearResource()
        }
    }
}
