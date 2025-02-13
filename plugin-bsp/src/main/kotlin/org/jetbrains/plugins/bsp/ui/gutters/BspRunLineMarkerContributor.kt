package org.jetbrains.plugins.bsp.ui.gutters

import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.plugins.bsp.config.isBspProject
import org.jetbrains.plugins.bsp.target.targetUtils
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils.fillWithEligibleActions
import org.jetbrains.plugins.bsp.workspacemodel.entities.BuildTargetInfo

private class BspLineMakerInfo(text: String, actions: List<AnAction>) :
  RunLineMarkerContributor.Info(null, actions.toTypedArray(), { text }) {
  override fun shouldReplace(other: RunLineMarkerContributor.Info): Boolean = true
}

abstract class BspRunLineMarkerContributor : RunLineMarkerContributor() {
  override fun getInfo(element: PsiElement): Info? = getSlowInfo(element)

  override fun getSlowInfo(element: PsiElement): Info? =
    if (element.project.isBspProject && element.shouldAddMarker()) {
      element.calculateLineMarkerInfo()
    } else {
      null
    }

  abstract fun PsiElement.shouldAddMarker(): Boolean

  open fun getSingleTestFilter(element: PsiElement): String? = null

  private fun PsiElement.calculateLineMarkerInfo(): Info? =
    containingFile.virtualFile?.let { url ->
      val temporaryTargetUtils = project.targetUtils
      val targetInfos =
        temporaryTargetUtils
          .getExecutableTargetsForFile(url)
          .mapNotNull { temporaryTargetUtils.getBuildTargetInfoForId(it) }
      calculateLineMarkerInfo(project, targetInfos, getSingleTestFilter(this))
    }

  private fun calculateLineMarkerInfo(
    project: Project,
    targetInfos: List<BuildTargetInfo>,
    singleTestFilter: String?,
  ): Info? =
    targetInfos
      .flatMap { it.calculateEligibleActions(project, singleTestFilter, targetInfos.size > 1) }
      .takeIf { it.isNotEmpty() }
      ?.let {
        BspLineMakerInfo(
          text = "Run",
          actions = it,
        )
      }

  private fun BuildTargetInfo?.calculateEligibleActions(
    project: Project,
    singleTestFilter: String?,
    verboseText: Boolean,
  ): List<AnAction> =
    if (this == null) {
      emptyList()
    } else {
      DefaultActionGroup().fillWithEligibleActions(project, this, verboseText, singleTestFilter).childActionsOrStubs.toList()
    }
}
