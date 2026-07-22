package org.jetbrains.bazel.ui.gutters

import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.runnerAction.BazelRunnerActionDescriptor
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.ui.widgets.tool.window.utils.fillWithEligibleActions
import org.jetbrains.bsp.protocol.ExecutableTarget
import javax.swing.Icon

private class BazelRunLineMarkerInfo(
  text: String,
  icon: Icon?,
  actions: List<AnAction>,
  private val shouldReplaceOtherMarkers: Boolean,
) :
  RunLineMarkerContributor.Info(icon, actions.toTypedArray(), { text }) {
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

  protected val executeRunLineMarkerIcon: Icon // execute icon, provided here in order not to depend on AllIcons in other modules
    get() = AllIcons.Actions.Execute

  /**
   * Single method for override instead of several to avoid double computations for the same element
   */
  open fun getGutterAction(element: PsiElement): GutterAction? = null

  data class GutterAction(
    /**
     * `null` by default, meaning: only add our actions only in existing run gutters,
     * such as Java's JvmApplicationRunLineMarkerContributor or TestRunLineMarkerProvider
     */
    val icon: Icon? = null,
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
          icon = gutterAction.icon,
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
    val targetUtils = element.project.targetUtils
    val containingFile = element.containingFile?.virtualFile ?: return emptyList()
    val normalTargets = targetUtils.getTargetsForFile(containingFile)
      .mapNotNull { targetUtils.getBuildTargetForLabel(it) }
    val executableTargets = targetUtils.getExecutableTargetsForFile(containingFile)
      .mapNotNull { targetUtils.getBuildTargetForLabel(it) }
    return (normalTargets + executableTargets).distinctBy { it.id }
  }
}
