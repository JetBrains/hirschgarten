package org.jetbrains.bazel.ui.gutters

import com.intellij.execution.PsiLocation
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bsp.protocol.BuildTarget

@ApiStatus.Internal
class BazelRunLocation(
  val target: BuildTarget,
  /**
   * If provided, this can be, for example, a test method in PSI, from which a test filter can be derived.
   */
  val originalLocation: PsiLocation<*>,
) : PsiLocation<PsiElement>(originalLocation.project, null, PsiBazelTarget(originalLocation.project, target)) {
  /**
   * No other information except the target itself is available, e.g., from Bazel target tool window.
   */
  constructor(project: Project, target: BuildTarget) :
    this(target, PsiLocation(PsiBazelTarget(project, target)))
}
