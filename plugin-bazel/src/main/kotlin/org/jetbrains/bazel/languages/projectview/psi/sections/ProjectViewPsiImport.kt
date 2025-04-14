package org.jetbrains.bazel.languages.projectview.psi.sections

import com.intellij.lang.ASTNode
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewBaseElement
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewElementVisitor

class ProjectViewPsiImport(node: ASTNode) : ProjectViewBaseElement(node) {
  fun getKeyword(): String = firstChild.text

  fun getPath(): String = children.last().text

  override fun acceptVisitor(visitor: ProjectViewElementVisitor) {
    visitor.visitImport(this)
  }
}
