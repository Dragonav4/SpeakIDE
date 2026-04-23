package com.danilian.speakide.action

import com.danilian.speakide.AudioCapture
import com.danilian.speakide.settings.SpeakIdeSettings
import com.danilian.speakide.stt.SttProviderFactory
import com.danilian.speakide.textInsertion.CaretTextInsertion
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

class ToggleRecordingAction : AnAction(), DumbAware {

    private var capture: AudioCapture? = null
    private val pcmBuffer = mutableListOf<ByteArray>()
    private var isRecording = AtomicBoolean(false)
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project

        if (isRecording.compareAndSet(false, true)) {
            pcmBuffer.clear()
            println("=== SpeakIDE: Recording started ===")
            showNotification(project, "SpeakIDE", "Recording started...", NotificationType.INFORMATION)

            capture = AudioCapture(
                onData = { chunk -> pcmBuffer.add(chunk) },
                onError = { ex ->
                    println("=== SpeakIDE: Audio error: ${ex.message} ===")
                    showNotification(project, "SpeakIDE", "Audio error: ${ex.message}", NotificationType.ERROR)
                    stopRecording(project, editor = null)
                }
            ).also { it.start() }
        } else {
            // Capture the editor at the moment the user stops recording
            val editor = e.getData(CommonDataKeys.EDITOR)
            stopRecording(project, editor)
        }
    }

    private fun stopRecording(project: Project?, editor: Editor?) {
        if (!isRecording.compareAndSet(true, false)) return

        capture?.stop()
        capture = null

        val pcm = collectPcm()
        println("=== SpeakIDE: Recording stopped. ${pcm.size} bytes captured. ===")

        if (pcm.isEmpty()) {
            showNotification(project, "SpeakIDE", "Nothing recorded.", NotificationType.WARNING)
            return
        }

        showNotification(project, "SpeakIDE", "Recognizing...", NotificationType.INFORMATION)
        scope.launch { recognize(pcm, project, editor) }
    }

    private fun collectPcm(): ByteArray {
        val pcm = pcmBuffer.fold(ByteArray(0)) { acc, chunk -> acc + chunk }
        pcmBuffer.clear()
        return pcm
    }

    private suspend fun recognize(pcm: ByteArray, project: Project?, editor: Editor?) {
        try {
            val settings = SpeakIdeSettings.getInstance()
            val lang = settings.state.language.takeUnless { it == "auto" }
            val text = SttProviderFactory.create(settings).transcribe(pcm, lang).text.trim()
            println("=== SpeakIDE Recognized: $text ===")
            deliverResult(text, project, editor)
        } catch (ex: Throwable) {
            println("=== SpeakIDE: Recognition error -> ${ex::class.simpleName}: ${ex.message} ===")
            showNotification(project, "SpeakIDE: Error", "${ex::class.simpleName}: ${ex.message}", NotificationType.ERROR)
        }
    }

    private fun deliverResult(text: String, project: Project?, editor: Editor?) {
        if (text.isBlank()) {
            showNotification(project, "SpeakIDE", "Nothing recognized.", NotificationType.WARNING)
            return
        }
        if (project != null && editor != null) {
            // WriteCommandAction must run on the EDT
            ApplicationManager.getApplication().invokeLater {
                CaretTextInsertion().insertTextAtCaret(project, editor, text)
            }
        } else {
            showNotification(project, "SpeakIDE: Recognized", text, NotificationType.INFORMATION)
        }
    }

    private fun showNotification(project: Project?, title: String, content: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("SpeakIDE")
            .createNotification(title, content, type)
            .notify(project)
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = true
        e.presentation.text = if (isRecording.get()) "SpeakIDE: Stop Recording" else "SpeakIDE: Start Voice Recording"
    }
}
