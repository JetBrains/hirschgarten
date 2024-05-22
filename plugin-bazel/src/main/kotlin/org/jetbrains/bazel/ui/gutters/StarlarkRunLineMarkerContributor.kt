package org.jetbrains.bazel.ui.gutters

import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkExpressionStatement
import org.jetbrains.plugins.bsp.config.isBspProject
import org.jetbrains.plugins.bsp.config.rootDir
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.plugins.bsp.target.temporaryTargetUtils
import org.jetbrains.plugins.bsp.ui.actions.target.BuildTargetAction
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils.fillWithEligibleActions
import java.net.URI
import kotlin.io.path.Path
import kotlin.io.path.toPath

internal class StarlarkRunLineMarkerContributor : RunLineMarkerContributor() {
  override fun getInfo(element: PsiElement): Info? {
    return if (element.project.isBspProject && element.shouldAddMarker())
      element.calculateMarkerInfo()
    else null
  }

  private fun PsiElement.shouldAddMarker(): Boolean =
    when (this) {
      is StarlarkCallExpression -> isTopLevelCall(this)
      else -> false
    }

  private fun isTopLevelCall(element: PsiElement): Boolean =
    element.parent is StarlarkExpressionStatement && element.parent.parent is StarlarkFile

  private fun PsiElement.calculateMarkerInfo(): Info? =
    containingFile.virtualFile?.url?.let { url ->
      val targetId = calculateTargetId(url) ?: return null

      val realTargetId = project.temporaryTargetUtils.allTargetIds().firstOrNull { it.uri.endsWith(targetId) }
      val targetInfo = realTargetId?.let { project.temporaryTargetUtils.getBuildTargetInfoForId(it) }
      calculateLineMarkerInfo(targetInfo).takeIf { it.actions.isNotEmpty() }
    }

  private fun PsiElement.calculateTargetId(buildFileUrl: String): String? {
    val targetName = getTargetName() ?: return null
    val projectBaseDirectory = Path(project.rootDir.path)
    val targetBaseDirectory = URI.create(buildFileUrl).toPath().parent
    val relativeTargetBaseDirectory = projectBaseDirectory.relativize(targetBaseDirectory)
    return "@//$relativeTargetBaseDirectory:$targetName"
  }

  private fun PsiElement.getTargetName(): String? {
    val visitor = StarlarkCallExpressionVisitor()
    this.accept(visitor)
    return visitor.identifier
  }

  private fun calculateLineMarkerInfo(
    targetInfo: BuildTargetInfo?,
  ): Info =
    Info(
      AllIcons.Actions.Execute,
      targetInfo.calculateEligibleActions().toTypedArray(),
    ) { "Run" }

  private fun BuildTargetInfo?.calculateEligibleActions(): List<AnAction> =
    if (this == null)
      emptyList()
    else DefaultActionGroup().fillWithEligibleActions(this, true).childActionsOrStubs.toList() +
      BuildTargetAction(this.id)
}

private class StarlarkCallExpressionVisitor : StarlarkElementVisitor() {
  var identifier: String? = null

  override fun visitCallExpression(node: StarlarkCallExpression) {
    identifier = node.getArgumentList()?.getNameArgumentValue()
  }
}
