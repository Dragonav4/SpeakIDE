package com.danilian.speakide.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(
    name = "com.danilian.speakide.SpeakIdeSettings",
    storages = [Storage("SpeakIdeSettings.xml")]
)
@Service(Service.Level.APP)
class SpeakIdeSettings : PersistentStateComponent<SpeakIdeSettings.State> {

    data class State(
        var sttProvider: String = SttProviderOption.OPENAI_WHISPER.id,
        var language: String = "auto",
        var silenceDetectionEnabled: Boolean = true,
        var silenceThresholdMs: Int = 2000,
        var voskModelPath: String = "",

        var whisperBaseUrl: String = "https://api.groq.com/openai/v1",

        var whisperModel: String = "whisper-large-v3",
        var whisperLocalModelPath: String = "",
        var showRecordingOverlay: Boolean = true,
    )

    private var _state = State()

    override fun getState(): State = _state

    override fun loadState(state: State) {
        _state = state
    }

    companion object {
        fun getInstance(): SpeakIdeSettings =
            ApplicationManager.getApplication().getService(SpeakIdeSettings::class.java)
    }
}

enum class SttProviderOption(val id: String, val displayName: String) {
    OPENAI_WHISPER("openai-whisper", "OpenAI Whisper (Cloud native)"),
    VOSK("vosk", "Vosk (Offline)"),
    WHISPER_LOCAL("whisper-local", "Whisper (Local)"),
}
