package com.danilian.speakide.action

import com.danilian.speakide.AudioCapture
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware

class ToggleRecordingAction : AnAction(), DumbAware {

    private var capture: AudioCapture? = null

    override fun actionPerformed(e: AnActionEvent) {
        if (capture == null) {
            capture = AudioCapture(
                onData = { /* TODO: data for STT */ },
                onError = { capture = null }
            ).also { it.start() }
        } else {
            capture?.stop()
            capture = null
        }
    }

    override fun update(e: AnActionEvent) {
        // Always enabled for now
        e.presentation.isEnabledAndVisible = true
    }
}
