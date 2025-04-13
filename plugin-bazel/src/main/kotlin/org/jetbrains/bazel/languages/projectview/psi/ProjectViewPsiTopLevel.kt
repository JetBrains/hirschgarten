package org.jetbrains.bazel.languages.projectview.psi

import com.intellij.lang.ASTNode

abstract class ProjectViewPsiTopLevel(node : ASTNode) : ProjectViewBaseElement(node) {
  fun getKeyword(): String = firstChild.text

  fun isEmpty(): Boolean = children.lastOrNull()?.firstChild == null
}
