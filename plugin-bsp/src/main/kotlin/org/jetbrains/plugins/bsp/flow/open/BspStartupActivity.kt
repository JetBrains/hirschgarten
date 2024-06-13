package org.jetbrains.plugins.bsp.flow.open

import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.ide.impl.isTrusted
import com.intellij.openapi.application.AppUIExecutor
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.wm.impl.CloseProjectWindowHelper
import org.jetbrains.plugins.bsp.config.BspFeatureFlags
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.BspWorkspace
import org.jetbrains.plugins.bsp.config.isBspProject
import org.jetbrains.plugins.bsp.config.isBspProjectInitialized
import org.jetbrains.plugins.bsp.config.rootDir
import org.jetbrains.plugins.bsp.server.connection.DefaultBspConnection
import org.jetbrains.plugins.bsp.server.connection.connection
import org.jetbrains.plugins.bsp.server.connection.connectionDetailsProvider
import org.jetbrains.plugins.bsp.server.tasks.SyncProjectTask
import org.jetbrains.plugins.bsp.ui.console.BspConsoleService
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
    executeEveryTime()

    if (!isBspProjectInitialized) {
      executeForNewProject()
    }

    postActivity()
  }

  private suspend fun Project.executeEveryTime() {
    log.debug("Executing BSP startup activities for every opening")
    registerBspToolWindow(this)
    updateBspFileTargetsWidget()
    RunConfigurationProducersDisabler(this)
  }

  private suspend fun Project.executeForNewProject() {
    log.debug("Executing BSP startup activities only for new project")
    try {
      runSync(this)
      isBspProjectInitialized = true
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

  private suspend fun runSync(project: Project) {
    log.info("Running BSP sync")

    project.connection = DefaultBspConnection(project, project.connectionDetailsProvider)

    val wasFirstOpeningSuccessful = project.connectionDetailsProvider.onFirstOpening(project, project.rootDir)
    log.debug("Was onFirstOpening successful: $wasFirstOpeningSuccessful")

    if (wasFirstOpeningSuccessful) {
      if (project.isTrusted()) {
        log.info("Running BSP sync task")
        SyncProjectTask(project).execute(
          shouldBuildProject = BspFeatureFlags.isBuildProjectOnSyncEnabled,
        )
      }
    } else {
      log.info("Cancelling BSP sync. Closing the project window")
      // TODO https://youtrack.jetbrains.com/issue/BAZEL-623
      AppUIExecutor.onUiThread().execute {
        CloseProjectWindowHelper().windowClosing(project)
      }
    }
  }

  private fun Project.postActivity() {
    log.info("Executing BSP startup activity")
    BspWorkspace.getInstance(this).initialize()
  }
}
