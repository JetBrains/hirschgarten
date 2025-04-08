package org.jetbrains.bazel.ui.gutters

import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.ui.widgets.tool.window.utils.fillWithEligibleActions
import org.jetbrains.bsp.protocol.BuildTarget

private class BspLineMakerInfo(text: String, actions: List<AnAction>) :
  RunLineMarkerContributor.Info(null, actions.toTypedArray(), { text }) {
  override fun shouldReplace(other: RunLineMarkerContributor.Info): Boolean = true
}

abstract class BspRunLineMarkerContributor : RunLineMarkerContributor() {
  override fun getInfo(element: PsiElement): Info? = getSlowInfo(element)

  override fun getSlowInfo(element: PsiElement): Info? =
    if (element.project.isBazelProject && element.shouldAddMarker()) {
      element.calculateLineMarkerInfo()
    } else {
      null
    }

  abstract fun PsiElement.shouldAddMarker(): Boolean

  open fun getSingleTestFilter(element: PsiElement): String? = null

  private fun PsiElement.calculateLineMarkerInfo(): Info? =
    containingFile.virtualFile?.let { url ->
      val targetUtils = project.targetUtils
      val targetInfos =
        targetUtils
          .getExecutableTargetsForFile(url)
          .mapNotNull { targetUtils.getBuildTargetForLabel(it) }
      calculateLineMarkerInfo(project, targetInfos, getSingleTestFilter(this), this)
    }

  private fun calculateLineMarkerInfo(
    project: Project,
    targetInfos: List<BuildTarget>,
    singleTestFilter: String?,
    psiElement: PsiElement,
  ): Info? =
    targetInfos
      .flatMap { it.calculateEligibleActions(project, singleTestFilter, targetInfos.size > 1, psiElement) }
      .takeIf { it.isNotEmpty() }
      ?.let {
        BspLineMakerInfo(
          text = "Run",
          actions = it,
        )
      }

  private fun BuildTarget?.calculateEligibleActions(
    project: Project,
    singleTestFilter: String?,
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
          psiElement,
        ).childActionsOrStubs
        .toList()
    }
}
