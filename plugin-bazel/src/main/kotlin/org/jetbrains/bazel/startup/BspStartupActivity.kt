package org.jetbrains.bazel.startup

import com.intellij.openapi.application.AppUIExecutor
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.impl.CloseProjectWindowHelper
import org.jetbrains.bazel.config.BspFeatureFlags
import org.jetbrains.bazel.config.isBspProjectInitialized
import org.jetbrains.bazel.config.openedTimesSinceLastStartupResync
import org.jetbrains.bazel.config.workspaceModelLoadedFromCache
import org.jetbrains.bazel.projectAware.BspWorkspace
import org.jetbrains.bazel.sync.scope.SecondPhaseSync
import org.jetbrains.bazel.sync.task.PhasedSync
import org.jetbrains.bazel.sync.task.ProjectSyncTask
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.ui.widgets.fileTargets.updateBspFileTargetsWidget
import org.jetbrains.bazel.ui.widgets.tool.window.all.targets.registerBspToolWindow
import org.jetbrains.bazel.utils.RunConfigurationProducersDisabler
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

  private fun Project.handleFailedFirstOpening() {
    log.info("Cancelling BSP sync. Closing the project window")
    // TODO https://youtrack.jetbrains.com/issue/BAZEL-623
    AppUIExecutor.onUiThread().execute {
      CloseProjectWindowHelper().windowClosing(this)
    }
  }

  private suspend fun Project.resyncProjectIfNeeded() {
    if (isProjectInIncompleteState()) {
      if (BspFeatureFlags.isPhasedSync) {
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

  private fun Project.isProjectInIncompleteState() = targetUtils.allTargets().isEmpty() || !workspaceModelLoadedFromCache

  private fun Project.updateProjectProperties() {
    isBspProjectInitialized = true
    openedTimesSinceLastStartupResync += 1
  }
}
