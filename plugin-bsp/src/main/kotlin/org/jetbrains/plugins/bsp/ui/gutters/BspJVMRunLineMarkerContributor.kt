package org.jetbrains.plugins.bsp.ui.gutters

import ch.epfl.scala.bsp4j.TextDocumentIdentifier
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNameIdentifierOwner
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.magicmetamodel.DocumentTargetsDetails
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetId
import org.jetbrains.plugins.bsp.assets.BuildToolAssetsExtension
import org.jetbrains.plugins.bsp.config.buildToolId
import org.jetbrains.plugins.bsp.config.isBspProject
import org.jetbrains.plugins.bsp.extension.points.withBuildToolIdOrDefault
import org.jetbrains.plugins.bsp.services.MagicMetaModelService
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions.RunTargetAction
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions.TestTargetAction

private class BspLineMakerInfo(text: String, actions: List<AnAction>) :
  RunLineMarkerContributor.Info(null, actions.toTypedArray(), { text }) {
  override fun shouldReplace(other: RunLineMarkerContributor.Info): Boolean = true
}

public class BspJVMRunLineMarkerContributor : RunLineMarkerContributor() {
  override fun getInfo(element: PsiElement): Info? = getSlowInfo(element)

  override fun getSlowInfo(element: PsiElement): Info? =
    if (element.project.isBspProject && element.shouldAddMarker()) element.calculateLineMarkerInfo()
    else null

  private fun PsiElement.shouldAddMarker(): Boolean =
    !isInsideJar() && getStrictParentOfType<PsiNameIdentifierOwner>()
      ?.takeIf { it.nameIdentifier == this }
      ?.isClassOrMethod() ?: false

  private fun PsiElement.isInsideJar() =
    containingFile.virtualFile?.url?.startsWith("jar://") ?: false

  private fun PsiNameIdentifierOwner.isClassOrMethod(): Boolean =
    this is KtClassOrObject || this is KtNamedFunction || this is PsiClass || this is PsiMethod

  private fun PsiElement.calculateLineMarkerInfo(): Info? =
    containingFile.virtualFile?.url?.let { url ->
      val magicMetaModel = MagicMetaModelService.getInstance(project).value
      val documentId = TextDocumentIdentifier(url)
      val documentTargetDetails = magicMetaModel.getTargetsDetailsForDocument(documentId)
      val assetsExtension = BuildToolAssetsExtension.ep.withBuildToolIdOrDefault(project.buildToolId)

      return if (isTest()) documentTargetDetails.calculateTestLineMarkerInfo(assetsExtension.presentableName)
      else documentTargetDetails.calculateRunLineMarkerInfo(assetsExtension.presentableName)
    }

  private fun PsiElement.isTest(): Boolean {
    val fileIndex = ProjectRootManager.getInstance(project).fileIndex

    return fileIndex.isInTestSourceContent(containingFile.virtualFile)
  }

  private fun DocumentTargetsDetails.calculateTestLineMarkerInfo(buildToolPresentableName: String): BspLineMakerInfo =
    BspLineMakerInfo(
      text = "Run Test",
      actions = this.calculateActions { toTestTargetAction(it, buildToolPresentableName) }
    )

  private fun toTestTargetAction(targetId: BuildTargetId, buildToolPresentableName: String): TestTargetAction =
    TestTargetAction(
      targetId = targetId,
      text = { "Run '$targetId' using $buildToolPresentableName" },
      icon = AllIcons.RunConfigurations.TestState.Run
    )

  private fun DocumentTargetsDetails.calculateRunLineMarkerInfo(buildToolPresentableName: String): BspLineMakerInfo =
    BspLineMakerInfo(
      text = "Run",
      actions = this.calculateActions { toRunTargetAction(it, buildToolPresentableName) }
    )

  private fun toRunTargetAction(targetId: BuildTargetId, buildToolPresentableName: String): RunTargetAction =
    RunTargetAction(
      targetId = targetId,
      text = { "Run '$targetId' using $buildToolPresentableName" },
      icon = AllIcons.RunConfigurations.TestState.Run
    )

  private fun DocumentTargetsDetails.calculateActions(mapping: (BuildTargetId) -> AnAction): List<AnAction> =
    listOfNotNull(
      loadedTargetId?.let { mapping(it) },
      Separator(),
    ) + notLoadedTargetsIds.map { mapping(it) }
}
