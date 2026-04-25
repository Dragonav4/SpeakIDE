package com.danilian.speakide.settings


object SpeakIdeConstants {
    const val NOTIFICATION_GROUP_ID = "SpeakIDE"

    const val AUDIO_BUFFER_SIZE = 1024

    const val AUDIO_BUFFER_OVERLAP = 0

    const val SAMPLE_RATE = 44100

    const val CHANNELS = 1

    const val BITS_PER_SAMPLE = 16

    val SUPPORTED_LANGUAGES = listOf("auto", "en", "ru", "de", "fr", "es", "zh", "ja")
}
