package org.jetbrains.plugins.bsp.ui.gutters

import ch.epfl.scala.bsp4j.TextDocumentIdentifier
import com.goide.psi.GoFile
import com.goide.psi.impl.GoFunctionDeclarationImpl
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Separator
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNameIdentifierOwner
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.plugins.bsp.config.isBspProject
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.plugins.bsp.services.MagicMetaModelService
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils.fillWithEligibleActions

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
    (!isInsideJar() && getStrictParentOfType<PsiNameIdentifierOwner>()
      ?.isClassOrMethod() ?: false) || isGoTopLevelFunction()

  private fun PsiElement.isGoTopLevelFunction() =
      getStrictParentOfType<PsiNameIdentifierOwner>() is GoFunctionDeclarationImpl
        && getStrictParentOfType<PsiNameIdentifierOwner>()?.parent is GoFile

  private fun PsiElement.isInsideJar() =
    containingFile.virtualFile?.url?.startsWith("jar://") ?: false

  private fun PsiNameIdentifierOwner.isClassOrMethod(): Boolean =
    this is KtClassOrObject || this is KtNamedFunction || this is PsiClass || this is PsiMethod

  private fun PsiElement.calculateLineMarkerInfo(): Info? =
    containingFile.virtualFile?.url?.let { url ->
      val magicMetaModel = MagicMetaModelService.getInstance(project).value
      val documentId = TextDocumentIdentifier(url)
      val documentTargetDetails = magicMetaModel.getTargetsDetailsForDocument(documentId)
      val targetsMap = magicMetaModel.facade.targets
      val loadedTargetInfo = documentTargetDetails.loadedTargetId?.let { targetsMap[it] }
      val notLoadedTargetInfos = documentTargetDetails.notLoadedTargetsIds.mapNotNull { targetsMap[it] }
      calculateLineMarkerInfo(loadedTargetInfo, notLoadedTargetInfos)
    }

  private fun calculateLineMarkerInfo(
    loadedTargetInfo: BuildTargetInfo?,
    notLoadedTargetInfos: List<BuildTargetInfo>,
  ): Info =
    BspLineMakerInfo(
      text = "Run",
      actions = calculateEligibleActionsForTargets(loadedTargetInfo, notLoadedTargetInfos)
    )

  private fun calculateEligibleActionsForTargets(
    loadedTargetInfo: BuildTargetInfo?,
    notLoadedTargetInfos: List<BuildTargetInfo>,
  ) = loadedTargetInfo.calculateEligibleActions() +
    Separator() +
    notLoadedTargetInfos.flatMap { it.calculateEligibleActions() }

  private fun BuildTargetInfo?.calculateEligibleActions(): List<AnAction> =
    if (this == null) emptyList()
    else DefaultActionGroup().fillWithEligibleActions(this, true).childActionsOrStubs.toList()
}
