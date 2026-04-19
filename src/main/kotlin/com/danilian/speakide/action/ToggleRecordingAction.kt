package com.danilian.speakide.action

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware

class ToggleRecordingAction : AnAction(), DumbAware {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project

        // TODO replace with RecordingOrchestrator.toggle() in the future
        NotificationGroupManager.getInstance()
            .getNotificationGroup("SpeakIDE")
            .createNotification(
                "SpeakIDE",
                "Shortcut works ;)",
                NotificationType.INFORMATION
            )
            .notify(project)
    }

    override fun update(e: AnActionEvent) {
        // Always enabled for now
        e.presentation.isEnabledAndVisible = true
    }
}
