package org.jetbrains.bazel.languages.projectview.imports

import org.jetbrains.bazel.languages.projectview.imports.Import.OutsideOfProjectRoot
import org.jetbrains.bazel.languages.projectview.imports.Import.Resolved
import org.jetbrains.bazel.languages.projectview.imports.Import.Unresolved
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiImport
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiImportBase
import java.nio.file.InvalidPathException
import java.nio.file.Path
import kotlin.io.path.isRegularFile

internal object ImportFactory {

  /**
   * Resolves the import path of [element] against [root].
   * The normalized path must be inside [root], or the function returns [OutsideOfProjectRoot] and does not touch the file system.
   */
  fun from(
    root: Path,
    element: ProjectViewPsiImportBase,
  ): Import {
    val text = element.getImportPath()?.text?.trim() ?: ""
    val required = element is ProjectViewPsiImport
    if (text.isEmpty()) return Unresolved(text, required)
    val path = try {
      root.resolve(text).normalize()
    }
    catch (_: InvalidPathException) {
      return Unresolved(text, required)
    }
    return when {
      !path.startsWith(root.normalize()) -> OutsideOfProjectRoot(path, required)
      path.isRegularFile() -> Resolved(path, required)
      else -> Unresolved(text, required)
    }
  }
}
