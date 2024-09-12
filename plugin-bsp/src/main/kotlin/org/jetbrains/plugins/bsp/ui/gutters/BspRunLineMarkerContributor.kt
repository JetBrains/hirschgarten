package org.jetbrains.plugins.bsp.ui.gutters

import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.psi.PsiElement
import org.jetbrains.plugins.bsp.config.isBspProject
import org.jetbrains.plugins.bsp.impl.target.temporaryTargetUtils
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils.fillWithEligibleActions
import org.jetbrains.plugins.bsp.workspacemodel.entities.BuildTargetInfo

private class BspLineMakerInfo(text: String, actions: List<AnAction>) :
  RunLineMarkerContributor.Info(null, actions.toTypedArray(), { text }) {
  override fun shouldReplace(other: RunLineMarkerContributor.Info): Boolean = true
}

public abstract class BspRunLineMarkerContributor : RunLineMarkerContributor() {
  override fun getInfo(element: PsiElement): Info? = getSlowInfo(element)

  override fun getSlowInfo(element: PsiElement): Info? =
    if (element.project.isBspProject && element.shouldAddMarker()) {
      element.calculateLineMarkerInfo()
    } else {
      null
    }

  abstract fun PsiElement.shouldAddMarker(): Boolean

  private fun PsiElement.calculateLineMarkerInfo(): Info? =
    containingFile.virtualFile?.let { url ->
      val temporaryTargetUtils = project.temporaryTargetUtils
      val targetInfos =
        temporaryTargetUtils
          .getTargetsForFile(url, project)
          .mapNotNull { temporaryTargetUtils.getBuildTargetInfoForId(it) }
      calculateLineMarkerInfo(targetInfos)
    }

  private fun calculateLineMarkerInfo(targetInfos: List<BuildTargetInfo>): Info? =
    targetInfos
      .flatMap { it.calculateEligibleActions() }
      .takeIf { it.isNotEmpty() }
      ?.let {
        BspLineMakerInfo(
          text = "Run",
          actions = it,
        )
      }

  private fun BuildTargetInfo?.calculateEligibleActions(): List<AnAction> =
    if (this == null) {
      emptyList()
    } else {
      DefaultActionGroup().fillWithEligibleActions(this, true).childActionsOrStubs.toList()
    }
}
