package org.jetbrains.bazel.languages.projectview.formatting

import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegateAdapter
import com.intellij.ide.DataManager
import com.intellij.injected.editor.EditorWindow
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.editor.actions.SplitLineAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageEditorUtil
import org.jetbrains.bazel.languages.projectview.lexer.ProjectViewTokenType
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewPsiFile

class ProjectViewEnterHandler : EnterHandlerDelegateAdapter() {
  companion object {
    const val INDENT = 2
  }

  fun isApplicable(file: PsiFile, dataContext: DataContext): Boolean {
    if (file !is ProjectViewPsiFile) {
      return false
    }
    val isSplitLine = DataManager.getInstance().loadFromDataContext(dataContext, SplitLineAction.SPLIT_LINE_KEY)
    return isSplitLine == null
  }

  fun insertIndent(file: PsiFile, offset: Int): Boolean {
    if (offset == 0) {
      return false
    }

    var element = file.findElementAt(offset - 1)
    while (element != null && element is PsiWhiteSpace) {
      element = element.prevSibling
    }
    if (element == null || element.node.elementType != ProjectViewTokenType.COLON) {
      return false
    }
    val prev = element.node.treePrev
    return prev?.elementType == ProjectViewTokenType.SECTION_KEYWORD
  }

  fun isBlankLine(caretOffset: Int, document: Document): Boolean {
    val lineNumber = document.getLineNumber(caretOffset)

    val lineStart = document.getLineStartOffset(lineNumber)
    val lineEnd = document.getLineEndOffset(lineNumber)

    val textInLine = document.getText(TextRange(lineStart, lineEnd)).trim()

    return textInLine.isEmpty()
  }

  override fun preprocessEnter(
    file: PsiFile,
    editor: Editor,
    caretOffset: Ref<Int>,
    caretAdvance: Ref<Int>,
    dataContext: DataContext,
    originalHandler: EditorActionHandler?,
  ): EnterHandlerDelegate.Result {
    var offset = caretOffset.get()
    var file = file // now it's mutable
    var editor = editor

    if (editor is EditorWindow) {
      file = InjectedLanguageManager.getInstance(file.project).getTopLevelFile(file)
      editor = InjectedLanguageEditorUtil.getTopLevelEditor(editor)
      offset = editor.caretModel.offset
    }

    if (!isApplicable(file, dataContext)) {
      return EnterHandlerDelegate.Result.Continue
    }
    val document = editor.document
    PsiDocumentManager.getInstance(file.project).commitDocument(document)
    editor.caretModel.moveToOffset(offset)

    // remove indent if enter is pressed on the blank line
    if (isBlankLine(offset, document)) {
      val lineSeparator = FileDocumentManager.getInstance().getLineSeparator(file.virtualFile, file.project)
      editor.document.insertString(offset, lineSeparator)
      editor.caretModel.moveToOffset(offset + lineSeparator.length)
      return EnterHandlerDelegate.Result.Stop
    }
    if (!insertIndent(file, offset)) {
      return EnterHandlerDelegate.Result.Continue
    }

    originalHandler?.execute(editor, editor.caretModel.currentCaret, dataContext)
    val position = editor.caretModel.logicalPosition
    if (position.column < INDENT) {
      val spacesPadding = StringUtil.repeat(" ", INDENT - position.column)
      document.insertString(editor.caretModel.offset, spacesPadding)
    }
    editor.caretModel.moveToLogicalPosition(LogicalPosition(position.line, INDENT))
    return EnterHandlerDelegate.Result.Stop
  }
}
