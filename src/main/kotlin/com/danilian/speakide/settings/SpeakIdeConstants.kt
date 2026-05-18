package com.danilian.speakide.settings

import com.danilian.speakide.audio.AudioFormat


object SpeakIdeConstants {
    const val NOTIFICATION_GROUP_ID = "SpeakIDE"

    const val AUDIO_BUFFER_SIZE = 1024

    const val AUDIO_BUFFER_OVERLAP = 0

    const val SAMPLE_RATE = 44100

    const val CHANNELS = 1

    const val BITS_PER_SAMPLE = 16

    val CAPTURE_FORMAT = AudioFormat(SAMPLE_RATE, CHANNELS, BITS_PER_SAMPLE)

    const val AUTO_LANGUAGE = "auto"

    val SUPPORTED_LANGUAGES = listOf(AUTO_LANGUAGE, "en", "ru", "de", "fr", "es", "zh", "ja")
}
