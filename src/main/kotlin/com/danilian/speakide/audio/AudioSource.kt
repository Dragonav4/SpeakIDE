package com.danilian.speakide.audio

interface AudioSource {
    fun start()

    fun stop()

    fun currentDb(): Double

    fun isSilent(): Boolean
}
