package org.jetbrains.plugins.bsp.startup

import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.openapi.application.AppUIExecutor
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.impl.CloseProjectWindowHelper
import org.jetbrains.plugins.bsp.config.BspFeatureFlags
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.buildToolId
import org.jetbrains.plugins.bsp.config.isBspProjectInitialized
import org.jetbrains.plugins.bsp.config.isBspProjectLoaded
import org.jetbrains.plugins.bsp.config.openedTimesSinceLastStartupResync
import org.jetbrains.plugins.bsp.config.rootDir
import org.jetbrains.plugins.bsp.config.workspaceModelLoadedFromCache
import org.jetbrains.plugins.bsp.projectAware.BspWorkspace
import org.jetbrains.plugins.bsp.server.connection.connectionDetailsProvider
import org.jetbrains.plugins.bsp.sync.scope.SecondPhaseSync
import org.jetbrains.plugins.bsp.sync.task.PhasedSync
import org.jetbrains.plugins.bsp.sync.task.ProjectSyncTask
import org.jetbrains.plugins.bsp.target.targetUtils
import org.jetbrains.plugins.bsp.ui.console.BspConsoleService
import org.jetbrains.plugins.bsp.ui.widgets.fileTargets.updateBspFileTargetsWidget
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.all.targets.registerBspToolWindow
import org.jetbrains.plugins.bsp.utils.RunConfigurationProducersDisabler
import java.util.Collections
import java.util.WeakHashMap

private val log = logger<BspStartupActivity>()

// Use WeakHashMap to avoid leaking the Project instance
private val executedForProject: MutableSet<Project> =
  Collections.synchronizedSet(Collections.newSetFromMap(WeakHashMap()))

/**
 * Runs actions after the project has started up and the index is up-to-date.
 *
 * @see BspProjectOpenProcessor for additional actions that
 * may run when a project is being imported for the first time.
 */
class BspStartupActivity : BspProjectActivity() {
  override suspend fun Project.executeForBspProject() {
    if (startupActivityExecutedAlready()) {
      log.info("BSP startup activity executed already for project: $this")
      return
    }
    log.info("Executing BSP startup activity for project: $this")
    BspStartupActivityTracker.startConfigurationPhase(this)
    executeEveryTime()

    executeForNewProject()

    resyncProjectIfNeeded()

    updateProjectProperties()

    BspStartupActivityTracker.stopConfigurationPhase(this)
  }

  /**
   * Make sure calling [BazelBspOpenProjectProvider.performOpenBazelProjectViaBspPlugin]
   * won't cause [BspStartupActivity] to execute twice.
   */
  private fun Project.startupActivityExecutedAlready(): Boolean = !executedForProject.add(this)

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
      if (!isBspProjectLoaded) {
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
      // TODO: https://youtrack.jetbrains.com/issue/BAZEL-1555
      if (BspFeatureFlags.isPhasedSync && buildToolId.id == "bazelbsp") {
        log.info("Running BSP phased sync task")
        PhasedSync(this).sync()
      } else {
        log.info("Running BSP sync task")
        ProjectSyncTask(this).sync(
          syncScope = SecondPhaseSync,
          buildProject = BspFeatureFlags.isBuildProjectOnSyncEnabled,
        )
      }

      openedTimesSinceLastStartupResync = 0
    }
  }

  private fun Project.isProjectInIncompleteState() = targetUtils.allTargetIds().isEmpty() || !workspaceModelLoadedFromCache

  private fun Project.updateProjectProperties() {
    isBspProjectInitialized = true
    openedTimesSinceLastStartupResync += 1
  }
}
