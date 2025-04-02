package org.jetbrains.bazel.languages.projectview.formatting

import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegateAdapter
import com.intellij.ide.DataManager
import com.intellij.injected.editor.EditorWindow
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.editor.actions.SplitLineAction
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageEditorUtil
import com.intellij.psi.util.elementType
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

  fun isBlankLine(file: PsiFile, caretOffset: Int): Boolean {
    val start = file.findElementAt(caretOffset - 1)
    var left = start
    while (left != null && left is PsiWhiteSpace) {
      left = left.prevSibling
    }

    var right = file.findElementAt(caretOffset)
    while (right != null && right is PsiWhiteSpace) {
      right = right.nextSibling
    }

    return (left == null || left.elementType == ProjectViewTokenType.NEWLINE) &&
      (right == null || right.elementType == ProjectViewTokenType.NEWLINE)
  }

  override fun preprocessEnter(
    file: PsiFile,
    editor: Editor,
    caretOffset: Ref<Int?>,
    caretAdvance: Ref<Int?>,
    dataContext: DataContext,
    originalHandler: EditorActionHandler?,
  ): EnterHandlerDelegate.Result? {
    var offset = caretOffset.get() ?: return EnterHandlerDelegate.Result.Continue
    var file = file // now it's mutable
    var editor = editor

    if (editor is EditorWindow) {
      file = InjectedLanguageManager.getInstance(file.project).getTopLevelFile(file) as ProjectViewPsiFile
      editor = InjectedLanguageEditorUtil.getTopLevelEditor(editor)
      offset = editor.caretModel.offset
    }

    if (!isApplicable(file, dataContext)) {
      return EnterHandlerDelegate.Result.Default
    }
    // remove indent if enter is pressed on the blank line
    if (isBlankLine(file, offset)) {
      editor.document.insertString(offset, "\n")
      editor.caretModel.moveToOffset(offset + 1)
      return EnterHandlerDelegate.Result.Stop
    }
    if (!insertIndent(file, offset)) {
      return EnterHandlerDelegate.Result.Default
    }

    editor.caretModel.moveToOffset(offset)
    val doc = editor.document
    PsiDocumentManager.getInstance(file.project).commitDocument(doc)

    originalHandler?.execute(editor, editor.caretModel.currentCaret, dataContext)
    val position = editor.caretModel.logicalPosition
    if (position.column < INDENT) {
      val spacesPadding = StringUtil.repeat(" ", INDENT - position.column)
      doc.insertString(editor.caretModel.offset, spacesPadding)
    }
    editor.caretModel.moveToLogicalPosition(LogicalPosition(position.line, INDENT))
    return EnterHandlerDelegate.Result.Stop
  }
}
