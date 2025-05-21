package org.jetbrains.bazel.golang.sync

import com.goide.sdk.GoSdk
import com.goide.sdk.GoSdkService
import com.goide.sdk.GoSdkUtil
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.project.Project
import com.intellij.platform.util.progress.SequentialProgressReporter
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.sync.ProjectPostSyncHook
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.ui.console.withSubtask
import org.jetbrains.bsp.protocol.utils.extractGoBuildTarget

/** From [com.goide.inspections.GoWrongSdkConfigurationNotificationProvider].  */
private const val DO_NOT_SHOW_NOTIFICATION_ABOUT_EMPTY_GOPATH = "DO_NOT_SHOW_NOTIFICATION_ABOUT_EMPTY_GOPATH"

class GoSdkSyncHook : ProjectPostSyncHook {
  override suspend fun onPostSync(environment: ProjectPostSyncHook.ProjectPostSyncHookEnvironment) {
    val project = environment.project
    calculateAndAddGoSdk(environment.progressReporter, project, environment.taskId)
    PropertiesComponent.getInstance().setValue(DO_NOT_SHOW_NOTIFICATION_ABOUT_EMPTY_GOPATH, true)
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
      .let { it ?: GoSdkUtil.suggestSdkDirectory() }
      ?.let { path -> GoSdk.fromHomePath(path.toString()) }
      ?.setAsUsed(project)
  }

  private suspend fun GoSdk.setAsUsed(project: Project) {
    val goSdkService = GoSdkService.getInstance(project)
    writeAction { goSdkService.setSdk(this) }
  }
}
