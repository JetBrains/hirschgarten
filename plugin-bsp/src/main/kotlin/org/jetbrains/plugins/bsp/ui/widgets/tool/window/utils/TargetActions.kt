package org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.magicmetamodel.impl.workspacemodel.isJvmTarget
import org.jetbrains.plugins.bsp.services.BspCoroutineService
import org.jetbrains.plugins.bsp.ui.actions.LoadTargetAction
import org.jetbrains.plugins.bsp.ui.actions.LoadTargetWithDependenciesAction
import org.jetbrains.plugins.bsp.ui.configuration.run.BspRunHandler
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions.BspRunnerAction
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions.BuildTargetAction
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions.RunTargetAction
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions.RunWithLocalJvmRunnerAction
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions.TestTargetAction
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions.TestWithLocalJvmRunnerAction

public object TargetActions {
  public fun getStandardContextActions(
    node: TargetNode,
    inGutter: Boolean,
  ): List<AnAction> =
    when ((node as? TargetNode.ValidTarget)?.isLoaded) {
      true -> getActionsForLoadedTarget(node.target, inGutter)
      false -> getActionsForUnloadedTarget(node.target)
      null -> emptyList() // we do not provide any actions for directories or invalid targets
    }

  public fun getActionsForLoadedTarget(
    target: BuildTargetInfo,
    inGutter: Boolean,
  ): List<AnAction> {
    val capabilities = target.capabilities
    val applicableActions = mutableListOf<AnAction>()
    val add: AnAction.() -> Unit = { applicableActions.add(this) }

    if (!inGutter && capabilities.canCompile)
      BuildTargetAction(target.id).add()
    if (capabilities.canRun)
      RunTargetAction(targetInfo = target, verboseText = inGutter).add()
    if (capabilities.canTest)
      TestTargetAction(targetInfo = target, verboseText = inGutter).add()
    if (capabilities.canDebug && BspRunHandler.getRunHandler(target).canDebug(target))
      RunTargetAction(targetInfo = target, verboseText = inGutter, isDebugAction = true).add()
    if (target.languageIds.isJvmTarget())
      applicableActions.addLocalJvmRunnerActions(target, inGutter)

    return applicableActions.toList()
  }

  private fun getActionsForUnloadedTarget(target: BuildTargetInfo): List<AnAction> =
    listOf(
      LoadTargetAction(
        targetId = target.id,
        text = { org.jetbrains.plugins.bsp.config.BspPluginBundle.message("widget.load.target.popup.message") }
      ),
      LoadTargetWithDependenciesAction(
        targetId = target.id,
        text = {
          org.jetbrains.plugins.bsp.config.BspPluginBundle.message("widget.load.target.with.deps.popup.message")
        },
      ),
    )

  private fun MutableList<AnAction>.addLocalJvmRunnerActions(
    target: BuildTargetInfo,
    inGutter: Boolean,
  ) {
    if (target.capabilities.canRun) {
      add(RunWithLocalJvmRunnerAction(target, verboseText = inGutter))
      if (target.capabilities.canDebug) {
        add(RunWithLocalJvmRunnerAction(target, isDebugMode = true, verboseText = inGutter))
      }
    }
    if (target.capabilities.canTest) {
      add(TestWithLocalJvmRunnerAction(target, verboseText = inGutter))
      if (target.capabilities.canDebug) {
        add(TestWithLocalJvmRunnerAction(target, isDebugMode = true, verboseText = inGutter))
      }
    }
  }

  public fun performDefaultAction(node: TargetNode, project: Project) {
    if (node is TargetNode.ValidTarget) node.performDefaultValidTargetAction(project)
  }
}

private fun TargetNode.ValidTarget.performDefaultValidTargetAction(project: Project) {
  when {
    !isLoaded ->
      BspCoroutineService.getInstance(project).start { LoadTargetAction.loadTarget(project, target.id) }
    target.capabilities.canTest ->
      TestTargetAction(targetInfo = target).prepareAndPerform(project)
    target.capabilities.canRun ->
      RunTargetAction(targetInfo = target).prepareAndPerform(project)
    target.capabilities.canCompile ->
      BuildTargetAction.buildTarget(project, target.id)
  }
}

private fun BspRunnerAction.prepareAndPerform(project: Project) {
  BspCoroutineService.getInstance(project).start {
    doPerformAction(project)
  }
}
