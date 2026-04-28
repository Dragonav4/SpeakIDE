package com.danilian.speakide.stt.whisperlocal

import com.danilian.speakide.audio.AudioResampler
import com.danilian.speakide.stt.SttProvider
import com.danilian.speakide.stt.SttResult
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.logger
import io.github.givimad.whisperjni.WhisperContext
import io.github.givimad.whisperjni.WhisperFullParams
import io.github.givimad.whisperjni.WhisperJNI
import io.github.givimad.whisperjni.WhisperSamplingStrategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Path

private val LOG = logger<WhisperLocalProvider>()

class WhisperLocalProvider(private val modelPath: String) : SttProvider, Disposable {

    override val displayName = "Whisper (Local)"
    override val requiresNetwork = false

    private val mutex = Mutex()
    private var whisper: WhisperJNI? = null
    private var context: WhisperContext? = null

    @Volatile
    private var disposed = false

    init {
        try {
            WhisperJNI.loadLibrary()
        } catch (e: Exception) {
            LOG.warn("Failed to load WhisperJNI native library", e)
        }
    }

    private suspend fun getOrLoadContext(): Pair<WhisperJNI, WhisperContext> {
        whisper?.let { w -> context?.let { c -> return w to c } }

        return mutex.withLock {
            whisper?.let { w -> context?.let { c -> return@withLock w to c } }

            if (disposed) error("WhisperLocalProvider is already disposed")

            val path = modelPath.ifBlank {
                throw IllegalArgumentException(
                    "Whisper local model path is not set. " +
                            "Set it in Settings → Tools → SpeakIDE → Whisper local model."
                )
            }

            val file = File(path)
            if (!file.exists() || !file.isFile || !file.name.endsWith(".bin")) {
                throw FileNotFoundException("Whisper model not found or invalid at: $path")
            }

            LOG.info("WhisperLocalProvider: loading model from $path")
            val w = WhisperJNI()
            val ctx = withContext(Dispatchers.IO) { w.init(Path.of(path)) }
            
            whisper = w
            context = ctx
            LOG.info("WhisperLocalProvider: model loaded successfully")
            
            w to ctx
        }
    }

    override suspend fun transcribe(audioData: ByteArray, language: String?): SttResult {
        if (audioData.isEmpty()) return SttResult(text = "")

        val (w, ctx) = getOrLoadContext()

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
            
            val lang = language?.takeIf { it != "auto" }
            if (lang != null) {
                params.language = lang
            } else {
                params.language = "auto"
            }

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
            val ctx = context
            if (ctx != null && whisper != null) {
                whisper?.free(ctx)
            }
        } catch (e: Exception) {
            LOG.warn("Error freeing Whisper context", e)
        } finally {
            context = null
            whisper = null
        }
    }
}
