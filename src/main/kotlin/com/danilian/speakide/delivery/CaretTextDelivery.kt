package com.danilian.speakide.delivery

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project


object CaretTextDelivery : TextDelivery {

    override fun deliver(text: String, project: Project?, editor: Editor?) {
        if (project == null || editor == null) return
        val document = editor.document
        val caretModel = editor.caretModel
        val offset = caretModel.offset

        WriteCommandAction.runWriteCommandAction(project) {
            if (document.isWritable) {
                document.insertString(offset, text)
                caretModel.moveToOffset(offset + text.length)
            }
        }
    }
}
