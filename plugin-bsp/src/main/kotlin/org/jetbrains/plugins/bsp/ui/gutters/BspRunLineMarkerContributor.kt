package org.jetbrains.plugins.bsp.ui.gutters

import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtNamedFunction
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
      val singleTestFilter = if (this.parent is KtNamedFunction) (this.parent as KtNamedFunction).name else null
      calculateLineMarkerInfo(targetInfos, singleTestFilter)
    }

  private fun calculateLineMarkerInfo(targetInfos: List<BuildTargetInfo>, singleTestFilter: String?): Info? =
    targetInfos
      .flatMap { it.calculateEligibleActions(singleTestFilter) }
      .takeIf { it.isNotEmpty() }
      ?.let {
        BspLineMakerInfo(
          text = "Run",
          actions = it,
        )
      }

  private fun BuildTargetInfo?.calculateEligibleActions(singleTestFilter: String?): List<AnAction> =
    if (this == null) {
      emptyList()
    } else {
      DefaultActionGroup().fillWithEligibleActions(this, false, singleTestFilter).childActionsOrStubs.toList()
    }
}
