package org.jetbrains.bazel.languages.projectview.checker

import com.intellij.codeInspection.util.InspectionMessage
import com.intellij.psi.PsiElement
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.projectview.ProjectViewSectionKey

@ApiStatus.Internal
data class ProjectViewProblem(
  @InspectionMessage val message: String,
  val severity: Severity,
  val type: Type = Type.Message,
  val quickFixes: List<QuickFix> = emptyList(),
  val element: PsiElement? = null,
) {

  enum class Severity {
    Error, Warning, WeakWarning
  }

  enum class Type {
    Message, Deprecation
  }

  /**
   * Quick fix descriptor which will be turned into appropriate action.
   */
  sealed interface QuickFix {

    /**
     * Merge the deprecated section values to the section with [targetSectionKey]. If target section does not exist, it will be created.
     *
     * Can only be applied to section PSI element.
     */
    data class MergeIntoSection(val targetSectionKey: ProjectViewSectionKey<*>) : QuickFix
  }
}
