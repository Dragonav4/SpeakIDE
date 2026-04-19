package com.danilian.speakide.settings

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe

object SecureStorage {

    private val credentialAttributes = CredentialAttributes(
        generateServiceName("SpeakIDE", "OpenAI_Whisper_API_KEY"),
    )

    fun setOpenAiKey(key: String?) {
        PasswordSafe.instance.setPassword(credentialAttributes, key)
    }

    fun getOpenAiKey(): String? {
        return PasswordSafe.instance.getPassword(credentialAttributes)
    }
}