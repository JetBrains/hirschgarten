package org.jetbrains.bazel.ui.gutters

import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.ui.widgets.tool.window.utils.fillWithEligibleActions
import org.jetbrains.bsp.protocol.BuildTarget
import javax.swing.Icon

private class BazelRunLineMarkerInfo(text: String, icon: Icon?, actions: List<AnAction>) :
  RunLineMarkerContributor.Info(icon, actions.toTypedArray(), { text }) {
  override fun shouldReplace(other: RunLineMarkerContributor.Info): Boolean = true
}

abstract class BazelRunLineMarkerContributor : RunLineMarkerContributor() {
  override fun getInfo(element: PsiElement): Info? =
    // gutter icons are only allowed to be added to leaf elements
    if (element is LeafPsiElement && element.project.isBazelProject && element.shouldAddMarker()) {
      element.calculateLineMarkerInfo()
    } else {
      null
    }

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

  private fun PsiElement.calculateLineMarkerInfo(): Info? =
    containingFile.virtualFile?.let { url ->
      val targetUtils = project.targetUtils
      val normalTargets = targetUtils.getTargetsForFile(url)
        .mapNotNull { targetUtils.getBuildTargetForLabel(it) }
      val executableTargets = targetUtils.getExecutableTargetsForFile(url)
        .mapNotNull { targetUtils.getBuildTargetForLabel(it) }
      val targets = (normalTargets + executableTargets)
        .distinctBy { it.id }
      calculateLineMarkerInfo(
        project = project,
        targetInfos = targets,
        hasTestTarget = targets.any { it.kind.ruleType == RuleType.TEST },
        testExecutableArguments = getExtraProgramArguments(this),
        psiElement = this,
      )
    }

  private fun calculateLineMarkerInfo(
    project: Project,
    targetInfos: List<BuildTarget>,
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
        )
      }
  }

  private fun BuildTarget?.calculateEligibleActions(
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
