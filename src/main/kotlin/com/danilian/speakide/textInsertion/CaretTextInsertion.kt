package com.danilian.speakide.textInsertion

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project

class CaretTextInsertion {

    fun insertTextAtCaret(project: Project, editor: Editor, textToInsert: String) {
        val document = editor.document
        val caretModel = editor.caretModel
        val offset = caretModel.offset

        WriteCommandAction.runWriteCommandAction(project) {
            if (document.isWritable) {
                document.insertString(offset, textToInsert)
                caretModel.moveToOffset(offset + textToInsert.length)
            }
        }
    }
}