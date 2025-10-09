package org.jetbrains.bazel.languages.projectview.psi.sections

import com.intellij.lang.ASTNode
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewBaseElement
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType

abstract class ProjectViewPsiImportBase(node: ASTNode) : ProjectViewBaseElement(node) {
  fun getImportPath(): ProjectViewPsiImportItem? = getChildOfType<ProjectViewPsiImportItem>()

  abstract val isImportRequired: Boolean
}
