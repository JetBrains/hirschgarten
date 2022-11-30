package org.jetbrains.plugins.bsp.runConfig

import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope

public class BspConsoleView(project: Project, searchScope: GlobalSearchScope, viewer: Boolean, usePredefinedMessageFilter: Boolean) :
ConsoleViewImpl(project, searchScope, viewer, usePredefinedMessageFilter) {
  private var hasErrors = false

  // TODO: Taken from intellij-rust, figure out what it does and if it's needed
  override fun doCreateConsoleEditor(): EditorEx {
    val editor = super.doCreateConsoleEditor()
    editor.document.addDocumentListener(object : DocumentListener {
      override fun documentChanged(e: DocumentEvent) {
        if ("error" !in e.newFragment) return

        val document = e.document
        val startLine = document.getLineNumber(e.offset)
        val endLine = document.getLineNumber(e.offset + e.newLength)
        for (lineNumber in startLine..endLine) {
          val lineStart = document.getLineStartOffset(lineNumber)
          val lineEnd = document.getLineEndOffset(lineNumber)
          processLine(lineNumber, document.immutableCharSequence.subSequence(lineStart, lineEnd))
        }
      }

      private fun processLine(lineNumber: Int, line: CharSequence) {
        if (ERROR_RE.matches(line)) {
          if (!hasErrors) {
            getEditor().caretModel.moveToLogicalPosition(LogicalPosition(lineNumber - 1, 0))
            getEditor().scrollingModel.scrollToCaret(ScrollType.CENTER)
          }
          hasErrors = true
        }
      }
    })
    return editor
  }

  override fun scrollToEnd() {
    if (hasErrors) return
    super.scrollToEnd()
  }
}

private val ERROR_RE = """^\s*error\S*:.*""".toRegex()