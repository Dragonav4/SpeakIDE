package com.danilian.speakide.stt.openai

import com.danilian.speakide.audio.AudioData
import com.danilian.speakide.audio.toWav
import com.danilian.speakide.settings.SecureStorage
import com.danilian.speakide.settings.SpeakIdeConstants
import com.danilian.speakide.settings.SpeakIdeSettings
import com.danilian.speakide.stt.SttProvider
import com.danilian.speakide.stt.SttResult
import com.danilian.speakide.stt.WhisperHallucinations
import com.intellij.openapi.diagnostic.logger
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val LOG = logger<OpenAiWhisperProvider>()

class OpenAiWhisperProvider(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val model: String,
    private val apiKeyProvider: () -> String? = { SecureStorage.getOpenAiKey() },
) : SttProvider {

    override val displayName = "OpenAI Whisper (Cloud)"
    override val requiresNetwork = true

    override suspend fun transcribe(audio: AudioData, language: String?): SttResult {
        if (audio.pcm.isEmpty()) return SttResult(text = "")
        val apiKey = requireApiKey()
        val wavBytes = audio.toWavBytes()
        val response = sendTranscriptionRequest(wavBytes, apiKey, language)
        return parseResponse(response)
    }

    private fun requireApiKey(): String =
        apiKeyProvider() ?: throw IllegalStateException(
            "API key is not set. Add it in Settings → Tools → SpeakIDE."
        )

    private fun AudioData.toWavBytes(): ByteArray =
        pcm.toWav(sampleRate = format.sampleRate, channels = format.channels, bitsPerSample = format.bitsPerSample)

    private suspend fun sendTranscriptionRequest(wavBytes: ByteArray, apiKey: String, language: String?): HttpResponse =
        withContext(Dispatchers.IO) {
            httpClient.post("$baseUrl/audio/transcriptions") {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                setBody(buildMultipartBody(wavBytes, language))
            }
        }

    private fun buildMultipartBody(wavBytes: ByteArray, language: String?) =
        MultiPartFormDataContent(
            formData {
                append("file", wavBytes, Headers.build {
                    append(HttpHeaders.ContentType, "audio/wav")
                    append(HttpHeaders.ContentDisposition, "filename=\"audio.wav\"")
                })
                append("model", model)
                if (language != null && language != SpeakIdeConstants.AUTO_LANGUAGE) {
                    append("language", language)
                }
                append("response_format", "json")
            }
        )

    private suspend fun parseResponse(response: HttpResponse): SttResult {
        if (response.status.value !in 200..299) {
            throw IllegalStateException("API Error (${response.status}): ${response.bodyAsText()}")
        }
        val parsed: WhisperResponse = response.body()
        LOG.debug("OpenAiWhisperProvider: result = ${parsed.text}")
        return SttResult(text = parsed.text.trim())
    }

    override fun postProcess(result: SttResult): SttResult =
        WhisperHallucinations.filterResult(result, displayName, LOG)

    companion object {
        private val sharedClient: HttpClient by lazy {
            HttpClient(CIO) {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
            }
        }

        fun create(settings: SpeakIdeSettings = SpeakIdeSettings.getInstance()): OpenAiWhisperProvider =
            OpenAiWhisperProvider(
                httpClient = sharedClient,
                baseUrl = settings.state.whisperBaseUrl,
                model = settings.state.whisperModel,
            )
    }
}

@Serializable
private data class WhisperResponse(
    @SerialName("text") val text: String,
)
