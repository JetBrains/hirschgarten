package org.jetbrains.bazel.ui.projectTree.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.toNioPathOrNull
import org.jetbrains.bazel.action.SuspendableAction
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.runnerAction.RunWithCoverageAction
import org.jetbrains.bazel.runnerAction.TestTargetAction
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetTag
import javax.swing.Icon

internal open class RunAllTestsBaseAction(
  text: () -> String,
  icon: Icon,
  private val createAction: (project: Project, targets: List<BuildTarget>, directoryName: String) -> SuspendableAction,
) : SuspendableAction(
    text = text,
    icon = icon,
  ) {
  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    val action =
      createAction(
        project,
        readAction { getAllTestTargetInfos(project, e) },
        e.getCurrentPath()?.name.orEmpty(),
      )
    action.actionPerformed(e)
  }

  override fun update(project: Project, e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = runReadAction { shouldShowAction(project, e) }
  }

  private fun getAllTestTargetInfos(project: Project, event: AnActionEvent): List<BuildTarget> =
    event
      .getCurrentPath()
      ?.toChildTestTargets(project)
      .orEmpty()

  private fun shouldShowAction(project: Project, e: AnActionEvent): Boolean =
    e.getCurrentPath()?.toChildTestTargets(project)?.isNotEmpty() ?: false

  private fun AnActionEvent.getCurrentPath(): VirtualFile? = getData(PlatformDataKeys.VIRTUAL_FILE)

  private fun VirtualFile.toChildTestTargets(project: Project): List<BuildTarget> {
    val childTargets =
      if (isDirectory) {
        val path = toNioPathOrNull() ?: return emptyList()
        project.targetUtils
          .allBuildTargets()
          .filter { it.baseDirectory.startsWith(path) }
      } else {
        project.targetUtils.getExecutableTargetsForFile(this).mapNotNull { project.targetUtils.getBuildTargetForLabel(it) }
      }
    return childTargets.filter {
      it.kind.ruleType == RuleType.TEST && !it.tags.contains(BuildTargetTag.MANUAL)
    }
  }
}

internal class RunAllTestsAction :
  RunAllTestsBaseAction(
    text = { BazelPluginBundle.message("action.run.all.tests") },
    icon = AllIcons.Actions.Execute,
    createAction = { project, targets, directoryName ->
      TestTargetAction(
        project,
        targets,
        text = {
          BazelPluginBundle.message("action.run.all.tests.under", directoryName)
        },
      )
    },
  )

internal class RunAllTestsWithCoverageAction :
  RunAllTestsBaseAction(
    text = { BazelPluginBundle.message("action.run.all.tests.with.coverage") },
    icon = AllIcons.General.RunWithCoverage,
    createAction = { project, targets, directoryName ->
      RunWithCoverageAction(project, targets, text = {
        BazelPluginBundle.message("action.run.all.tests.under.with.coverage", directoryName)
      })
    },
  )
