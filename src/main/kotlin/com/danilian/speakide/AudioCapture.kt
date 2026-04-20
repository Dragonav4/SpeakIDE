package com.danilian.speakide

import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.SilenceDetector
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory
import com.danilian.speakide.audio.MicPermissionProbe
import com.danilian.speakide.audio.SilenceTimeoutProcessor
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import javax.sound.sampled.LineUnavailableException


class AudioCapture(
    private val silenceThresholdDb: Double = -70.0,
    private val silenceDurationMs: Long = 2000L,
    private val onSilenceTimeout: () -> Unit = {},
    private val onData: (ByteArray) -> Unit,
    private val onError: (Exception) -> Unit = {},
) {

    val silenceDetector = SilenceDetector(silenceThresholdDb, false)
    private var dispatcher: AudioDispatcher? = null

    private fun notifyMicDenied() {
        try {
            ApplicationManager.getApplication() ?: return
            NotificationGroupManager.getInstance()
                .getNotificationGroup("SpeakIDE")
                .createNotification(
                    "Microphone access denied",
                    "SpeakIDE cannot open the microphone. " +
                        "Go to System Settings → Privacy & Security → Microphone, " +
                        "enable access for IntelliJ IDEA, then restart the IDE.",
                    NotificationType.ERROR
                )
                .notify(null)
        } catch (_: NoClassDefFoundError) {
        }
    }

    fun start() {
        try {
            dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(1024, 0).also { d ->
                d.addAudioProcessor(silenceDetector)
                d.addAudioProcessor(MicPermissionProbe(
                    silenceDetector = silenceDetector,
                    onMicDenied = {
                        notifyMicDenied()
                        onError(SecurityException("Microphone access denied by macOS"))
                    }
                ))
                d.addAudioProcessor(
                    SilenceTimeoutProcessor(
                        silenceDetector = silenceDetector,
                        silenceThresholdDb = silenceThresholdDb,
                        silenceDurationMs = silenceDurationMs,
                        onSilenceTimeout = onSilenceTimeout
                    )
                )
                d.addAudioProcessor(object : AudioProcessor {
                    override fun process(event: AudioEvent): Boolean {
                        onData(event.byteBuffer.copyOf())
                        return true
                    }

                    override fun processingFinished() = Unit
                })
            }
            Thread(dispatcher, "speakide-audio-capture").apply {
                isDaemon = true
                start()
            }
        } catch (e: LineUnavailableException) {
            notifyMicDenied()
            onError(e)
        } catch (e: Exception) {
            onError(e)
        }
    }

    fun stop() = dispatcher?.stop()

    fun currentDb(): Double = silenceDetector.currentSPL()

    fun isSilent(): Boolean = currentDb() < silenceThresholdDb
}
