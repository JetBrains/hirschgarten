package org.jetbrains.bazel.startup

import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolderEx
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.workspaceModel.ide.impl.WorkspaceModelImpl
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.BazelProjectProperties
import org.jetbrains.bazel.projectAware.BazelWorkspace
import org.jetbrains.bazel.sync.scope.SecondPhaseSync
import org.jetbrains.bazel.sync.task.PhasedSync
import org.jetbrains.bazel.sync.task.ProjectSyncTask
import org.jetbrains.bazel.target.TargetUtils
import org.jetbrains.bazel.ui.settings.BazelApplicationSettingsService
import org.jetbrains.bazel.ui.widgets.fileTargets.updateBazelFileTargetsWidget
import org.jetbrains.bazel.ui.widgets.tool.window.all.targets.registerBazelToolWindow
import org.jetbrains.bazel.ui.widgets.tool.window.components.BazelTargetsPanelModel
import org.jetbrains.bazel.utils.configureRunConfigurationIgnoreProducers

private val log = logger<BazelStartupActivity>()

private val EXECUTED_FOR_PROJECT = Key<Boolean>("bazel.startup.executed.for.project")

/**
 * Runs actions after the project has started up and the index is up to date.
 *
 * @see org.jetbrains.bazel.flow.open.BazelProjectOpenProcessor for additional actions that
 * may run when a project is being imported for the first time.
 */
class BazelStartupActivity : BazelProjectActivity() {
  override suspend fun executeForBazelProject(project: Project) {
    if (startupActivityExecutedAlready(project)) {
      log.info("Bazel startup activity executed already for project: $project")
      return
    }

    log.info("Executing Bazel startup activity for project: $project")
    BazelStartupActivityTracker.startConfigurationPhase(project)

    executeOnEveryProjectStartup(project)

    resyncProjectIfNeeded(project)

    project.serviceAsync<BazelProjectProperties>().isInitialized = true

    BazelStartupActivityTracker.stopConfigurationPhase(project)
  }
}

private suspend fun updateTargetToolwindow(project: Project) {
  val targets = project.serviceAsync<TargetUtils>().allBuildTargetAsLabelToTargetMap()
  project.serviceAsync<BazelTargetsPanelModel>().updateTargets(targets)
}

private suspend fun executeOnEveryProjectStartup(project: Project) {
  log.debug("Executing Bazel startup activities for every opening")
  registerBazelToolWindow(project)
  updateTargetToolwindow(project)
  updateBazelFileTargetsWidget(project)
  configureRunConfigurationIgnoreProducers(project)
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

/**
 * Make sure calling [org.jetbrains.bazel.flow.open.performOpenBazelProject]
 * won't cause [BazelStartupActivity] to execute twice.
 */
private fun startupActivityExecutedAlready(project: Project): Boolean =
  !(project as UserDataHolderEx).replace(EXECUTED_FOR_PROJECT, null, true)

private suspend fun isProjectInIncompleteState(project: Project): Boolean =
  project.serviceAsync<TargetUtils>().getTotalTargetCount() == 0 ||
    !(project.serviceAsync<WorkspaceModel>() as WorkspaceModelImpl).loadedFromCache
