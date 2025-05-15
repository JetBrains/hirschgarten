package org.jetbrains.bazel.golang.sync

import com.goide.sdk.GoSdk
import com.goide.sdk.GoSdkService
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.project.Project
import com.intellij.platform.util.progress.SequentialProgressReporter
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.projectStructure.workspaceModel.workspaceModelDiff
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.ui.console.withSubtask
import org.jetbrains.bsp.protocol.utils.extractGoBuildTarget

class GoSdkSyncHook : ProjectSyncHook {
  override suspend fun onSync(environment: ProjectSyncHook.ProjectSyncHookEnvironment) {
    val project = environment.project
    environment.diff.workspaceModelDiff.addPostApplyAction {
      calculateAndAddGoSdk(environment.progressReporter, project, environment.taskId)
    }
  }

  private suspend fun calculateAndAddGoSdk(
    reporter: SequentialProgressReporter,
    project: Project,
    taskId: String,
  ) = project.withSubtask(
    reporter = reporter,
    taskId = taskId,
    text = BazelPluginBundle.message("console.task.model.calculate.add.go.fetched.sdk"),
  ) {
    project.targetUtils
      .allBuildTargets()
      .firstNotNullOfOrNull { extractGoBuildTarget(it)?.sdkHomePath }
      ?.let { path -> GoSdk.fromHomePath(path.toString()) }
      ?.setAsUsed(project)
  }

  private suspend fun GoSdk.setAsUsed(project: Project) {
    val goSdkService = GoSdkService.getInstance(project)
    writeAction { goSdkService.setSdk(this) }
  }
}
