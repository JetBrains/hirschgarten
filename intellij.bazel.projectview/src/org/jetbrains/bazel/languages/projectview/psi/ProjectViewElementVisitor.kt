package org.jetbrains.bazel.languages.projectview.psi

import com.intellij.psi.PsiElementVisitor
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiImport
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiImportItem
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSectionItem
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSectionName
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiTryImport

@ApiStatus.Internal
open class ProjectViewElementVisitor : PsiElementVisitor() {
  internal fun visitImport(node: ProjectViewPsiImport) {
    visitElement(node)
  }

  internal fun visitTryImport(node: ProjectViewPsiTryImport) {
    visitElement(node)
  }

  internal fun visitImportItem(node: ProjectViewPsiImportItem) {
    visitElement(node)
  }

  internal fun visitSection(node: ProjectViewPsiSection) {
    visitElement(node)
  }

  fun visitSectionItem(node: ProjectViewPsiSectionItem) {
    visitElement(node)
  }

  internal fun visitSectionName(node: ProjectViewPsiSectionName) {
    visitElement(node)
  }
}
