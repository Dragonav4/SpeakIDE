package com.danilian.speakide.audio

import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.SilenceDetector

class MicPermissionProbe(
    private val silenceDetector: SilenceDetector,
    private val checkAfterFrames: Int = 50,
    private val onMicDenied: () -> Unit,
) : AudioProcessor {

    private var frameCount = 0

    override fun process(event: AudioEvent): Boolean {
        frameCount++
        if (frameCount == checkAfterFrames && silenceDetector.currentSPL().isInfinite()) {
            onMicDenied()
            return false
        }
        return true
    }

    override fun processingFinished() = Unit
}
