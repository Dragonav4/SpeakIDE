package com.danilian.speakide.stt.openai

import com.danilian.speakide.toWav
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class OpenAiWhisperProviderTest {

    //pcmToWav

    @Test
    fun `pcmToWav produces correct total size`() {
        val pcm = ByteArray(1000)
        val wav = pcm.toWav( sampleRate = 44100, channels = 1, bitsPerSample = 16)
        assertEquals(1044, wav.size) // 44-byte header + 1000 data
    }

    @Test
    fun `pcmToWav starts with RIFF marker`() {
        val pcm = ByteArray(1000)
        val wav = pcm.toWav( 44100, 1, 16)
        assertEquals("RIFF", String(wav.sliceArray(0..3)))
    }

    @Test
    fun `pcmToWav contains WAVE marker at offset 8`() {
        val pcm = ByteArray(1000)
        val wav = pcm.toWav(44100, 1, 16)
        assertEquals("WAVE", String(wav.sliceArray(8..11)))
    }

    @Test
    fun `pcmToWav contains data marker at offset 36`() {
        val pcm = ByteArray(1000)
        val wav = pcm.toWav( 44100, 1, 16)
        assertEquals("data", String(wav.sliceArray(36..39)))
    }

    // transcribe: guard conditions

    @Test
    fun `transcribe returns empty result for empty audio`() = runBlocking {
        val provider = buildProvider()
        val result = provider.transcribe(ByteArray(0), null)
        assertEquals("", result.text)
    }

    @Test
    fun `transcribe throws when API key is missing`() {
        val provider = buildProvider(apiKey = null)
        assertThrows<IllegalStateException> {
            runBlocking { provider.transcribe(ByteArray(100), null) }
        }
    }

    // transcribe: happy path

    @Test
    fun `transcribe returns text from API response`() = runBlocking {
        val provider = buildProvider(responseBody = """{"text": "hello world"}""")
        val result = provider.transcribe(ByteArray(100), null)
        assertEquals("hello world", result.text)
    }

    @Test
    fun `transcribe trims whitespace from response`() = runBlocking {
        val provider = buildProvider(responseBody = """{"text": "  hello  "}""")
        val result = provider.transcribe(ByteArray(100), null)
        assertEquals("hello", result.text)
    }

    @Test
    fun `transcribe sends correct Authorization header`() = runBlocking {
        var capturedAuth: String? = null
        val provider = buildProvider(
            apiKey = "sk-test-123",
            onRequest = { request -> capturedAuth = request.headers[HttpHeaders.Authorization] },
        )
        provider.transcribe(ByteArray(100), null)
        assertEquals("Bearer sk-test-123", capturedAuth)
    }

    @Test
    fun `transcribe sends language field when specified`() = runBlocking {
        var capturedBody = ""
        val provider = buildProvider(
            onRequest = { request ->
                capturedBody = request.body.toByteArray().decodeToString()
            },
        )
        provider.transcribe(ByteArray(100), language = "fr")
        assert(capturedBody.contains("fr")) { "Expected 'fr' in multipart body" }
    }

    @Test
    fun `transcribe does not send language field when auto`() = runBlocking {
        var capturedBody = ""
        val provider = buildProvider(
            onRequest = { request ->
                capturedBody = request.body.toByteArray().decodeToString()
            },
        )
        provider.transcribe(ByteArray(100), language = "auto")
        assert(!capturedBody.contains("language")) { "Expected no language field for 'auto'" }
    }

    //helpers

    private fun buildProvider(
        responseBody: String = """{"text": ""}""",
        apiKey: String? = "test-key",
        onRequest: (suspend (io.ktor.client.request.HttpRequestData) -> Unit)? = null,
    ): OpenAiWhisperProvider = OpenAiWhisperProvider(
        httpClient = HttpClient(MockEngine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            engine {
                addHandler { request ->
                    onRequest?.invoke(request)
                    respond(
                        content = ByteReadChannel(responseBody),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }
            }
        },
        baseUrl = "https://api.groq.com/openai/v1",
        model = "whisper-large-v3",
        apiKeyProvider = { apiKey },
    )
}