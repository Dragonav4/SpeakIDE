package com.danilian.speakide.stt.vosk

import com.danilian.speakide.stt.SttProvider
import com.danilian.speakide.stt.SttResult
import com.intellij.openapi.Disposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.io.FileNotFoundException

class VoskProvider(private val modelPath: String) : SttProvider, Disposable {

    companion object {
        private const val SAMPLE_RATE = 44100f
        private val TEXT_PATTERN = Regex(""""text"\s*:\s*"([^"]*)"""")

        fun parseVoskResult(json: String): SttResult {
            val text = TEXT_PATTERN.find(json)
                ?.groupValues
                ?.getOrNull(1)
                ?.trim()
                .orEmpty()
            return SttResult(text = text)
        }
    }

    override val displayName = "Vosk (Offline)"
    override val requiresNetwork = false

    private var model: Model? = null
    private val mutex = Mutex()
    private var disposed = false

    private suspend fun getOrLoadModel(): Model {
        model?.let { return it }

        return mutex.withLock {
            model?.let { return@withLock it }

            if (disposed) error("VoskProvider is already disposed")

            val path = modelPath.ifBlank {
                throw IllegalArgumentException(
                    "Vosk model path is not set. " +
                    "Set it in Settings → Tools → SpeakIDE → Vosk Model Path."
                )
            }

            val dir = File(path)
            if (!dir.exists() || !dir.isDirectory) {
                throw FileNotFoundException("Vosk model not found at: $path")
            }

            println("=== VoskProvider: loading model from $path ===")
            val loaded = withContext(Dispatchers.IO) { Model(path) }
            println("=== VoskProvider: model loaded ===")
            model = loaded
            loaded
        }
    }

    override suspend fun transcribe(audioData: ByteArray, language: String?): SttResult {
        if (audioData.isEmpty()) return SttResult(text = "")

        val m = getOrLoadModel()

        println("=== VoskProvider: transcribing ${audioData.size} bytes ===")
        val resultJson = withContext(Dispatchers.IO) {
            Recognizer(m, SAMPLE_RATE).use { recognizer ->
                recognizer.acceptWaveForm(audioData, audioData.size)
                recognizer.finalResult
            }
        }
        println("=== VoskProvider: raw result = $resultJson ===")

        return parseVoskResult(resultJson)
    }

    override fun dispose() {
        disposed = true
        model?.close()
        model = null
    }
}
