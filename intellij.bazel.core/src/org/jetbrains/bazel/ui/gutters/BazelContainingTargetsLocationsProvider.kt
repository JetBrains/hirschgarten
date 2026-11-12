package org.jetbrains.bazel.ui.gutters

import com.intellij.execution.Location
import com.intellij.execution.PsiLocation
import com.intellij.execution.actions.MultipleRunLocationsProvider
import com.intellij.psi.PsiElement
import com.intellij.util.ThreeState
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.target.getTargetInfo
import org.jetbrains.bazel.target.targetStorage
import org.jetbrains.bsp.protocol.BuildTargetInfo

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
  private fun getTargets(element: PsiElement): List<BuildTargetInfo> {
    val targetUtils = element.project.targetStorage
    val containingFile = element.containingFile?.virtualFile ?: return emptyList()
    val normalTargets = targetUtils.getTargetsForFile(containingFile)
      .mapNotNull { targetUtils.getTargetInfo(it) }
    val executableTargets = targetUtils.getExecutableTargetsForFile(containingFile)
      .mapNotNull { targetUtils.getTargetInfo(it) }
    val normalLabels = normalTargets.mapTo(HashSet()) { it.id }
    val dependentTargets = executableTargets.filter { it.id !in normalLabels }
    return (normalTargets + filterDependentTargets(element, dependentTargets)).distinctBy { it.id }
  }

  private fun filterDependentTargets(element: PsiElement, targets: List<BuildTargetInfo>): List<BuildTargetInfo> {
    if (targets.size < 2) return targets
    val matches = BazelEntryPointMatcher.matchAll(element, targets)
    // in case element is not recognized as an entry point of any given targets, keep all targets
    if (matches.values.none { it == ThreeState.YES }) return targets
    return targets.filter {
      when (matches.getValue(it.id)) {
        ThreeState.YES, ThreeState.UNSURE -> true
        ThreeState.NO -> false
      }
    }
  }

  override fun getLocationDisplayName(
    locationCreatedFrom: Location<*>,
    originalLocation: Location<*>,
  ): String? = null
}
