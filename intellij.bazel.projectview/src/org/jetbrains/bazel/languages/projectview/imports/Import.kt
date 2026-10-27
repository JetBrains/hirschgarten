package org.jetbrains.bazel.languages.projectview.imports

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
  ): Import {
    val path = element.getImportPath()?.text?.trim() ?: ""
    val file = root.resolveImport(path)
    val required = element is ProjectViewPsiImport
    return when (file) {
      null -> Unresolved(path, required)
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
}
