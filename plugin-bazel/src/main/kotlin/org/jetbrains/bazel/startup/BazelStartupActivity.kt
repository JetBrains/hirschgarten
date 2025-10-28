package org.jetbrains.bazel.startup

import com.intellij.find.impl.FindInProjectUtil
import com.intellij.ide.util.gotoByName.GOTO_FILE_SEARCH_IN_NON_INDEXABLE
import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.ui.EditorNotificationProvider
import com.intellij.util.PlatformUtils
import com.intellij.workspaceModel.ide.impl.WorkspaceModelImpl
import kotlinx.coroutines.flow.update
import org.jetbrains.bazel.bazelrunner.outputs.ProcessSpawner
import org.jetbrains.bazel.commons.BidirectionalMap
import org.jetbrains.bazel.commons.FileUtil
import org.jetbrains.bazel.commons.SystemInfoProvider
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.BazelProjectProperties
import org.jetbrains.bazel.config.workspaceModelLoadedFromCache
import org.jetbrains.bazel.flow.sync.bazelPaths.BazelBinPathService
import org.jetbrains.bazel.performance.telemetry.TelemetryManager
import org.jetbrains.bazel.projectAware.BazelWorkspace
import org.jetbrains.bazel.startup.utils.BazelProjectActivity
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
class BazelStartupActivity : BazelProjectActivity() {
  override suspend fun executeForBazelProject(project: Project) {
    ProcessSpawner.provideProcessSpawner(GenericCommandLineProcessSpawner)
    TelemetryManager.provideTelemetryManager(IntellijTelemetryManager)
    BidirectionalMap.provideBidirectionalMapFactory { IntellijBidirectionalMap<Any, Any>() }
    SystemInfoProvider.provideSystemInfoProvider(IntellijSystemInfoProvider)
    FileUtil.provideFileUtil(FileUtilIntellij)
    log.info("Executing Bazel startup activity for project: $project")
    val trackerService = project.serviceAsync<BspConfigurationTrackerService>()
    try {
      trackerService.isRunning.update { true }

      executeOnEveryProjectStartup(project)

      resyncProjectIfNeeded(project)

      executeOnSyncedProject(project)

      project.serviceAsync<BazelProjectProperties>().isInitialized = true
    } finally {
      trackerService.isRunning.update { false }
    }
  }
}

private suspend fun executeOnEveryProjectStartup(project: Project) {
  log.debug("Executing Bazel startup activities for every opening")
  updateBazelFileTargetsWidget(project)
  project.serviceAsync<BazelWorkspace>().initialize()
  disableUnableToFindJdkNotification(project)
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
  // Only enable searching after all the excludes from the project view are applied
  if (!BazelFeatureFlags.findInFilesNonIndexable) return
  project.putUserData(FindInProjectUtil.FIND_IN_FILES_SEARCH_IN_NON_INDEXABLE, true)
  project.putUserData(GOTO_FILE_SEARCH_IN_NON_INDEXABLE, true)
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
  project
    .serviceAsync<BazelBinPathService>()
    .bazelExecPath
    ?.let { Path.of(it) }
    ?.isDirectory() == true

// https://youtrack.jetbrains.com/issue/BAZEL-2598
// TODO: remove once a proper API/fix exists on Docker side
private fun disableUnableToFindJdkNotification(project: Project) {
  val notificationProvider = EditorNotificationProvider.EP_NAME.findFirstSafe(project) {
    it.javaClass.name == "com.intellij.cwm.plugin.java.rdserver.unattendedHost.editor.UnattendedHostJdkConfigurationNotificationProvider"
  } ?: return
  val disableNotificationMethod = try {
    notificationProvider.javaClass.getDeclaredMethod("disableNotification", Project::class.java)
  } catch (_: NoSuchMethodException) {
    return
  }
  disableNotificationMethod.isAccessible = true
  disableNotificationMethod.invoke(notificationProvider, project)
}
