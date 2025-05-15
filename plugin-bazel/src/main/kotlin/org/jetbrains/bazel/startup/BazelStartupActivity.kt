package org.jetbrains.bazel.startup

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.util.PlatformUtils
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.isBazelProjectInitialized
import org.jetbrains.bazel.config.workspaceModelLoadedFromCache
import org.jetbrains.bazel.projectAware.BazelWorkspace
import org.jetbrains.bazel.sync.scope.SecondPhaseSync
import org.jetbrains.bazel.sync.task.PhasedSync
import org.jetbrains.bazel.sync.task.ProjectSyncTask
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.ui.settings.BazelApplicationSettingsService
import org.jetbrains.bazel.ui.widgets.fileTargets.updateBazelFileTargetsWidget
import org.jetbrains.bazel.ui.widgets.tool.window.all.targets.registerBazelToolWindow
import org.jetbrains.bazel.ui.widgets.tool.window.components.BazelTargetsPanelModel
import org.jetbrains.bazel.utils.RunConfigurationProducersDisabler
import java.util.Collections
import java.util.WeakHashMap

private val log = logger<BazelStartupActivity>()

// Use WeakHashMap to avoid leaking the Project instance
private val executedForProject: MutableSet<Project> =
  Collections.synchronizedSet(Collections.newSetFromMap(WeakHashMap()))

/**
 * Runs actions after the project has started up and the index is up-to-date.
 *
 * @see BazelProjectOpenProcessor for additional actions that
 * may run when a project is being imported for the first time.
 */
class BazelStartupActivity : BazelProjectActivity() {
  override suspend fun Project.executeForBazelProject() {
    if (startupActivityExecutedAlready()) {
      log.info("Bazel startup activity executed already for project: $this")
      return
    }
    log.info("Executing Bazel startup activity for project: $this")
    BazelStartupActivityTracker.startConfigurationPhase(this)
    executeOnEveryProjectStartup()

    resyncProjectIfNeeded()

    updateProjectProperties()

    BazelStartupActivityTracker.stopConfigurationPhase(this)
  }

  private fun Project.updateTargetToolwindow() {
    val targets = targetUtils.allBuildTargets().associateBy { it.id }
    service<BazelTargetsPanelModel>().updateTargets(targets)
  }

  /**
   * Make sure calling [BazelOpenProjectProvider.performOpenBazelProject]
   * won't cause [BazelStartupActivity] to execute twice.
   */
  private fun Project.startupActivityExecutedAlready(): Boolean = !executedForProject.add(this)

  private suspend fun Project.executeOnEveryProjectStartup() {
    log.debug("Executing Bazel startup activities for every opening")
    registerBazelToolWindow(this)
    updateTargetToolwindow()
    updateBazelFileTargetsWidget()
    RunConfigurationProducersDisabler(this)
    BazelWorkspace.getInstance(this).initialize()
  }

  private suspend fun Project.resyncProjectIfNeeded() {
    if (isProjectInIncompleteState()) {
      if (BazelApplicationSettingsService.getInstance().settings.enablePhasedSync) {
        log.info("Running Bazel phased sync task")
        PhasedSync(this).sync()
      } else {
        log.info("Running Bazel sync task")
        ProjectSyncTask(this).sync(
          syncScope = SecondPhaseSync,
          buildProject = BazelFeatureFlags.isBuildProjectOnSyncEnabled,
        )
      }
    }
  }

  /**
   * [workspaceModelLoadedFromCache] is always false with GoLand
   * TODO: BAZEL-2038
   */
  private fun Project.isProjectInIncompleteState() =
    targetUtils.allTargets().isEmpty() || !PlatformUtils.isGoIde() && !workspaceModelLoadedFromCache

  private fun Project.updateProjectProperties() {
    isBazelProjectInitialized = true
  }
}
