package org.jetbrains.bazel.ui.gutters

import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkExpressionStatement
import org.jetbrains.bazel.languages.starlark.repomapping.calculateLabel
import org.jetbrains.bazel.runnerAction.BuildTargetAction
import org.jetbrains.bazel.sync.action.ResyncTargetAction
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.ui.widgets.tool.window.utils.fillWithEligibleActions
import org.jetbrains.bsp.protocol.BuildTarget

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
    this.project.isBazelProject &&
      this.elementType == StarlarkTokenTypes.IDENTIFIER &&
      grandParent is StarlarkCallExpression &&
      isTopLevelCall(
        grandParent,
      )

  private fun isTopLevelCall(element: PsiElement): Boolean =
    element.parent is StarlarkExpressionStatement && element.parent?.parent is StarlarkFile

  private fun PsiElement.calculateMarkerInfo(): Info? =
    containingFile.virtualFile?.let { virtualFile ->
      val targetName = getTargetName() ?: return null
      val path = virtualFile.toNioPathOrNull() ?: return null
      val targetLabel = calculateLabel(project, path, targetName)
      val targetInfo = targetLabel?.let { project.targetUtils.getBuildTargetForLabel(it) }
      calculateLineMarkerInfo(project, targetInfo).takeIf { it.actions.isNotEmpty() }
    }

  private fun PsiElement.getTargetName(): String? {
    val visitor = StarlarkCallExpressionVisitor()
    this.accept(visitor)
    return visitor.identifier
  }

  private fun calculateLineMarkerInfo(project: Project, targetInfo: BuildTarget?): Info {
    val actions = targetInfo.calculateEligibleActions(project).toTypedArray()
    val onlyBuild = actions.singleOrNull() is BuildTargetAction
    return Info(
      if (onlyBuild) AllIcons.Actions.Compile else AllIcons.Actions.Execute,
      actions,
    )
  }

  private fun BuildTarget?.calculateEligibleActions(project: Project): List<AnAction> =
    if (this == null) {
      emptyList()
    } else {
      listOfNotNull(ResyncTargetAction.createIfEnabled(project, id)) +
        DefaultActionGroup().fillWithEligibleActions(project, this, false).childActionsOrStubs.toList() +
        BuildTargetAction(this.id)
    }
}

private class StarlarkCallExpressionVisitor : StarlarkElementVisitor() {
  var identifier: String? = null

  override fun visitCallExpression(node: StarlarkCallExpression) {
    identifier = node.getArgumentList()?.getNameArgumentValue()
  }
}
