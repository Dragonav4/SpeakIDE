package com.danilian.speakide.notification

import com.danilian.speakide.settings.SpeakIdeConstants
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

class SpeakIdeNotifier {

    fun notify(project: Project?, title: String, content: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(SpeakIdeConstants.NOTIFICATION_GROUP_ID)
            .createNotification(title, content, type)
            .notify(project)
    }

    fun notifyMicDenied(project: Project?) = notify(
        project,
        title = "Microphone access denied",
        content = "SpeakIDE cannot open the microphone. " +
            "Go to System Settings → Privacy & Security → Microphone, " +
            "enable access for IntelliJ IDEA, then restart the IDE.",
        type = NotificationType.ERROR
    )

    fun notifyNothingRecorded(project: Project?) = notify(
        project,
        title = "SpeakIDE",
        content = "Nothing recorded.",
        type = NotificationType.WARNING
    )

    fun notifyNothingRecognized(project: Project?) = notify(
        project,
        title = "SpeakIDE",
        content = "Nothing recognized.",
        type = NotificationType.WARNING
    )

    fun notifyAudioError(project: Project?, message: String?) = notify(
        project,
        title = "SpeakIDE",
        content = "Audio error: $message",
        type = NotificationType.ERROR
    )

    fun notifyRecognitionError(project: Project?, cause: Throwable) = notify(
        project,
        title = "SpeakIDE: Error",
        content = "${cause::class.simpleName}: ${cause.message}",
        type = NotificationType.ERROR
    )

    fun notifyClipboard(project: Project?, text: String) = notify(
        project,
        title = "SpeakIDE: Copied to clipboard",
        content = text,
        type = NotificationType.INFORMATION
    )
}
