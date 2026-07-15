package org.jetbrains.bazel.languages.projectview.imports

import com.intellij.build.FilePosition
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.psi.PsiElement
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.projectview.imports.Import.Resolved
import org.jetbrains.bazel.languages.projectview.imports.Import.Unresolved
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiImport
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiImportBase

internal object ImportFactory {
  fun from(
    root: VirtualFile,
    element: ProjectViewPsiImportBase,
  ): Import {
    val importPathElement = element.getImportPath()
    val path = importPathElement?.text?.trim() ?: ""
    val file = root.findFileByRelativePath(path)
    val required = element is ProjectViewPsiImport
    return when (file) {
      null -> Unresolved(path, importPathElement?.filePositionOrNull(), required)
      else -> Resolved(file, required)
    }
  }

  private fun PsiElement.filePositionOrNull(): FilePosition? {
    val file = this.containingFile ?: return null
    val path = file.virtualFile?.toNioPathOrNull() ?: return null
    val text = file.text
    val start = StringUtil.offsetToLineColumn(text, textRange.startOffset) ?: return FilePosition(path, 0, 0)
    val end = StringUtil.offsetToLineColumn(text, textRange.endOffset) ?: return FilePosition(path, 0, 0)
    return FilePosition(path, start.line, start.column, end.line, end.column)
  }
}

@ApiStatus.Internal
fun Import.resolvedFileOrNull(): VirtualFile? = (this as? Import.Resolved)?.file
