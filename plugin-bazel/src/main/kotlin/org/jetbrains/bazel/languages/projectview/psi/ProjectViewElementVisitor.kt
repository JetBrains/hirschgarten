package org.jetbrains.bazel.languages.projectview.psi

import com.intellij.psi.PsiElementVisitor
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewList
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewListKey
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewListValue
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewScalar
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewScalarKey
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewScalarValue

open class ProjectViewElementVisitor : PsiElementVisitor() {
  fun visitList(node: ProjectViewList) {
    visitElement(node)
  }

  fun visitListKey(node: ProjectViewListKey) {
    visitElement(node)
  }

  fun visitListValue(node: ProjectViewListValue) {
    visitElement(node)
  }

  fun visitScalar(node: ProjectViewScalar) {
    visitElement(node)
  }

  fun visitScalarKey(node: ProjectViewScalarKey) {
    visitElement(node)
  }

  fun visitScalarValue(node: ProjectViewScalarValue) {
    visitElement(node)
  }
}
