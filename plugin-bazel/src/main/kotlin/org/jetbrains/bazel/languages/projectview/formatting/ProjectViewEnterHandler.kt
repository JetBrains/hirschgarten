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
import org.jetbrains.bazel.languages.projectview.lexer.ProjectViewTokenType
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewFile

class ProjectViewEnterHandler : EnterHandlerDelegateAdapter() {
  fun isApplicable(file: PsiFile, dataContext: DataContext): Boolean {
    if (file !is ProjectViewFile) {
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
    if (element == null || element.text !== ":") {
      return false
    }
    val prev = element.node.treePrev
    if (prev == null) return false
    return prev.firstChildNode != null && prev.firstChildNode.elementType === ProjectViewTokenType.LIST_KEYWORD
  }

  override fun preprocessEnter(
    file: PsiFile,
    editor: Editor,
    caretOffset: Ref<Int?>,
    caretAdvance: Ref<Int?>,
    dataContext: DataContext,
    originalHandler: EditorActionHandler?,
  ): EnterHandlerDelegate.Result? {
    var offset = caretOffset.get()!!
    var file = file // now it's mutable
    var editor = editor

    if (editor is EditorWindow) {
      file = InjectedLanguageManager.getInstance(file.project).getTopLevelFile(file) as ProjectViewFile
      editor = InjectedLanguageEditorUtil.getTopLevelEditor(editor)
      offset = editor.caretModel.offset
    }

    if (!isApplicable(file, dataContext) || !insertIndent(file, offset)) {
      return EnterHandlerDelegate.Result.Continue
    }
    val indent = 2

    editor.caretModel.moveToOffset(offset)
    val doc = editor.document
    PsiDocumentManager.getInstance(file.project).commitDocument(doc)

    originalHandler?.execute(editor, editor.caretModel.currentCaret, dataContext)
    val position = editor.caretModel.logicalPosition
    if (position.column < indent) {
      val spacesPadding = StringUtil.repeat(" ", indent - position.column)
      doc.insertString(editor.caretModel.offset, spacesPadding)
    }
    editor.caretModel.moveToLogicalPosition(LogicalPosition(position.line, indent))
    return EnterHandlerDelegate.Result.Stop
  }
}
