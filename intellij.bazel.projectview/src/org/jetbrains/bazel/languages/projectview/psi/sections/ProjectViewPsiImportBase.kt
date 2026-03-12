package org.jetbrains.bazel.languages.projectview.psi.sections

import com.intellij.lang.ASTNode
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewBaseElement

internal abstract class ProjectViewPsiImportBase(node: ASTNode) : ProjectViewBaseElement(node) {
  fun getImportPath(): ProjectViewPsiImportItem? = PsiTreeUtil.getChildOfType(this, ProjectViewPsiImportItem::class.java)

  abstract val isImportRequired: Boolean
}
