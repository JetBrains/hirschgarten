package org.jetbrains.bazel.languages.projectview.checker

import com.intellij.codeInspection.util.InspectionMessage
import com.intellij.psi.PsiElement
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
data class ProjectViewProblem(
  @InspectionMessage val message: String,
  val severity: Severity,
  val element: PsiElement? = null,
) {

  enum class Severity {
    Error, Warning
  }
}
