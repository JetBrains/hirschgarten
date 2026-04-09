package org.jetbrains.bazel.ui.gutters

import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import org.jetbrains.bazel.commons.RuleType
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
import org.jetbrains.bazel.runnerAction.RunWithCoverageAction
import org.jetbrains.bazel.runnerAction.TestTargetAction
import org.jetbrains.bazel.sync.action.ResyncTargetAction
import org.jetbrains.bazel.sync.workspace.targetKind.TargetKindService
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.ui.widgets.tool.window.utils.fillWithEligibleActions

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

  private fun PsiElement.calculateMarkerInfo(): Info? {
    val virtualFile = containingFile.virtualFile ?: return null
    if (BazelFileType.ofFileName(virtualFile.name) != BazelFileType.BUILD) return null
    val visitor = StarlarkCallExpressionVisitor()
    this.accept(visitor)
    val ruleName = visitor.ruleName ?: return null
    val targetName = visitor.targetName ?: return null
    val path = virtualFile.toNioPathOrNull() ?: return null
    val targetLabel = calculateLabel(project, path, targetName) ?: return null
    return calculateLineMarkerInfo(project, targetLabel, ruleName).takeIf { it.actions.isNotEmpty() }
  }

  private fun calculateLineMarkerInfo(project: Project, targetLabel: ResolvedLabel, ruleName: String): Info {
    val actions = calculateEligibleActions(project, targetLabel, ruleName).toTypedArray()
    val onlyBuild = actions.singleOrNull() is BuildTargetAction
    return Info(
      if (onlyBuild) AllIcons.Actions.Compile else AllIcons.Actions.Execute,
      actions,
    )
  }

  private fun calculateEligibleActions(project: Project, targetLabel: ResolvedLabel, ruleName: String): List<AnAction> = buildList {
    val targetUtils = project.targetUtils
    val targetInfo = targetUtils.getBuildTargetForLabel(targetLabel)
    val targetKind = targetInfo?.kind ?: TargetKindService.getInstance().guessFromRuleName(ruleName)

    add(BuildTargetAction(targetLabel))
    targetInfo?.let {
      ResyncTargetAction.createIfEnabled(targetLabel)?.let { add(it) }
    }

    val executableTargetsFromTargetUtils =
      targetUtils.getExecutableTargetsForTarget(targetLabel)
        .mapNotNull { executableLabel -> targetUtils.getBuildTargetForLabel(executableLabel) }
        .map { NonImportedExecutableTarget(it.id, it.kind) }
    val executableTargets = if (targetInfo != null) {
      // If we have the targetInfo, we know for sure whether the target is executable.
      // If it isn't executable, do not show gutter, even if executableTargetsFromTargetUtils has targets.
      listOfNotNull(targetInfo.takeIf { targetInfo.kind.isExecutable })
    }
    else if (executableTargetsFromTargetUtils.isNotEmpty()) {
      // Support cases like java_test_suite, which generates targets but isn't imported itself
      executableTargetsFromTargetUtils
    }
    else if (targetKind.isExecutable) {
      // We guessed via our heuristics that the target is executable (e.g., the rule name is my_custom_binary)
      setOf(NonImportedExecutableTarget(targetLabel, targetKind))
    }
    else {
      emptySet()
    }

    val testableTargets = executableTargets.filter { it.kind.ruleType == RuleType.TEST }
    if (testableTargets.size > 1) {
      add(TestTargetAction(project, testableTargets))
      add(RunWithCoverageAction(project, testableTargets))
    }

    val executeActions = executableTargets.flatMap { executableTarget ->
      DefaultActionGroup().fillWithEligibleActions(
        project,
        executableTarget,
        includeTargetNameInText = executableTarget.id != targetLabel,
      ).childActionsOrStubs.toList()
    }
    addAll(executeActions)
  }
}

private class StarlarkCallExpressionVisitor : StarlarkElementVisitor() {
  var ruleName: String? = null
  var targetName: String? = null

  override fun visitCallExpression(node: StarlarkCallExpression) {
    ruleName = node.name
    targetName = node.getTargetName()
  }
}
