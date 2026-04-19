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
    private val apiKeyField = JPasswordField(SecureStorage.getOpenAiKey() ?: "")
    private lateinit var providerCombo: ComboBox<SttProviderOption>

    override fun createPanel(): DialogPanel = panel {
        group("STT Provider Settings") {
            row("Provider:") {
                providerCombo = comboBox(SttProviderOption.entries)
                    .bindItem(
                        { SttProviderOption.entries.find { it.id == settings.sttProvider } ?: SttProviderOption.OPENAI_WHISPER },
                        { settings.sttProvider = it?.id ?: SttProviderOption.OPENAI_WHISPER.id }
                    ).component
            }
            row("Language:") {
                comboBox(listOf("auto", "en", "ru", "de", "fr", "es", "zh", "ja"))
                    .bindItem(
                        { settings.language },
                        { settings.language = it ?: "auto" }
                    )
                    .comment("Language hint for transcription. \"auto\" lets the provider detect it")
            }
            row("OpenAI API Key:") {
                cell(apiKeyField)
                    .comment("Your key is stored securely in the system keychain")
            }.visibleIf(object : ComponentPredicate() {
                override fun invoke() = providerCombo.selectedItem == SttProviderOption.OPENAI_WHISPER
                override fun addListener(listener: (Boolean) -> Unit) {
                    providerCombo.addActionListener { listener(invoke()) }
                }
            })
            row("Vosk model path:") {
                textFieldWithBrowseButton(
                    fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor(),
                    browseDialogTitle = "Select Vosk Model Folder"
                ).bindText(settings::voskModelPath)
                    .comment("Download a model from <a href=\"https://alphacephei.com/vosk/models\">alphacephei.com/vosk/models</a> and point here")
            }.visibleIf(object : ComponentPredicate() {
                override fun invoke() = providerCombo.selectedItem == SttProviderOption.VOSK
                override fun addListener(listener: (Boolean) -> Unit) {
                    providerCombo.addActionListener { listener(invoke()) }
                }
            })
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

        group("Notifications") {
            row {
                checkBox("Notify when AI assistant finishes responding")
                    .bindSelected(settings::notifyOnAiCompletion)
                    .comment("Experimental — sends a balloon when the AI Chat response completes")
            }
        }
    }

    override fun apply() {
        super.apply()
        SecureStorage.setOpenAiKey(String(apiKeyField.password))
    }

    override fun isModified(): Boolean {
        return super.isModified() || String(apiKeyField.password) != (SecureStorage.getOpenAiKey() ?: "")
    }

    override fun reset() {
        super.reset()
        apiKeyField.text = SecureStorage.getOpenAiKey() ?: ""
    }
}
