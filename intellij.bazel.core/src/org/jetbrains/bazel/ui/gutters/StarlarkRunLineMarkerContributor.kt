package org.jetbrains.bazel.ui.gutters

import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.languages.starlark.bazel.BazelFileType
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkExpressionStatement
import org.jetbrains.bazel.languages.starlark.repomapping.calculateLabel
import org.jetbrains.bazel.runnerAction.BuildTargetAction
import org.jetbrains.bazel.sync.action.ResyncTargetAction
import org.jetbrains.bazel.sync.workspace.targetKind.TargetKindService
import org.jetbrains.bazel.target.targetStorage

@ApiStatus.Internal
open class StarlarkRunLineMarkerContributor : RunLineMarkerContributor() {
  override fun isDumbAware(): Boolean = true

  protected open fun isProjectApplicable(project: Project): Boolean =
    project.isBazelProject

  override fun getInfo(element: PsiElement): Info? {
    if (!isProjectApplicable(element.project)) return null
    val grandParent = element.parent?.parent ?: return null
    return if (element.shouldAddMarker(grandParent)) {
      grandParent.calculateMarkerInfo()
    }
    else {
      null
    }
  }

  private fun PsiElement.shouldAddMarker(grandParent: PsiElement): Boolean =
    this.elementType == StarlarkTokenTypes.IDENTIFIER &&
    grandParent is StarlarkCallExpression &&
    isTopLevelCall(
      grandParent,
    )

  private fun isTopLevelCall(element: PsiElement): Boolean =
    element.parent is StarlarkExpressionStatement && element.parent?.parent is StarlarkFile

  private fun PsiElement.calculateMarkerInfo(): Info? {
    val virtualFile = containingFile.virtualFile ?: return null
    if (BazelFileType.ofFileName(virtualFile.name) != BazelFileType.BUILD) return null
    val visitor = StarlarkCallExpressionVisitor()
    this.accept(visitor)
    val ruleName = visitor.ruleName ?: return null
    val targetName = visitor.targetName ?: return null
    val targetLabel = calculateLabel(project, virtualFile, targetName) ?: return null
    return calculateLineMarkerInfo(project, virtualFile, targetLabel, ruleName).takeIf { it.actions.isNotEmpty() }
  }

  private fun calculateLineMarkerInfo(project: Project, buildFile: VirtualFile, targetLabel: ResolvedLabel, ruleName: String): Info {
    val actions = calculateEligibleActions(project, buildFile, targetLabel, ruleName).toTypedArray()
    val onlyBuild = actions.singleOrNull() is BuildTargetAction
    return Info(
      if (onlyBuild) AllIcons.Actions.Compile else AllIcons.Actions.Execute,
      actions,
    )
  }

  private fun calculateEligibleActions(project: Project, buildFile: VirtualFile, targetLabel: ResolvedLabel, ruleName: String): List<AnAction> = buildList {
    val targetUtils = project.targetStorage
    val targetInfo = targetUtils.getTargetSummary(targetLabel)
    val targetKind = targetInfo?.kind ?: TargetKindService.getInstance().guessFromRuleName(ruleName)

    add(BuildTargetAction(targetLabel))
    targetInfo?.let {
      ResyncTargetAction.createIfEnabled(targetLabel)?.let { add(it) }
    }

    if (targetKind.isExecutable) {
      val executableTarget = targetInfo ?: NonImportedBuildTarget(targetLabel, targetKind, (buildFile.parent ?: buildFile).toNioPath())
      addAll(getExecutorActions(project, executableTarget))
    }
  }
}

private class StarlarkCallExpressionVisitor : StarlarkElementVisitor() {
  var ruleName: String? = null
  var targetName: String? = null

  override fun visitCallExpression(node: StarlarkCallExpression) {
    ruleName = node.getCalledFunctionName()
    targetName = node.getNameAttributeValue()
  }
}
