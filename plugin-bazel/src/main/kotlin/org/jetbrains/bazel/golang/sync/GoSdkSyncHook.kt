package org.jetbrains.bazel.golang.sync

import com.goide.sdk.GoSdk
import com.goide.sdk.GoSdkService
import com.goide.sdk.GoSdkUtil
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.backgroundWriteAction
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.platform.util.progress.SequentialProgressReporter
import com.intellij.util.FileContentUtilCore
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.sync.ProjectPostSyncHook
import org.jetbrains.bazel.sync.SyncCache
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.ui.console.withSubtask
import org.jetbrains.bsp.protocol.GoBuildTarget
import org.jetbrains.bsp.protocol.utils.extractGoBuildTarget

/** From [com.goide.inspections.GoWrongSdkConfigurationNotificationProvider].  */
private const val DO_NOT_SHOW_NOTIFICATION_ABOUT_EMPTY_GOPATH = "DO_NOT_SHOW_NOTIFICATION_ABOUT_EMPTY_GOPATH"

class GoSdkSyncHook : ProjectPostSyncHook {
  override fun isEnabled(project: Project): Boolean =
    BazelFeatureFlags.isGoSupportEnabled &&
      project.targetUtils
        .allBuildTargets()
        .filter { it.id.isMainWorkspace }
        .mapNotNull {
          it.data as? GoBuildTarget
        }.any { it.generatedSources.isNotEmpty() }

  override suspend fun onPostSync(environment: ProjectPostSyncHook.ProjectPostSyncHookEnvironment) {
    val project = environment.project
    calculateAndAddGoSdk(environment.progressReporter, project, environment.taskId)
    PropertiesComponent.getInstance().setValue(DO_NOT_SHOW_NOTIFICATION_ABOUT_EMPTY_GOPATH, true)

    refreshGeneratedSources(project)
  }

  private suspend fun refreshGeneratedSources(project: Project) {
    val generatedSources = project.targetUtils
      .allBuildTargets()
      .mapNotNull { extractGoBuildTarget(it) }
      .flatMap { it.generatedSources }
      .toSet()

    SyncCache.getInstance(project).clear()

    val virtualFileManager = VirtualFileManager.getInstance()
    val filesToReparse = generatedSources.mapNotNull { virtualFileManager.findFileByNioPath(it) }
    if (filesToReparse.isNotEmpty()) {
      writeAction {
        filesToReparse.forEach { it.refresh(false, false) }
      }
      backgroundWriteAction {
        FileContentUtilCore.reparseFiles(filesToReparse)
      }
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
      .let { it ?: GoSdkUtil.suggestSdkDirectory() }
      ?.let { path -> GoSdk.fromHomePath(path.toString()) }
      ?.setAsUsed(project)
  }

  private suspend fun GoSdk.setAsUsed(project: Project) {
    val goSdkService = GoSdkService.getInstance(project)
    writeAction { goSdkService.setSdk(this) }
  }
}
