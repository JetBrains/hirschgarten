package org.jetbrains.bazel.ui.gutters

import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkExpressionStatement
import org.jetbrains.plugins.bsp.config.isBspProject
import org.jetbrains.plugins.bsp.config.rootDir
import org.jetbrains.plugins.bsp.runnerAction.BuildTargetAction
import org.jetbrains.plugins.bsp.sync.action.ResyncTargetAction
import org.jetbrains.plugins.bsp.target.targetUtils
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils.fillWithEligibleActions
import org.jetbrains.plugins.bsp.workspacemodel.entities.BuildTargetInfo

internal class StarlarkRunLineMarkerContributor : RunLineMarkerContributor() {
  override fun isDumbAware(): Boolean = true

  override fun getInfo(element: PsiElement): Info? {
    val grandParent = element.parent?.parent ?: return null
    return if (element.shouldAddMarker(grandParent)) {
      grandParent.calculateMarkerInfo()
    } else {
      null
    }
  }

  private fun PsiElement.shouldAddMarker(grandParent: PsiElement): Boolean =
    this.project.isBspProject &&
      this.elementType == StarlarkTokenTypes.IDENTIFIER &&
      grandParent is StarlarkCallExpression &&
      isTopLevelCall(
        grandParent,
      )

  private fun isTopLevelCall(element: PsiElement): Boolean =
    element.parent is StarlarkExpressionStatement && element.parent?.parent is StarlarkFile

  private fun PsiElement.calculateMarkerInfo(): Info? =
    containingFile.virtualFile?.let { virtualFile ->
      val targetId = calculateTargetId(virtualFile) ?: return null

      val realTargetId = project.targetUtils.allTargetIds().firstOrNull { it.uri.endsWith(targetId) }
      val targetInfo = realTargetId?.let { project.targetUtils.getBuildTargetInfoForId(it) }
      calculateLineMarkerInfo(project, targetInfo).takeIf { it.actions.isNotEmpty() }
    }

  private fun PsiElement.calculateTargetId(buildFile: VirtualFile): String? {
    val targetName = getTargetName() ?: return null
    val projectBaseDirectory = project.rootDir
    val targetBaseDirectory = buildFile.parent ?: return null
    val relativeTargetBaseDirectory = VfsUtilCore.getRelativePath(targetBaseDirectory, projectBaseDirectory) ?: return null

    return "@//$relativeTargetBaseDirectory:$targetName"
  }

  private fun PsiElement.getTargetName(): String? {
    val visitor = StarlarkCallExpressionVisitor()
    this.accept(visitor)
    return visitor.identifier
  }

  private fun calculateLineMarkerInfo(project: Project, targetInfo: BuildTargetInfo?): Info =
    Info(
      AllIcons.Actions.Execute,
      targetInfo.calculateEligibleActions(project).toTypedArray(),
    ) { "Run" }

  private fun BuildTargetInfo?.calculateEligibleActions(project: Project): List<AnAction> =
    if (this == null) {
      emptyList()
    } else {
      listOf(ResyncTargetAction(id)) + DefaultActionGroup().fillWithEligibleActions(project, this, false).childActionsOrStubs.toList() +
        BuildTargetAction(this.id)
    }
}

private class StarlarkCallExpressionVisitor : StarlarkElementVisitor() {
  var identifier: String? = null

  override fun visitCallExpression(node: StarlarkCallExpression) {
    identifier = node.getArgumentList()?.getNameArgumentValue()
  }
}
