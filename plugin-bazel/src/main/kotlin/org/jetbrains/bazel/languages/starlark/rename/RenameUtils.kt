package org.jetbrains.bazel.languages.starlark.rename

import com.intellij.openapi.project.Project

object RenameUtils {
  fun createNewName(project: Project, name: String) = StarlarkElementGenerator(project).createNameIdentifier(name)
}
