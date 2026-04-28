package com.danilian.speakide.stt.vosk

import com.danilian.speakide.stt.OfflineSttProvider
import com.danilian.speakide.stt.SttResult
import com.intellij.openapi.diagnostic.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.io.FileNotFoundException

private val LOG = logger<VoskProvider>()

class VoskProvider(modelPath: String) : OfflineSttProvider<Model>(modelPath) {

    companion object {
        private const val SAMPLE_RATE = 44100f
        private val voskJson = Json { ignoreUnknownKeys = true }

        fun parseVoskResult(json: String): SttResult {
            val text = runCatching { voskJson.decodeFromString<VoskResponse>(json).text }
                .getOrDefault("")
                .trim()
            return SttResult(text = text)
        }
    }

    override val displayName = "Vosk (Offline)"
    override val requiresNetwork = false

    override fun validatePath(path: String) {
        val dir = File(path)
        if (!dir.exists() || !dir.isDirectory) {
            throw FileNotFoundException("Vosk model not found at: $path")
        }
    }

    override suspend fun loadResource(path: String): Model = Model(path)

    override suspend fun transcribe(audioData: ByteArray, language: String?): SttResult {
        if (audioData.isEmpty()) return SttResult(text = "")

        val model = getOrLoad()

        LOG.debug("VoskProvider: transcribing ${audioData.size} bytes")
        val resultJson = withContext(Dispatchers.IO) {
            Recognizer(model, SAMPLE_RATE).use { recognizer ->
                recognizer.acceptWaveForm(audioData, audioData.size)
                recognizer.finalResult
            }
        }
        LOG.debug("VoskProvider: raw result = $resultJson")

        return parseVoskResult(resultJson)
    }

    override fun dispose() {
        disposed = true
        getLoadedResource()?.close()
        clearResource()
    }
}

@Serializable
private data class VoskResponse(val text: String = "")
