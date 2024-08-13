package org.jetbrains.bazel.languages.projectview.lexer

import com.intellij.psi.tree.IElementType
import org.jetbrains.bazel.languages.projectview.base.ProjectViewLanguage

class ProjectViewTokenType(debugName: String) : IElementType(debugName, ProjectViewLanguage) {
  override fun toString(): String = "ProjectView:" + super.toString()
}
