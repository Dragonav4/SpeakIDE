package com.danilian.speakide.delivery

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project


fun interface TextDelivery {
    fun deliver(text: String, project: Project?, editor: Editor?)
}
