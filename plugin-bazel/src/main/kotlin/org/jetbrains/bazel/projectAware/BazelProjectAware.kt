package org.jetbrains.bazel.projectAware

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectAware
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectId
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectListener
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectReloadContext
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectTracker
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemRefreshStatus
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bazel.config.bazelProjectProperties
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.settings.bazel.bazelProjectSettings
import org.jetbrains.bazel.sync.SyncCache
import org.jetbrains.bazel.sync.scope.SecondPhaseSync
import org.jetbrains.bazel.sync.status.SyncStatusListener
import org.jetbrains.bazel.sync.task.ProjectSyncTask

class BazelProjectAware(private val project: Project) : ExternalSystemProjectAware {
  override val projectId: ExternalSystemProjectId = getBazelProjectId(project.rootDir)

  @VisibleForTesting
  val cachedBazelFiles =
    SyncCache.SyncCacheComputable {
      // if a write action collides with this read action, a cancellation exception will be thrown and the value will not be cached
      ReadAction.computeCancellable<Set<String>, Throwable> { calculateBazelConfigFiles(project) }
    }

  override val settingsFiles: Set<String>
    get() = SyncCache.getInstance(project).get(cachedBazelFiles)

  override fun reloadProject(context: ExternalSystemProjectReloadContext) {
    if (context.isExplicitReload) {
      BazelCoroutineService.getInstance(project).start {
        ProjectSyncTask(project).sync(syncScope = SecondPhaseSync, buildProject = false)
      }
    }
  }

  override fun subscribe(listener: ExternalSystemProjectListener, parentDisposable: Disposable) {
    project.messageBus.connect().subscribe(
      SyncStatusListener.TOPIC,
      object : SyncStatusListener {
        override fun syncStarted() {
          listener.onProjectReloadStart()
        }

        override fun syncFinished(canceled: Boolean) {
          ProjectViewUtil.expandTopLevel(project)
          listener.onProjectReloadFinish(
            if (canceled) {
              ExternalSystemRefreshStatus.CANCEL
            } else {
              ExternalSystemRefreshStatus.SUCCESS
            },
          )
        }
      },
    )
  }

  companion object {
    @JvmStatic
    fun initialize(workspace: BazelWorkspace) {
      val project = workspace.project
      val projectAware = BazelProjectAware(project)
      val projectTracker = ExternalSystemProjectTracker.getInstance(project)
      projectTracker.register(projectAware)
      projectTracker.activate(projectAware.projectId)
    }

    /**
     * Shows the "Sync Bazel changes" button.
     * After using this function, the button will not disappear automatically until the project is reloaded.
     */
    @JvmStatic
    fun notify(project: Project) {
      val projectTracker = ExternalSystemProjectTracker.getInstance(project)
      projectTracker.markDirty(getBazelProjectId(project.rootDir))
      projectTracker.scheduleChangeProcessing()
    }
  }
}

@RequiresReadLock
private fun calculateBazelConfigFiles(project: Project): Set<String> {
  val rootDir = project.bazelProjectProperties.rootDir
  val searchScope = GlobalSearchScope.projectScope(project)
  val projectView = project.bazelProjectSettings.projectViewPath
  val globalFiles = GLOBAL_CONFIG_FILES.map { rootDir?.findChild(it) }

  // getVirtualFilesByNames() function is private in FilenameIndex, so a mutable list is used as a workaround
  val localFiles = mutableListOf<VirtualFile>()
  // the search must be case-sensitive, otherwise it will be very expensive
  FilenameIndex.processFilesByNames(LOCAL_CONFIG_FILES, true, searchScope, null) {
    localFiles.add(it)
    true
  }

  return (globalFiles + localFiles + listOf(projectView)).mapNotNull { it?.path }.toSet()
}

private fun getBazelProjectId(projectPath: VirtualFile): ExternalSystemProjectId =
  ExternalSystemProjectId(BazelPluginConstants.SYSTEM_ID, projectPath.path)

// Bazel config files to look for only in the project root
private val GLOBAL_CONFIG_FILES: Array<String> = Constants.WORKSPACE_FILE_NAMES + Constants.BAZELISK_FILE_NAMES + Constants.BAZEL_RC_FILE_NAME

// Bazel config files to look for in all project directories
private val LOCAL_CONFIG_FILES: Set<String> = Constants.BUILD_FILE_NAMES.toSet()
