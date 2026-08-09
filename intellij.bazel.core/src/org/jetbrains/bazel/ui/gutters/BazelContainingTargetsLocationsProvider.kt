package org.jetbrains.bazel.ui.gutters

import com.intellij.execution.Location
import com.intellij.execution.PsiLocation
import com.intellij.execution.actions.MultipleRunLocationsProvider
import com.intellij.psi.PsiElement
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.target.targetStorage
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.id

/**
 * Needed because [com.intellij.execution.actions.RunConfigurationProducer] can only produce one configuration per one location.
 */
@ApiStatus.Internal
class BazelContainingTargetsLocationsProvider : MultipleRunLocationsProvider() {
  override fun getAlternativeLocations(originalLocation: Location<*>): List<Location<*>> {
    if (!originalLocation.project.isBazelProject) return emptyList()
    if (originalLocation !is PsiLocation) return emptyList()
    val targets = getTargets(originalLocation.psiElement)
    if (targets.isEmpty()) return emptyList()
    return targets.map { target ->
      BazelRunLocation(target, originalLocation)
    } + listOf(originalLocation)
  }

  /**
   * This can also include non-executable targets (for synthetic run gutters)
   */
  private fun getTargets(element: PsiElement): List<BuildTarget> {
    val targetUtils = element.project.targetStorage
    val containingFile = element.containingFile?.virtualFile ?: return emptyList()
    val normalTargets = targetUtils.getTargetsForFile(containingFile)
      .mapNotNull { targetUtils.getTargetSummary(it) }
    val executableTargets = targetUtils.getExecutableTargetsForFile(containingFile)
      .mapNotNull { targetUtils.getTargetSummary(it) }
    return (normalTargets + executableTargets).distinctBy { it.id }
  }

  override fun getLocationDisplayName(
    locationCreatedFrom: Location<*>,
    originalLocation: Location<*>,
  ): String? = null
}
