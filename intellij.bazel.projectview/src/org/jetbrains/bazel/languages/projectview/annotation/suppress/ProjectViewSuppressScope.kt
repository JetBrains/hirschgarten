package org.jetbrains.bazel.languages.projectview.annotation.suppress

import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import com.intellij.psi.util.parentOfType
import com.intellij.psi.util.prevLeaf
import org.jetbrains.annotations.PropertyKey
import org.jetbrains.bazel.languages.projectview.BazelProjectViewBundle
import org.jetbrains.bazel.languages.projectview.lexer.ProjectViewTokenType
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSectionItem


internal enum class ProjectViewSuppressScope(
  @PropertyKey(resourceBundle = BazelProjectViewBundle.NAME) val actionKey: String,
  @PropertyKey(resourceBundle = BazelProjectViewBundle.NAME) val batchKey: String,
) {
  Item("suppress.for.item", "suppress.batch.for.item") {

    override fun containerOf(element: PsiElement): PsiElement? = element
      .parentOfType<ProjectViewPsiSectionItem>(withSelf = true)
      ?.takeIf { it.prevLeaf().elementType == ProjectViewTokenType.INDENT }
  },
  Section("suppress.for.section", "suppress.batch.for.section") {

    override fun containerOf(element: PsiElement): PsiElement? = element.parentOfType<ProjectViewPsiSection>(withSelf = true)
  };

  abstract fun containerOf(element: PsiElement): PsiElement?
}
