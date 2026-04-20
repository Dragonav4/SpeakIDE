package com.danilian.speakide

import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.SilenceDetector
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory
import com.danilian.speakide.audio.SilenceTimeoutProcessor


class AudioCapture(
    private val silenceThresholdDb: Double = -70.0,
    private val silenceDurationMs: Long = 2000L,
    private val onSilenceTimeout: () -> Unit = {},
    private val onData: (ByteArray) -> Unit
) {

    val silenceDetector = SilenceDetector(silenceThresholdDb, false)

    private val dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(
        1024, 0
    ).also { d ->
        d.addAudioProcessor(silenceDetector)
        d.addAudioProcessor(SilenceTimeoutProcessor(
            silenceDetector = silenceDetector,
            silenceThresholdDb = silenceThresholdDb,
            silenceDurationMs = silenceDurationMs,
            onSilenceTimeout = onSilenceTimeout
        ))
        d.addAudioProcessor(object : AudioProcessor {
            override fun process(event: AudioEvent): Boolean {
                onData(event.byteBuffer.copyOf())
                return true
            }

            override fun processingFinished() = Unit
        })
    }

    fun start() {
        Thread(dispatcher, "speakide-audio-capture").apply {
            isDaemon = true
            start()
        }
    }

    fun stop() = dispatcher.stop()

    fun currentDb(): Double = silenceDetector.currentSPL()

    fun isSilent(): Boolean = currentDb() < silenceThresholdDb
}
