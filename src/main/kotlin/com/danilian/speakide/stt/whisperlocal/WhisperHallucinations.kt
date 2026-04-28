package com.danilian.speakide.stt.whisperlocal

object WhisperHallucinations {

    val KNOWN_PHRASES = listOf(
        // Russian Hallucinations (Movie subtitles & credits)
        // All phrases must be lowercase — isHallucination() compares against text.lowercase()
        "редактор субтитров",
        "корректор",
        "субтитры сделаны",
        "субтитры подготовил",
        "субтитры создавал",
        "перевод на русский",
        "подзаголовки",
        "озвучено",
        "продолжение следует",
        "а.семкин",
        "а.егорова",
        "студия пифагор",
        "dimatorzok",
        "amara.org",

        // English Hallucinations (YouTube & TV credits)
        "subtitles by",
        "subtitle by",
        "captions by",
        "captioning sponsored by",
        "translated by",
        "thank you for watching",
        "thanks for watching",
        "please subscribe",
        "subscribe to my channel",
        "subscribe for more",
        "www.youtube.com",
        "mbc",
        "kbs",
        "you can support us",
        "patreon"
    )

    fun isHallucination(text: String): Boolean {
        // If the text is long, it's likely a real dictation that happens to contain a trigger word.
        if (text.length > 150) return false

        val lowerText = text.lowercase()
        return KNOWN_PHRASES.any { it in lowerText }
    }
}
