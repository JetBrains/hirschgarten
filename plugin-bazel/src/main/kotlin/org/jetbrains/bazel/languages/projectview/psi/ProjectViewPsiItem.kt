package org.jetbrains.bazel.languages.projectview.psi

import com.intellij.lang.ASTNode

abstract class ProjectViewPsiItem(node: ASTNode) : ProjectViewBaseElement(node) {
  fun getKeyword(): String = (parent as ProjectViewPsiTopLevel).getKeyword()
}
