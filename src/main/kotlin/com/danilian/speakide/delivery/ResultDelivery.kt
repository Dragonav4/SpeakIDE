package com.danilian.speakide.delivery

import com.danilian.speakide.notification.SpeakIdeNotifier
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project


class ResultDelivery(private val notifier: SpeakIdeNotifier = SpeakIdeNotifier()) {

    fun deliver(text: String, project: Project?, editor: Editor?) {
        if (text.isBlank()) {
            notifier.notifyNothingRecognized(project)
            return
        }
        if (project != null && editor != null) {
            ApplicationManager.getApplication().invokeLater {
                CaretTextDelivery.deliver(text, project, editor)
            }
        } else {
            ClipboardTextDelivery.deliver(text, project, editor)
            notifier.notifyClipboard(project, text)
        }
    }
}
