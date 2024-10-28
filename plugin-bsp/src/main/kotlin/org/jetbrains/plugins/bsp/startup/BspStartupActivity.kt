package org.jetbrains.plugins.bsp.startup

import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.openapi.application.AppUIExecutor
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.impl.CloseProjectWindowHelper
import com.intellij.platform.backend.workspace.workspaceModel
import com.intellij.workspaceModel.ide.impl.WorkspaceModelImpl
import org.jetbrains.plugins.bsp.building.BspConsoleService
import org.jetbrains.plugins.bsp.config.BspFeatureFlags
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.openedTimesSinceLastStartupResync
import org.jetbrains.plugins.bsp.config.rootDir
import org.jetbrains.plugins.bsp.impl.flow.sync.FullProjectSync
import org.jetbrains.plugins.bsp.impl.flow.sync.ProjectSyncTask
import org.jetbrains.plugins.bsp.impl.projectAware.BspWorkspace
import org.jetbrains.plugins.bsp.impl.server.connection.connectionDetailsProvider
import org.jetbrains.plugins.bsp.target.temporaryTargetUtils
import org.jetbrains.plugins.bsp.ui.widgets.fileTargets.updateBspFileTargetsWidget
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.all.targets.registerBspToolWindow
import org.jetbrains.plugins.bsp.utils.RunConfigurationProducersDisabler

private val log = logger<BspStartupActivity>()

/**
 * Runs actions after the project has started up and the index is up-to-date.
 *
 * @see BspProjectOpenProcessor for additional actions that
 * may run when a project is being imported for the first time.
 */
class BspStartupActivity : BspProjectActivity() {
  override suspend fun Project.executeForBspProject() {
    log.info("Executing BSP startup activity for project: $this")
    BspStartupActivityTracker.startConfigurationPhase(this)
    executeEveryTime()

    executeForNewProject()

    resyncProjectIfNeeded()

    openedTimesSinceLastStartupResync += 1

    BspStartupActivityTracker.stopConfigurationPhase(this)
  }

  private suspend fun Project.executeEveryTime() {
    log.debug("Executing BSP startup activities for every opening")
    registerBspToolWindow(this)
    updateBspFileTargetsWidget()
    RunConfigurationProducersDisabler(this)
    BspWorkspace.getInstance(this).initialize()
  }

  private suspend fun Project.executeForNewProject() {
    log.debug("Executing BSP startup activities only for new project")
    try {
      if (!(workspaceModel as WorkspaceModelImpl).loadedFromCache) {
        runOnFirstOpening()
      }
    } catch (e: Exception) {
      val bspSyncConsole = BspConsoleService.getInstance(this).bspSyncConsole
      log.info("BSP sync has failed", e)
      bspSyncConsole.startTask(
        taskId = "bsp-pre-import",
        title = BspPluginBundle.message("console.task.pre.import.title"),
        message = BspPluginBundle.message("console.task.pre.import.in.progress"),
      )
      bspSyncConsole.finishTask(
        taskId = "bsp-pre-import",
        message = BspPluginBundle.message("console.task.pre.import.failed"),
        result = FailureResultImpl(e),
      )
    }
  }

  private suspend fun Project.runOnFirstOpening() {
    val wasFirstOpeningSuccessful = connectionDetailsProvider.onFirstOpening(this, rootDir)
    log.debug("Was onFirstOpening successful: $wasFirstOpeningSuccessful")

    if (!wasFirstOpeningSuccessful) {
      handleFailedFirstOpening()
    }
  }

  private fun Project.handleFailedFirstOpening() {
    log.info("Cancelling BSP sync. Closing the project window")
    // TODO https://youtrack.jetbrains.com/issue/BAZEL-623
    AppUIExecutor.onUiThread().execute {
      CloseProjectWindowHelper().windowClosing(this)
    }
  }

  private suspend fun Project.resyncProjectIfNeeded() {
    if (isProjectInIncompleteState()) {
      log.info("Running BSP sync task")
      ProjectSyncTask(this).sync(
        syncScope = FullProjectSync,
        buildProject = BspFeatureFlags.isBuildProjectOnSyncEnabled,
      )
      openedTimesSinceLastStartupResync = 0
    }
  }

  private fun Project.isProjectInIncompleteState() =
    temporaryTargetUtils.allTargetIds().isEmpty() || !(workspaceModel as WorkspaceModelImpl).loadedFromCache
}
