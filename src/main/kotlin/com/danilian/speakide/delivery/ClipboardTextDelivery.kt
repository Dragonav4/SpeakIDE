package com.danilian.speakide.delivery

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

object ClipboardTextDelivery : TextDelivery {

    override fun deliver(text: String, project: Project?, editor: Editor?) {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
    }
}
