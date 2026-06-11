package org.jetbrains.bazel.ui.gutters

import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.config.isBazelProject
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
    if (element is LeafPsiElement && isProjectApplicable(element.project) && element.shouldAddMarker()) {
      element.calculateLineMarkerInfo()
    } else {
      null
    }

  @ApiStatus.Internal
  protected open fun isProjectApplicable(project: Project): Boolean = project.isBazelProject

  override fun getSlowInfo(element: PsiElement): Info? = null

  abstract fun PsiElement.shouldAddMarker(): Boolean

  open fun getSingleTestFilter(element: PsiElement): String? = null

  protected val executeRunLineMarkerIcon: Icon // execute icon, provided here in order not to depend on AllIcons in other modules
    get() = AllIcons.Actions.Execute

  open fun getRunLineMarkerIcon(element: PsiElement): Icon? = null

  /**
   * Allows adding additional test runner arguments.
   * Under the hood it uses `--testarg` bazel arguments
   */
  open fun getExtraProgramArguments(element: PsiElement): List<String> = emptyList()

  private fun PsiElement.calculateLineMarkerInfo(): Info? {
    val targets = getTargets(this)
    return calculateLineMarkerInfo(
      project = project,
      targetInfos = targets,
      hasTestTarget = targets.any { it.kind.ruleType == RuleType.TEST },
      testExecutableArguments = getExtraProgramArguments(this),
      psiElement = this,
    )
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

  private fun calculateLineMarkerInfo(
    project: Project,
    targetInfos: List<ExecutableTarget>,
    hasTestTarget: Boolean,
    testExecutableArguments: List<String>,
    psiElement: PsiElement,
  ): Info? {
    val singleTestFilter =
      if (hasTestTarget) getSingleTestFilter(psiElement) else null // no need to call the function if there are no tests among the targets
    return targetInfos
      .flatMap { it.calculateEligibleActions(project, singleTestFilter, testExecutableArguments, psiElement) }
      .takeIf { it.isNotEmpty() }
      ?.let {
        BazelRunLineMarkerInfo(
          text = "Run",
          icon = getRunLineMarkerIcon(psiElement),
          actions = it,
          shouldReplaceOtherMarkers = project.isBazelProject,
        )
      }
  }

  private fun ExecutableTarget?.calculateEligibleActions(
    project: Project,
    singleTestFilter: String?,
    testExecutableArguments: List<String>,
    psiElement: PsiElement,
  ): List<AnAction> =
    if (this == null) {
      emptyList()
    } else {
      DefaultActionGroup()
        .fillWithEligibleActions(
          project,
          this,
          singleTestFilter,
          testExecutableArguments,
          psiElement,
        ).childActionsOrStubs
        .toList()
    }
}
