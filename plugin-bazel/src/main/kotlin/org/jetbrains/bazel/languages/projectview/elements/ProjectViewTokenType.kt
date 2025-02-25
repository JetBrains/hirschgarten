package org.jetbrains.bazel.languages.projectview.elements

import com.intellij.psi.tree.IElementType
import org.jetbrains.bazel.languages.projectview.base.ProjectViewLanguage

/**
 * A token type for the project view language.
 *
 * See the Project Views documentation for more information.
 *
 * @param debugName the name of the element type, used for debugging purposes.
 */
class ProjectViewTokenType(debugName: String) : IElementType(debugName, ProjectViewLanguage) {
  override fun toString(): String = "ProjectView:" + super.toString()
}
