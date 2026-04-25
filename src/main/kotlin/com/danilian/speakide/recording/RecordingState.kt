package com.danilian.speakide.recording

import com.danilian.speakide.audio.AudioSource
import com.danilian.speakide.audio.PcmBuffer
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project

/**
 * Transitions:
 *   [Idle] ──startRecording──▶ [Recording] ──stopRecording──▶ [Transcribing] ──▶ [Idle] ──error──▶ [Idle]
 */
sealed class RecordingState {

    /** Plugin is not recording. Default state. */
    data object Idle : RecordingState()

    /**
     * Microphone is open and audio is being buffered.
     */
    data class Recording(
        val project: Project?,
        val editor: Editor?,
        val capture: AudioSource,
        val buffer: PcmBuffer,
    ) : RecordingState()

    /**
     * Recording has stopped and the accumulated PCM is being sent to the STT provider.
     * The project and editor are carried over so the result can be delivered to the right target.
     */
    data class Transcribing(
        val project: Project?,
        val editor: Editor?,
    ) : RecordingState()
}
