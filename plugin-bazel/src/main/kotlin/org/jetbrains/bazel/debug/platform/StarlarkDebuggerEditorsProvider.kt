package org.jetbrains.bazel.debug.platform

import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProviderBase
import org.jetbrains.bazel.languages.starlark.StarlarkFileType
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.StarlarkStatementList
import org.jetbrains.bazel.languages.starlark.psi.base.StarlarkElement

class StarlarkDebuggerEditorsProvider : XDebuggerEditorsProviderBase() {
  override fun getFileType(): FileType = StarlarkFileType

  override fun createExpressionCodeFragment(
    project: Project,
    text: String,
    context: PsiElement?,
    isPhysical: Boolean,
  ): PsiFile {
    val content = text.trim()
    return StarlarkExpressionCodeFragment(project, "fragment", content, context)
  }

  override fun getContextElement(file: VirtualFile, offset: Int, project: Project): PsiElement? =
    FileDocumentManager.getInstance().getDocument(file)?.let { document ->
      PsiDocumentManager.getInstance(project).getPsiFile(document)?.findInterestingElement(document, offset)
    }

  private fun PsiFile.findInterestingElement(document: Document, initialOffset: Int): PsiElement? {
    var coursor = initialOffset
    if (0 <= coursor && coursor < document.textLength) {
      val lineEndOffset = document.getLineEndOffset(document.getLineNumber(coursor))
      do {
        val element = this.findElementAt(coursor)
        if (element == null) {
          return null
        } else if (element !is PsiWhiteSpace && element !is PsiComment) {
          return element.getStatement()
        }
        coursor = element.textRange.endOffset
      } while (coursor < lineEndOffset)
    }
    return null
  }

  private fun PsiElement.getStatement(): PsiElement? =
    this.getTopLevelStatement()?.let { this.getAncestorRightBelow(it) }

  private fun PsiElement.getTopLevelStatement(): StarlarkElement? =
    if (this is StarlarkFile || this is StarlarkStatementList) {
      this as StarlarkElement
    } else {
      PsiTreeUtil.getParentOfType(this, StarlarkFile::class.java, StarlarkStatementList::class.java)
    }

  /** Returns element's ancestor, whose direct parent is given superParent (or `null` if nonexistent) */
  private fun PsiElement.getAncestorRightBelow(superParent: PsiElement): PsiElement? =
    PsiTreeUtil.findFirstParent(this, false) { it.parent == superParent }
}
