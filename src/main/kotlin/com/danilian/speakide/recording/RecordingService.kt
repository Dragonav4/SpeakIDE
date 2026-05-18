package com.danilian.speakide.recording

import com.danilian.speakide.audio.AudioCapture
import com.danilian.speakide.audio.AudioData
import com.danilian.speakide.audio.PcmBuffer
import com.danilian.speakide.delivery.ResultDelivery
import com.danilian.speakide.notification.SpeakIdeNotifier
import com.danilian.speakide.settings.SpeakIdeConstants
import com.danilian.speakide.settings.SpeakIdeSettings
import com.danilian.speakide.stt.TranscriptionService
import com.danilian.speakide.ui.RecordingIndicator
import com.danilian.speakide.ui.RecordingOverlay
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@Service(Service.Level.APP)
class RecordingService : Disposable {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val notifier = SpeakIdeNotifier()
    private val indicator: RecordingIndicator = RecordingOverlay()
    private val delivery = ResultDelivery(notifier)

    @Volatile
    private var state: RecordingState = RecordingState.Idle

    fun isRecording(): Boolean = state is RecordingState.Recording

    fun toggle(project: Project?, editor: Editor?) {
        when (val current = state) {
            is RecordingState.Idle -> startRecording(project, editor)
            is RecordingState.Recording -> stopRecording(current)
            is RecordingState.Transcribing -> Unit // ignore mid-transcription toggles
        }
    }

    private fun startRecording(project: Project?, editor: Editor?) {
        val buffer = PcmBuffer()
        val settings = SpeakIdeSettings.getInstance().state

        val capture = AudioCapture(
            silenceDurationMs = settings.silenceThresholdMs.toLong(),
            onSilenceTimeout = if (settings.silenceDetectionEnabled) {
                { stopRecording(state as? RecordingState.Recording ?: return@AudioCapture) }
            } else {
                {}
            },
            onData = { chunk -> buffer.add(chunk) },
            onMicDenied = {
                notifier.notifyMicDenied(project)
                transitionToIdle()
            },
            onError = { ex ->
                notifier.notifyAudioError(project, ex.message)
                transitionToIdle()
            }
        )

        state = RecordingState.Recording(project, editor, capture, buffer)
        if (settings.showRecordingOverlay) indicator.show()
        capture.start()
    }

    private fun stopRecording(current: RecordingState.Recording) {
        current.capture.stop()
        indicator.hide()
        state = RecordingState.Transcribing(current.project, current.editor)

        val pcm = current.buffer.drain()
        if (pcm.isEmpty()) {
            notifier.notifyNothingRecorded(current.project)
            state = RecordingState.Idle
            return
        }

        val audio = AudioData(pcm, SpeakIdeConstants.CAPTURE_FORMAT)
        scope.launch { transcribe(audio, current.project, current.editor) }
    }

    private suspend fun transcribe(audio: AudioData, project: Project?, editor: Editor?) {
        try {
            val lang = SpeakIdeSettings.getInstance().state.language.takeUnless { it == SpeakIdeConstants.AUTO_LANGUAGE }
            val transcriptionService = ApplicationManager.getApplication()
                .getService(TranscriptionService::class.java)
            val text = transcriptionService.transcribe(audio, lang).text.trim()
            delivery.deliver(text, project, editor)
        } catch (ex: Throwable) {
            notifier.notifyRecognitionError(project, ex)
        } finally {
            state = RecordingState.Idle
        }
    }

    private fun transitionToIdle() {
        indicator.hide()
        state = RecordingState.Idle
    }

    override fun dispose() {
        (state as? RecordingState.Recording)?.capture?.stop()
        indicator.hide()
        scope.cancel()
    }
}
