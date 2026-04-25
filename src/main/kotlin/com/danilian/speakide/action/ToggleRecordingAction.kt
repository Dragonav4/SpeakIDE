package com.danilian.speakide.action

import com.danilian.speakide.recording.RecordingService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.IconLoader


class ToggleRecordingAction : AnAction(), DumbAware {

    override fun actionPerformed(e: AnActionEvent) {
        service<RecordingService>().toggle(e.project, e.getData(CommonDataKeys.EDITOR))
    }

    override fun update(e: AnActionEvent) {
        val recording = service<RecordingService>().isRecording()
        e.presentation.isEnabledAndVisible = true
        e.presentation.text = if (recording) "SpeakIDE: Stop Recording" else "SpeakIDE: Start Voice Recording"
        e.presentation.icon = if (recording) ICON_RECORDING else ICON_IDLE
    }

    companion object {
        private val ICON_IDLE = IconLoader.getIcon("/icons/microphone.svg", ToggleRecordingAction::class.java)
        private val ICON_RECORDING =
            IconLoader.getIcon("/icons/microphone_recording.svg", ToggleRecordingAction::class.java)
    }
}
