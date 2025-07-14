package org.jetbrains.bazel.ui.gutters

import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.annotations.PublicApi
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.ui.widgets.tool.window.utils.fillWithEligibleActions
import org.jetbrains.bsp.protocol.BuildTarget

private class BazelRunLineMarkerInfo(text: String, actions: List<AnAction>) :
  RunLineMarkerContributor.Info(null, actions.toTypedArray(), { text }) {
  override fun shouldReplace(other: RunLineMarkerContributor.Info): Boolean = true
}

@PublicApi
abstract class BazelRunLineMarkerContributor : RunLineMarkerContributor() {
  override fun getInfo(element: PsiElement): Info? = getSlowInfo(element)

  override fun getSlowInfo(element: PsiElement): Info? =
    if (element.project.isBazelProject && element.shouldAddMarker()) {
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

  private fun PsiElement.calculateLineMarkerInfo(): Info? =
    containingFile.virtualFile?.let { url ->
      val targetUtils = project.targetUtils
      val targetInfos =
        targetUtils
          .getExecutableTargetsForFile(url)
          .mapNotNull { targetUtils.getBuildTargetForLabel(it) }
      calculateLineMarkerInfo(project, targetInfos, getSingleTestFilter(this),
                              getExtraProgramArguments(this), this)
    }

  private fun calculateLineMarkerInfo(
    project: Project,
    targetInfos: List<BuildTarget>,
    singleTestFilter: String?,
    testExecutableArguments: List<String>,
    psiElement: PsiElement,
  ): Info? =
    targetInfos
      .flatMap { it.calculateEligibleActions(project, singleTestFilter, testExecutableArguments, targetInfos.size > 1, psiElement) }
      .takeIf { it.isNotEmpty() }
      ?.let {
        BazelRunLineMarkerInfo(
          text = "Run",
          actions = it,
        )
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
}
