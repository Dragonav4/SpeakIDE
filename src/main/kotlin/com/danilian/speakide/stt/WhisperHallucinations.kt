package com.danilian.speakide.stt

object WhisperHallucinations {

    val KNOWN_PHRASES = listOf(
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
        "patreon",
    )

    fun isHallucination(text: String): Boolean {
        if (text.length > 150) return false
        val lowerText = text.lowercase()
        return KNOWN_PHRASES.any { it in lowerText }
    }
}
