package org.jetbrains.bazel.startup

import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.isFile
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.util.PlatformUtils
import com.intellij.workspaceModel.ide.impl.WorkspaceModelImpl
import kotlinx.coroutines.flow.update
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.BazelProjectProperties
import org.jetbrains.bazel.languages.projectview.DefaultProjectViewService
import org.jetbrains.bazel.languages.projectview.ProjectViewService
import org.jetbrains.bazel.projectAware.BazelWorkspace
import org.jetbrains.bazel.settings.bazel.bazelProjectSettings
import org.jetbrains.bazel.startup.utils.BazelProjectActivity
import org.jetbrains.bazel.sync.environment.projectCtx
import org.jetbrains.bazel.sync.scope.SecondPhaseSync
import org.jetbrains.bazel.sync.task.PhasedSync
import org.jetbrains.bazel.sync.task.ProjectSyncTask
import org.jetbrains.bazel.target.TargetUtils
import org.jetbrains.bazel.ui.settings.BazelApplicationSettingsService
import org.jetbrains.bazel.ui.widgets.fileTargets.updateBazelFileTargetsWidget
import java.nio.file.Path
import kotlin.io.path.isDirectory

private val log = logger<BazelStartupActivity>()

/**
 * Runs actions after the project has started up and the index is up to date.
 *
 * @see org.jetbrains.bazel.flow.open.BazelProjectOpenProcessor for additional actions that
 * may run when a project is being imported for the first time.
 */
internal class BazelStartupActivity : BazelProjectActivity() {
  override suspend fun executeForBazelProject(project: Project) {
    log.info("Executing Bazel startup activity for project: $project")
    val trackerService = project.serviceAsync<BspConfigurationTrackerService>()
    try {
      trackerService.isRunning.update { true }

      // when ProjectView file is known load it immediately,
      // otherwise wait for first invocation of `RegenerateProjectViewFileContentPreSyncHook`
      // sync hook which initializes ProjectView to correct value
      val projectViewPath = project.bazelProjectSettings.projectViewPath
      if (projectViewPath?.isFile == true) {
        val projectViewService = ProjectViewService.getInstance(project) as? DefaultProjectViewService
        projectViewService?.ensureProjectViewInitialized()
      }

      executeOnEveryProjectStartup(project)

      resyncProjectIfNeeded(project)

      executeOnSyncedProject(project)
    } finally {
      trackerService.isRunning.update { false }
    }
  }
}

private suspend fun executeOnEveryProjectStartup(project: Project) {
  log.debug("Executing Bazel startup activities for every opening")
  updateBazelFileTargetsWidget(project)
  project.serviceAsync<BazelWorkspace>().initialize()
}

private suspend fun resyncProjectIfNeeded(project: Project) {
  if (isProjectInIncompleteState(project)) {
    if (serviceAsync<BazelApplicationSettingsService>().settings.enablePhasedSync) {
      log.info("Running Bazel phased sync task")
      PhasedSync(project).sync()
    } else {
      log.info("Running Bazel sync task")
      ProjectSyncTask(project).sync(
        syncScope = SecondPhaseSync,
        buildProject = BazelFeatureFlags.isBuildProjectOnSyncEnabled,
      )
    }
  }
}

private fun executeOnSyncedProject(project: Project) {
}

/**
 * [workspaceModelLoadedFromCache] is always false with GoLand
 * TODO: BAZEL-2038
 */
private suspend fun isProjectInIncompleteState(project: Project): Boolean =
  project.serviceAsync<TargetUtils>().getTotalTargetCount() == 0 ||
    project.serviceAsync<BazelProjectProperties>().isBrokenBazelProject ||
    !PlatformUtils.isGoIde() &&
    !(project.serviceAsync<WorkspaceModel>() as WorkspaceModelImpl).loadedFromCache ||
    !bazelExecPathExists(project)

private suspend fun bazelExecPathExists(project: Project): Boolean =
  project.projectCtx
    .bazelExecPath
    ?.let { Path.of(it) }
    ?.isDirectory() == true
