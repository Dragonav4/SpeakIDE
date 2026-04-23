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
import com.intellij.openapi.util.IconLoader
import kotlinx.coroutines.*
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.util.concurrent.atomic.AtomicBoolean

class ToggleRecordingAction : AnAction(), DumbAware {

    private var capture: AudioCapture? = null
    private val pcmBuffer = mutableListOf<ByteArray>()
    private var isRecording = AtomicBoolean(false)
    private val scope = CoroutineScope(Dispatchers.IO)

    // Captured on the EDT at recording start; used by both manual stop and silence auto-stop
    @Volatile private var activeProject: Project? = null
    @Volatile private var activeEditor: Editor? = null

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project

        if (isRecording.compareAndSet(false, true)) {
            // Capture editor now — AnActionEvent is valid only on the EDT during this call
            activeProject = project
            activeEditor = e.getData(CommonDataKeys.EDITOR)

            pcmBuffer.clear()
            println("=== SpeakIDE: Recording started ===")
//            showNotification(project, "SpeakIDE", "Recording started...", NotificationType.INFORMATION)

            capture = AudioCapture(
                onSilenceTimeout = {
                    println("=== SpeakIDE: Silence timeout — auto-stopping ===")
                    stopRecording(activeProject, activeEditor)
                },
                onData = { chunk -> pcmBuffer.add(chunk) },
                onError = { ex ->
                    println("=== SpeakIDE: Audio error: ${ex.message} ===")
                    showNotification(project, "SpeakIDE", "Audio error: ${ex.message}", NotificationType.ERROR)
                    stopRecording(project, editor = null)
                }
            ).also { it.start() }
        } else {
            stopRecording(project, e.getData(CommonDataKeys.EDITOR))
        }
    }

    private fun stopRecording(project: Project?, editor: Editor?) {
        if (!isRecording.compareAndSet(true, false)) return

        capture?.stop()
        capture = null
        activeProject = null
        activeEditor = null

        val pcm = collectPcm()
//        println("=== SpeakIDE: Recording stopped. ${pcm.size} bytes captured. ===")

        if (pcm.isEmpty()) {
            showNotification(project, "SpeakIDE", "Nothing recorded.", NotificationType.WARNING)
            return
        }

//        showNotification(project, "SpeakIDE", "Recognizing...", NotificationType.INFORMATION)
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
            copyToClipboard(text)
            showNotification(project, "SpeakIDE: Copied to clipboard", text, NotificationType.INFORMATION)
        }
    }

    private fun copyToClipboard(text: String) {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
    }

    private fun showNotification(project: Project?, title: String, content: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("SpeakIDE")
            .createNotification(title, content, type)
            .notify(project)
    }

    override fun update(e: AnActionEvent) {
        val recording = isRecording.get()
        e.presentation.isEnabledAndVisible = true
        e.presentation.text = if (recording) "SpeakIDE: Stop Recording" else "SpeakIDE: Start Voice Recording"
        e.presentation.icon = if (recording) ICON_RECORDING else ICON_IDLE
    }

    companion object {
        private val ICON_IDLE      = IconLoader.getIcon("/icons/microphone.svg",           ToggleRecordingAction::class.java)
        private val ICON_RECORDING = IconLoader.getIcon("/icons/microphone_recording.svg", ToggleRecordingAction::class.java)
    }
}
