package org.jetbrains.bazel.languages.projectview.psi

import com.intellij.psi.PsiElementVisitor
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiImport
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiImportItem
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSectionItem
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSectionName
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiTryImport

open class ProjectViewElementVisitor : PsiElementVisitor() {
  fun visitImport(node: ProjectViewPsiImport) {
    visitElement(node)
  }

  fun visitTryImport(node: ProjectViewPsiTryImport) {
    visitElement(node)
  }

  fun visitImportItem(node: ProjectViewPsiImportItem) {
    visitElement(node)
  }

  fun visitSection(node: ProjectViewPsiSection) {
    visitElement(node)
  }

  fun visitSectionItem(node: ProjectViewPsiSectionItem) {
    visitElement(node)
  }

  fun visitSectionName(node: ProjectViewPsiSectionName) {
    visitElement(node)
  }
}
