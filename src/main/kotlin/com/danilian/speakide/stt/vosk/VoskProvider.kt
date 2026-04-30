package com.danilian.speakide.stt.vosk

import com.danilian.speakide.audio.AudioData
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

    override fun releaseResource(resource: Model) = resource.close()

    override suspend fun transcribe(audio: AudioData, language: String?): SttResult {
        if (audio.pcm.isEmpty()) return SttResult(text = "")

        val model = getOrLoad()
        val sampleRate = audio.format.sampleRate.toFloat()

        LOG.debug("VoskProvider: transcribing ${audio.pcm.size} bytes @ ${audio.format.sampleRate} Hz")
        val resultJson = withContext(Dispatchers.IO) {
            Recognizer(model, sampleRate).use { recognizer ->
                recognizer.acceptWaveForm(audio.pcm, audio.pcm.size)
                recognizer.finalResult
            }
        }
        LOG.debug("VoskProvider: raw result = $resultJson")

        return parseVoskResult(resultJson)
    }
}

@Serializable
private data class VoskResponse(val text: String = "")
