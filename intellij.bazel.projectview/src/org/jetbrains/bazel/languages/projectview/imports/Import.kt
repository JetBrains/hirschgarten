package org.jetbrains.bazel.languages.projectview.imports

import com.intellij.build.FilePosition
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.projectview.imports.Import.Resolved
import org.jetbrains.bazel.languages.projectview.imports.Import.Unresolved
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiImport
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiImportBase
import java.nio.file.InvalidPathException
import java.nio.file.Path
import kotlin.io.path.isRegularFile

internal object ImportFactory {

  fun from(
    root: Path,
    element: ProjectViewPsiImportBase,
    sourcePath: Path?,
  ): Import {
    val importPathElement = element.getImportPath()
    val path = importPathElement?.text?.trim() ?: ""
    val file = root.resolveImport(path)
    val required = element is ProjectViewPsiImport
    return when (file) {
      null -> Unresolved(path, importPathElement?.filePositionOrNull(sourcePath), required)
      else -> Resolved(file, required)
    }
  }

  private fun Path.resolveImport(importPath: String): Path? {
    if (importPath.isEmpty()) return null
    val path = try {
      resolve(importPath)
    } catch (_: InvalidPathException) {
      return null
    }
    return path.normalize().takeIf { it.isRegularFile() }
  }

  private fun PsiElement.filePositionOrNull(path: Path?): FilePosition? {
    path ?: return null
    val text = containingFile?.text ?: return null
    val start = StringUtil.offsetToLineColumn(text, textRange.startOffset) ?: return FilePosition(path, 0, 0)
    val end = StringUtil.offsetToLineColumn(text, textRange.endOffset) ?: return FilePosition(path, 0, 0)
    return FilePosition(path, start.line, start.column, end.line, end.column)
  }
}

@ApiStatus.Internal
fun Import.resolvedPathOrNull(): Path? = (this as? Resolved)?.path
