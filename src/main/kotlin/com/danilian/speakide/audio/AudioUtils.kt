package com.danilian.speakide.audio

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Wraps raw PCM bytes in a standard 44-byte RIFF/WAV header.
 * Without this header STT APIs (like Whisper) cannot detect the audio format.
 *
 * WAV header layout (little-endian):
 *  0- 3  "RIFF"
 *  4- 7  file size - 8
 *  8-11  "WAVE"
 * 12-15  "fmt "
 * 16-19  fmt chunk size = 16
 * 20-21  audio format = 1 (PCM)
 * 22-23  num channels
 * 24-27  sample rate
 * 28-31  byte rate = sampleRate * channels * bitsPerSample / 8
 * 32-33  block align = channels * bitsPerSample / 8
 * 34-35  bits per sample
 * 36-39  "data"
 * 40-43  PCM data size
 * 44+    PCM data
 */
fun ByteArray.toWav(sampleRate: Int = 44100, channels: Int = 1, bitsPerSample: Int = 16): ByteArray {
    val byteRate = sampleRate * channels * bitsPerSample / 8
    val blockAlign = channels * bitsPerSample / 8

    return ByteBuffer.allocate(44 + this.size)
        .order(ByteOrder.LITTLE_ENDIAN)
        .put("RIFF".toByteArray())
        .putInt(36 + this.size)
        .put("WAVE".toByteArray())
        .put("fmt ".toByteArray())
        .putInt(16)
        .putShort(1)
        .putShort(channels.toShort())
        .putInt(sampleRate)
        .putInt(byteRate)
        .putShort(blockAlign.toShort())
        .putShort(bitsPerSample.toShort())
        .put("data".toByteArray())
        .putInt(this.size)
        .put(this)
        .array()
}
