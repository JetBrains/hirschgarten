package org.jetbrains.bazel.languages.projectview.psi.sections

import com.intellij.lang.ASTNode
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewBaseElement
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewElementVisitor

class ProjectViewScalarKey(node: ASTNode): ProjectViewBaseElement(node)  {
  override fun acceptVisitor(visitor: ProjectViewElementVisitor) {
    visitor.visitScalarKey(this)
  }
}
