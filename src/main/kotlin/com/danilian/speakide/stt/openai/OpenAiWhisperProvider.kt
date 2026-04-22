package com.danilian.speakide.stt.openai

import com.danilian.speakide.settings.SecureStorage
import com.danilian.speakide.settings.SpeakIdeSettings
import com.danilian.speakide.stt.SttProvider
import com.danilian.speakide.stt.SttResult
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.danilian.speakide.toWav
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * STT provider backed by the OpenAI Whisper API (or any compatible endpoint).
 * Compatible providers:
 *   OpenAI — baseUrl = "https://api.openai.com/v1", model = "whisper-1"
 *   Groq — baseUrl = "https://api.groq.com/openai/v1", model = "whisper-large-v3"
 */
class OpenAiWhisperProvider(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val model: String,
    private val apiKeyProvider: () -> String? = { SecureStorage.getOpenAiKey() },
) : SttProvider {

    override val displayName = "OpenAI Whisper (Cloud)"
    override val requiresNetwork = true

    /**
     * Transcribes raw PCM audio via the Whisper API.
     *
     * Pipeline:
     *   ByteArray (raw PCM) → WAV (44-byte RIFF header prepended)
     *   → multipart/form-data POST → JSON response → SttResult
     */
    override suspend fun transcribe(audioData: ByteArray, language: String?): SttResult {
        if (audioData.isEmpty()) return SttResult(text = "")

        val apiKey = apiKeyProvider()
            ?: throw IllegalStateException(
                "API key is not set. Add it in Settings → Tools → SpeakIDE."
            )

        val wavBytes = audioData.toWav(sampleRate = SAMPLE_RATE, channels = CHANNELS, bitsPerSample = BITS_PER_SAMPLE)

        val response = withContext(Dispatchers.IO) {
            httpClient.post("$baseUrl/audio/transcriptions") {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("file", wavBytes, Headers.build {
                                append(HttpHeaders.ContentType, "audio/wav")
                                append(HttpHeaders.ContentDisposition, "filename=\"audio.wav\"")
                            })
                            append("model", model)
                            if (language != null && language != "auto") {
                                append("language", language)
                            }
                            append("response_format", "json")
                        }
                    )
                )
            }
        }

        if (response.status.value !in 200..299) {
            val errorBody = response.bodyAsText()
            throw IllegalStateException("API Error (${response.status}): $errorBody")
        }

        val parsedResponse: WhisperResponse = response.body()
        return SttResult(text = parsedResponse.text.trim())
    }

    companion object {
        private const val SAMPLE_RATE = 44100
        private const val CHANNELS = 1
        private const val BITS_PER_SAMPLE = 16

        fun create(settings: SpeakIdeSettings = SpeakIdeSettings.getInstance()): OpenAiWhisperProvider =
            OpenAiWhisperProvider(
                httpClient = HttpClient(CIO) {
                    install(ContentNegotiation) {
                        json(Json { ignoreUnknownKeys = true })
                    }
                },
                baseUrl = settings.state.whisperBaseUrl,
                model = settings.state.whisperModel,
            )
    }
}

@Serializable
private data class WhisperResponse(
    @SerialName("text") val text: String, )
