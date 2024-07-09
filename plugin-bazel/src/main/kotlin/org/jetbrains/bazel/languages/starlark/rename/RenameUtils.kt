package org.jetbrains.bazel.languages.starlark.rename

import com.intellij.lang.ASTNode
import com.intellij.openapi.project.Project

object RenameUtils {
  fun createNewName(project: Project, name: String): ASTNode =
    StarlarkElementGenerator(project).createNameIdentifier(name)
}
