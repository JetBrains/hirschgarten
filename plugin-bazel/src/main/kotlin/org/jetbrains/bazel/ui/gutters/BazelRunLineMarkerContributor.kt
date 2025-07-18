package org.jetbrains.bazel.ui.gutters

import com.intellij.execution.TestStateStorage
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.bazel.annotations.PublicApi
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.ui.widgets.tool.window.utils.fillWithEligibleActions
import org.jetbrains.bsp.protocol.BuildTarget
import javax.swing.Icon

private class BazelRunLineMarkerInfo(
  text: String,
  actions: List<AnAction>,
  icon: Icon?,
) : RunLineMarkerContributor.Info(icon, actions.toTypedArray(), { text }) {
  override fun shouldReplace(other: RunLineMarkerContributor.Info): Boolean = true
}

@PublicApi
abstract class BazelRunLineMarkerContributor : RunLineMarkerContributor() {
  override fun getInfo(element: PsiElement): Info? =
    // gutter icons are only allowed to be added to leaf elements
    if (element is LeafPsiElement && element.project.isBazelProject && element.shouldAddMarker()) {
      element.calculateLineMarkerInfo()
    } else {
      null
    }

  abstract fun PsiElement.shouldAddMarker(): Boolean

  open fun getSingleTestFilter(element: PsiElement): String? = null

  /**
   * Allows adding additional test runner arguments.
   * Under the hood it uses `--testarg` bazel arguments
   */
  open fun getExtraProgramArguments(element: PsiElement): List<String> = emptyList()

  open fun guessTestLocationHints(psiElement: PsiElement): List<String> = emptyList()

  open fun isTestSuite(psiElement: PsiElement): Boolean = false

  private fun PsiElement.calculateLineMarkerInfo(): Info? =
    containingFile.virtualFile?.let { url ->
      val targetUtils = project.targetUtils
      val targetInfos =
        targetUtils
          .getExecutableTargetsForFile(url)
          .mapNotNull { targetUtils.getBuildTargetForLabel(it) }
      val hasTestTarget = targetInfos.any { it.kind.ruleType == RuleType.TEST }
      calculateLineMarkerInfo(
        project,
        targetInfos,
        hasTestTarget,
        getExtraProgramArguments(this),
        this,
      )
    }

  private fun calculateLineMarkerInfo(
    project: Project,
    targetInfos: List<BuildTarget>,
    hasTestTarget: Boolean,
    testExecutableArguments: List<String>,
    psiElement: PsiElement,
  ): Info? {
    val icon = if (hasTestTarget) getTestStateIcon(psiElement) else null
    val singleTestFilter =
      if (hasTestTarget) getSingleTestFilter(psiElement) else null // no need to call the function if there are no tests among the targets
    return targetInfos
      .flatMap { it.calculateEligibleActions(project, singleTestFilter, testExecutableArguments, targetInfos.size > 1, psiElement) }
      .takeIf { it.isNotEmpty() }
      ?.let {
        BazelRunLineMarkerInfo(
          text = "Run",
          actions = it,
          icon = icon,
        )
      }
  }

  private fun BuildTarget?.calculateEligibleActions(
    project: Project,
    singleTestFilter: String?,
    testExecutableArguments: List<String>,
    includeTargetNameInText: Boolean,
    psiElement: PsiElement,
  ): List<AnAction> =
    if (this == null) {
      emptyList()
    } else {
      DefaultActionGroup()
        .fillWithEligibleActions(
          project,
          this,
          includeTargetNameInText,
          singleTestFilter,
          testExecutableArguments,
          psiElement,
        ).childActionsOrStubs
        .toList()
    }

  private fun getTestStateIcon(psiElement: PsiElement): Icon? {
    val testStateStorage = TestStateStorage.getInstance(psiElement.project)
    val testStateRecords =
      guessTestLocationHints(psiElement).mapNotNull { testStateStorage.getState(it) }
    val newestRecord =
      when (testStateRecords.size) {
        0 -> return null
        1 -> testStateRecords.single()
        else -> testStateRecords.maxBy { it.date } // take the newest test result
      }
    return getTestStateIcon(newestRecord, isTestSuite(psiElement))
  }
}
