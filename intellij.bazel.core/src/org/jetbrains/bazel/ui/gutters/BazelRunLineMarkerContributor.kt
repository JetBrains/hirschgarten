package org.jetbrains.bazel.ui.gutters

import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.runnerAction.BazelRunnerActionDescriptor
import org.jetbrains.bazel.target.targetStorage
import org.jetbrains.bazel.ui.widgets.tool.window.utils.fillWithEligibleActions
import org.jetbrains.bsp.protocol.ExecutableTarget

private class BazelRunLineMarkerInfo(
  text: String,
  actions: List<AnAction>,
  private val shouldReplaceOtherMarkers: Boolean,
) :
/**
 * [icon] here is `null`, meaning: only add our actions only in existing run gutters,
 * such as Java's JvmApplicationRunLineMarkerContributor or TestRunLineMarkerProvider
 */
  RunLineMarkerContributor.Info(/* icon */ null, actions.toTypedArray(), { text }) {
  override fun shouldReplace(other: RunLineMarkerContributor.Info): Boolean = shouldReplaceOtherMarkers
}

abstract class BazelRunLineMarkerContributor : RunLineMarkerContributor() {
  override fun getInfo(element: PsiElement): Info? =
    // gutter icons are only allowed to be added to leaf elements
    if (element is LeafPsiElement && isProjectApplicable(element.project)) {
      element.calculateLineMarkerInfo()
    } else {
      null
    }

  @ApiStatus.Internal
  protected open fun isProjectApplicable(project: Project): Boolean = project.isBazelProject

  override fun getSlowInfo(element: PsiElement): Info? = null

  /**
   * Single method for override instead of several to avoid double computations for the same element
   */
  open fun getGutterAction(element: PsiElement): GutterAction? = null

  data class GutterAction(
    val runnerActionDescriptor: BazelRunnerActionDescriptor = BazelRunnerActionDescriptor(),
  )

  private fun PsiElement.calculateLineMarkerInfo(): Info? {
    val gutterAction = getGutterAction(this) ?: return null
    return getTargets(this)
      .flatMap { it.calculateEligibleActions(project, gutterAction.runnerActionDescriptor, this) }
      .takeIf { it.isNotEmpty() }
      ?.let {
        BazelRunLineMarkerInfo(
          text = "Run",
          actions = it,
          shouldReplaceOtherMarkers = project.isBazelProject,
        )
      }
  }

  private fun ExecutableTarget?.calculateEligibleActions(
    project: Project,
    runnerActionDescriptor: BazelRunnerActionDescriptor,
    psiElement: PsiElement,
  ): List<AnAction> =
    if (this == null) {
      emptyList()
    } else {
      DefaultActionGroup()
        .fillWithEligibleActions(
          project,
          this,
          runnerActionDescriptor,
          psiElement,
        ).childActionsOrStubs
        .toList()
    }

  @ApiStatus.Internal
  open fun getTargets(element: PsiElement): List<ExecutableTarget> {
    val targetUtils = element.project.targetStorage
    val containingFile = element.containingFile?.virtualFile ?: return emptyList()
    val normalTargets = targetUtils.getTargetsForFile(containingFile)
      .mapNotNull { targetUtils.getTargetSummary(it) }
    val executableTargets = targetUtils.getExecutableTargetsForFile(containingFile)
      .mapNotNull { targetUtils.getTargetSummary(it) }
    return (normalTargets + executableTargets).distinctBy { it.id }
  }
}
