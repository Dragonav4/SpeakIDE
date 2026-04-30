package com.danilian.speakide.audio


data class AudioFormat(
    val sampleRate: Int,
    val channels: Int,
    val bitsPerSample: Int,
) {
    val bytesPerSample: Int get() = bitsPerSample / 8
    val byteRate: Int get() = sampleRate * channels * bytesPerSample
}

data class AudioData(
    val pcm: ByteArray,
    val format: AudioFormat,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AudioData) return false
        return pcm.contentEquals(other.pcm) && format == other.format
    }

    override fun hashCode(): Int = 31 * pcm.contentHashCode() + format.hashCode()
}
