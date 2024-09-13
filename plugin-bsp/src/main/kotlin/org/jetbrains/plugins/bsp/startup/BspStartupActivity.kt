package org.jetbrains.plugins.bsp.startup

import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.wm.impl.CloseProjectWindowHelper
import com.intellij.platform.backend.workspace.workspaceModel
import com.intellij.util.application
import com.intellij.workspaceModel.ide.impl.WorkspaceModelImpl
import org.jetbrains.plugins.bsp.building.BspConsoleService
import org.jetbrains.plugins.bsp.config.BspFeatureFlags
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.isBspProject
import org.jetbrains.plugins.bsp.config.isBspProjectInitialized
import org.jetbrains.plugins.bsp.config.rootDir
import org.jetbrains.plugins.bsp.impl.flow.sync.ProjectSyncTask
import org.jetbrains.plugins.bsp.impl.projectAware.BspWorkspace
import org.jetbrains.plugins.bsp.impl.server.connection.connectionDetailsProvider
import org.jetbrains.plugins.bsp.impl.target.temporaryTargetUtils
import org.jetbrains.plugins.bsp.ui.widgets.file.targets.updateBspFileTargetsWidget
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.all.targets.registerBspToolWindow
import org.jetbrains.plugins.bsp.utils.RunConfigurationProducersDisabler

private val log = logger<BspStartupActivity>()

/**
 * Runs actions after the project has started up and the index is up-to-date.
 *
 * @see BspProjectOpenProcessor for additional actions that
 * may run when a project is being imported for the first time.
 */
public class BspStartupActivity : ProjectActivity {
  override suspend fun execute(project: Project) {
    if (project.isBspProject) {
      project.executeForBspProject()
    }
  }

  private suspend fun Project.executeForBspProject() {
    log.info("Executing BSP startup activity for project: $this")
    BspStartupActivityTracker.startConfigurationPhase(this)
    executeEveryTime()

    executeForNewProject()

    resyncProjectIfNeeded()

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
      if (!isBspProjectInitialized || !(workspaceModel as WorkspaceModelImpl).loadedFromCache) {
        isBspProjectInitialized = runOnFirstOpening()
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

  private suspend fun Project.runOnFirstOpening(): Boolean {
    val wasFirstOpeningSuccessful = connectionDetailsProvider.onFirstOpening(this, rootDir)
    log.debug("Was onFirstOpening successful: $wasFirstOpeningSuccessful")

    if (!wasFirstOpeningSuccessful) {
      handleFailedFirstOpening()
      return false
    }

    return true
  }

  private fun Project.handleFailedFirstOpening() {
    log.info("Cancelling BSP sync. Closing the project window")
    // TODO https://youtrack.jetbrains.com/issue/BAZEL-623
    application.invokeLater {
      CloseProjectWindowHelper().windowClosing(this)
    }
  }

  private suspend fun Project.resyncProjectIfNeeded() {
    if (isBspProjectInitialized && isProjectInIncompleteState()) {
      log.info("Running BSP sync task")
      ProjectSyncTask(this).sync(
        buildProject = BspFeatureFlags.isBuildProjectOnSyncEnabled,
      )
    }
  }

  private fun Project.isProjectInIncompleteState() = temporaryTargetUtils.allTargetIds().isEmpty()
}
