package com.danilian.speakide.audio

import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.SilenceDetector
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory
import com.danilian.speakide.settings.SpeakIdeConstants
import com.intellij.openapi.diagnostic.logger
import javax.sound.sampled.LineUnavailableException

private val LOG = logger<AudioCapture>()


class AudioCapture(
    private val silenceThresholdDb: Double = -70.0,
    private val silenceDurationMs: Long = 2000L,
    private val onSilenceTimeout: () -> Unit = {},
    private val onData: (ByteArray) -> Unit,
    private val onMicDenied: () -> Unit = {},
    private val onError: (Exception) -> Unit = {},
) : AudioSource {

    private val silenceDetector = SilenceDetector(silenceThresholdDb, false)

    @Volatile
    private var dispatcher: AudioDispatcher? = null

    override fun start() {
        Thread(::runCapture, "speakide-audio-capture").apply {
            isDaemon = true
            start()
        }
    }

    private fun runCapture() {
        try {
            val d = AudioDispatcherFactory.fromDefaultMicrophone(
                SpeakIdeConstants.AUDIO_BUFFER_SIZE,
                SpeakIdeConstants.AUDIO_BUFFER_OVERLAP
            )
            d.addAudioProcessor(silenceDetector)
            d.addAudioProcessor(
                MicPermissionProbe(
                    silenceDetector = silenceDetector,
                    onMicDenied = {
                        onMicDenied()
                        // TODO: detect actual OS for the error message
                        onError(SecurityException("Microphone access denied by macOS"))
                    }
                )
            )
            d.addAudioProcessor(
                SilenceTimeoutProcessor(
                    silenceDetector = silenceDetector,
                    silenceThresholdDb = silenceThresholdDb,
                    silenceDurationMs = silenceDurationMs,
                    onSilenceTimeout = onSilenceTimeout
                )
            )
            d.addAudioProcessor(PcmForwardingProcessor(onData))
            dispatcher = d
            d.run() // blocks until stop() is called
        } catch (e: LineUnavailableException) {
            LOG.warn("AudioCapture: microphone line unavailable", e)
            onMicDenied()
            onError(e)
        } catch (e: Exception) {
            LOG.warn("AudioCapture: unexpected error", e)
            onError(e)
        }
    }

    override fun stop() {
        dispatcher?.stop()
    }

    override fun currentDb(): Double = silenceDetector.currentSPL()

    override fun isSilent(): Boolean = currentDb() < silenceThresholdDb
}

private class PcmForwardingProcessor(private val onData: (ByteArray) -> Unit) : AudioProcessor {
    override fun process(event: AudioEvent): Boolean {
        val floats = event.floatBuffer
        val pcm = ByteArray(floats.size * 2)
        for (i in floats.indices) {
            val sample = (floats[i] * 32767.0f).toInt().coerceIn(-32768, 32767)
            pcm[i * 2] = (sample and 0xFF).toByte()
            pcm[i * 2 + 1] = ((sample shr 8) and 0xFF).toByte()
        }
        onData(pcm)
        return true
    }

    override fun processingFinished() = Unit
}
