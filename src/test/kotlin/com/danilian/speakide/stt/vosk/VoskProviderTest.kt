package com.danilian.speakide.stt.vosk

import io.mockk.unmockkConstructor
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.vosk.Model
import org.vosk.Recognizer
import java.io.FileNotFoundException

class VoskProviderTest {

    @AfterEach
    fun tearDown() {
        // Clean up constructor mocks after each test.
        unmockkConstructor(Model::class)
        unmockkConstructor(Recognizer::class)
    }

    @Test
    fun `parseVoskResult extracts text from valid json`() {
        val result = VoskProvider.parseVoskResult("""{"text": "hello world"}""")
        assertEquals("hello world", result.text)
    }

    @Test
    fun `parseVoskResult returns empty string for empty json`() {
        val result = VoskProvider.parseVoskResult("""{"text": ""}""")
        assertEquals("", result.text)
    }

    @Test
    fun `parseVoskResult trims whitespace`() {
        val result = VoskProvider.parseVoskResult("""{"text" : "  hello  "}""")
        assertEquals("hello", result.text)
    }

    @Test
    fun `parseVoskResult returns empty string for malformed json`() {
        val result = VoskProvider.parseVoskResult("not json at all")
        assertEquals("", result.text)
    }

    @Test
    fun `transcribe returns empty result for empty audio`() = runBlocking {
        val provider = VoskProvider("")
        val result = provider.transcribe(ByteArray(0), null)
        assertEquals("", result.text)
    }

    @Test
    fun `transcribe throws IllegalArgumentException when modelPath is blank`() {
        val provider = VoskProvider("")
        assertThrows<IllegalArgumentException> {
            runBlocking { provider.transcribe(ByteArray(100), null) }
        }
    }

    @Test
    fun `transcribe throws FileNotFoundException when model directory does not exist`() {
        val provider = VoskProvider("/nonexistent/path/to/model")
        assertThrows<FileNotFoundException> {
            runBlocking { provider.transcribe(ByteArray(100), null) }
        }
    }

    @Test
    fun `transcribe throws after dispose`() {
        val provider = VoskProvider("/some/path")
        provider.dispose()
        assertThrows<IllegalStateException> {
            runBlocking { provider.transcribe(ByteArray(100), null) }
        }
    }
}
