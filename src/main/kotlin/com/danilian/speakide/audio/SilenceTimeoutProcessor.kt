package com.danilian.speakide.audio

import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.SilenceDetector

class SilenceTimeoutProcessor(
    private val silenceDetector: SilenceDetector,
    private val silenceThresholdDb: Double,
    private val silenceDurationMs: Long = 2000L,
    private val onSilenceTimeout: () -> Unit
) : AudioProcessor {

    @Volatile private var silenceStartedAt: Long? = null
    @Volatile private var fired = false
    @Volatile private var speechDetected = false

    override fun process(event: AudioEvent): Boolean {
        if (fired) return true

        val spl = silenceDetector.currentSPL()
        if (spl.isInfinite()) return true

        val isSilent = spl < silenceThresholdDb

        if (!isSilent) {
            speechDetected = true
            silenceStartedAt = null
        } else if (speechDetected) {
            if (silenceStartedAt == null) {
                silenceStartedAt = System.currentTimeMillis()
            }
            val elapsed = System.currentTimeMillis() - silenceStartedAt!!
            if (elapsed >= silenceDurationMs) {
                fired = true
                onSilenceTimeout()
            }
        }

        return true
    }

    override fun processingFinished() = Unit

    fun reset() {
        silenceStartedAt = null
        fired = false
        speechDetected = false
    }
}
