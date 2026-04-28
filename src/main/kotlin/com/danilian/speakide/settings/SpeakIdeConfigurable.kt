package com.danilian.speakide.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bindIntText
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.ComponentPredicate
import javax.swing.JPasswordField


class SpeakIdeConfigurable : BoundConfigurable("SpeakIDE") {

    private val settings = SpeakIdeSettings.getInstance().state
    private var loadedApiKey = ""
    private val apiKeyField = JPasswordField()

    init {
        com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread {
            val key = SecureStorage.getOpenAiKey() ?: ""
            loadedApiKey = key
            javax.swing.SwingUtilities.invokeLater {
                apiKeyField.text = key
            }
        }
    }

    private lateinit var providerCombo: ComboBox<SttProviderOption>

    override fun createPanel(): DialogPanel = panel {
        group("STT Provider Settings") {
            row("Provider:") {
                providerCombo = comboBox(SttProviderOption.entries)
                    .bindItem(
                        {
                            SttProviderOption.entries.find { it.id == settings.sttProvider }
                                ?: SttProviderOption.OPENAI_WHISPER
                        },
                        { settings.sttProvider = it?.id ?: SttProviderOption.OPENAI_WHISPER.id }
                    ).component
            }
            val isWhisperCloudPredicate = providerPredicate(SttProviderOption.OPENAI_WHISPER)
            val supportsLanguagePredicate =
                providerPredicate(SttProviderOption.OPENAI_WHISPER, SttProviderOption.WHISPER_LOCAL)

            row("Language:") {
                comboBox(SpeakIdeConstants.SUPPORTED_LANGUAGES)
                    .bindItem(
                        { settings.language },
                        { settings.language = it ?: "auto" }
                    )
                    .comment("Language hint for transcription. \"auto\" lets the provider detect it")
            }.visibleIf(supportsLanguagePredicate)

            row("API Key:") {
                cell(apiKeyField)
                    .comment("Stored securely. Required for Whisper API")
            }.visibleIf(isWhisperCloudPredicate)

            row("Base URL:") {
                textField()
                    .bindText(settings::whisperBaseUrl)
                    .comment("e.g. https://api.openai.com/v1 or https://api.groq.com/openai/v1")
            }.visibleIf(isWhisperCloudPredicate)

            row("Model Name:") {
                textField()
                    .bindText(settings::whisperModel)
                    .comment("e.g. whisper-1 (OpenAI) or whisper-large-v3-turbo (Groq)")
            }.visibleIf(isWhisperCloudPredicate)
            row("Vosk model path:") {
                textFieldWithBrowseButton(
                    fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
                        .withTitle("Select Vosk Model Folder")
                ).bindText(settings::voskModelPath)
                    .comment("Download a model from <a href=\"https://alphacephei.com/vosk/models\">alphacephei.com/vosk/models</a> and point here")
            }.visibleIf(providerPredicate(SttProviderOption.VOSK))

            row("Whisper local model (.bin):") {
                textFieldWithBrowseButton(
                    fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor()
                        .withTitle("Select Whisper ggml .bin Model")
                        .withExtensionFilter("bin", "bin")
                ).bindText(settings::whisperLocalModelPath)
                    .comment("Download a ggml model (e.g. ggml-base.en.bin) from huggingface and point here")
            }.visibleIf(providerPredicate(SttProviderOption.WHISPER_LOCAL))
        }

        group("Detection and UI") {
            row {
                checkBox("Enable silence detection")
                    .bindSelected(settings::silenceDetectionEnabled)
            }
            row("Silence threshold (ms):") {
                intTextField(0..10000)
                    .bindIntText(settings::silenceThresholdMs)
            }
            row {
                checkBox("Show recording overlay")
                    .bindSelected(settings::showRecordingOverlay)
            }
        }

    }

    override fun apply() {
        super.apply()
        val currentText = String(apiKeyField.password)
        if (currentText != loadedApiKey) {
            com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread {
                SecureStorage.setOpenAiKey(currentText)
            }
            loadedApiKey = currentText
        }
    }

    private fun providerPredicate(vararg options: SttProviderOption) = object : ComponentPredicate() {
        override fun invoke() = providerCombo.selectedItem in options
        override fun addListener(listener: (Boolean) -> Unit) {
            providerCombo.addActionListener { listener(invoke()) }
        }
    }

    override fun isModified(): Boolean {
        return super.isModified() || String(apiKeyField.password) != loadedApiKey
    }

    override fun reset() {
        super.reset()
        apiKeyField.text = loadedApiKey
    }
}
