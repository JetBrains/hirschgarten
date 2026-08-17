package org.jetbrains.bazel.projectAware

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ex.ApplicationUtil
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectAware
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectId
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectListener
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectReloadContext
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectTracker
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemRefreshStatus
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemSettingsFilesModificationContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vcs.BranchChangeListener
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileCopyEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.ui.tree.TreeVisitor
import com.intellij.util.concurrency.annotations.RequiresReadLock
import com.intellij.util.ui.tree.TreeUtil
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.project.projectViewFile
import org.jetbrains.bazel.sync.SyncCache
import org.jetbrains.bazel.sync.status.SyncStatusListener
import org.jetbrains.bazel.sync.ProjectSyncScope
import org.jetbrains.bazel.sync.ProjectSyncService
import org.jetbrains.bazel.ui.status.BazelFileStatusRefresher

@Service(Service.Level.PROJECT)
@ApiStatus.Internal
class BazelWorkspace(val project: Project) :
  ExternalSystemProjectAware,
  Disposable {
  private var initialized = false

  @Volatile
  private var disposed = false

  // computed eagerly: the tracker's remove callback reads this during disposal, when service access already throws
  override val projectId: ExternalSystemProjectId = getBazelProjectId(project.rootDir)

  @VisibleForTesting
  val cachedBazelFiles =
    SyncCache.SyncCacheComputable {
      // if a write action collides with this read action, a cancellation exception will be thrown and the value will not be cached
      ApplicationUtil.tryRunReadAction(Computable { calculateBazelConfigFiles(project) })
    }

  override val settingsFiles: Set<String>
    get() = SyncCache.getInstance(project).get(cachedBazelFiles)

  /**
   * Project disposal runs inside a write action, so performing the registration under a read action
   * guarantees it cannot interleave with the disposal of this workspace or of the auto-import tracker (BAZEL-3453).
   */
  suspend fun initialize() {
    readAction {
      if (initialized || disposed || project.isDisposed) return@readAction
      val projectTracker = ExternalSystemProjectTracker.getInstance(project)
      projectTracker.register(this, parentDisposable = this)
      projectTracker.activate(projectId)
      BspExternalServicesSubscriber(project).subscribe(this)
      initialized = true
    }
  }

  override fun reloadProject(context: ExternalSystemProjectReloadContext) {
    if (context.isExplicitReload) {
      BazelCoroutineService.getInstance(project).start {
        project.service<ProjectSyncService>().sync(ProjectSyncScope.Full(build = false, phased = false))
      }
    }
  }

  override fun isIgnoredSettingsFileEvent(path: String, context: ExternalSystemSettingsFilesModificationContext): Boolean {
    // We use FilenameIndex, which just means "files that have been loaded into VFS", so that means
    // it's random whether non-indexable files outside the loaded .bazelproject will be in it.
    // Don't mark the project as needing reload just because a new file appeared in FilenameIndex, but still respect UPDATE events.
    return context.event == ExternalSystemSettingsFilesModificationContext.Event.CREATE
  }

  override fun subscribe(listener: ExternalSystemProjectListener, parentDisposable: Disposable) {
    project.messageBus.connect(parentDisposable).subscribe(
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

  override fun dispose() {
    disposed = true
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project): BazelWorkspace = project.getService(BazelWorkspace::class.java)

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

internal class BspExternalServicesSubscriber(private val project: Project) {
  fun subscribe(parentDisposable: Disposable) {
    subscribeForConfigChanges(parentDisposable)
    subscribeForBranchChanges(parentDisposable)
  }

  fun subscribeForConfigChanges(parentDisposable: Disposable) {
    project.messageBus.connect(parentDisposable).subscribe(
      VirtualFileManager.VFS_CHANGES,
      object : BulkFileListener {
        override fun after(events: MutableList<out VFileEvent>) {
          if (shouldRefreshProjectView(events)) {
            BazelFileStatusRefresher.getInstance(project).refreshAllFilesPresentation()
          }
        }
      },
    )
  }

  private fun shouldRefreshProjectView(events: List<VFileEvent>): Boolean =
    events.any {
      it is VFileCreateEvent ||
        it is VFileCopyEvent ||
        it is VFileMoveEvent
    }

  private fun subscribeForBranchChanges(parentDisposable: Disposable) {
    project.messageBus.connect(parentDisposable).subscribe(
      BranchChangeListener.VCS_BRANCH_CHANGED,
      object : BranchChangeListener {
        override fun branchWillChange(branchName: String) {}

        override fun branchHasChanged(branchName: String) {
          // automatically expand the project view tree's root node on branch change event
          // TODO: remove this workaround once this bug is fixed from the platform side
          // https://youtrack.jetbrains.com/issue/IJPL-160019/Restore-workspace-when-switching-branches-collapses-root-in-project-tree-view-when-switching-between-local-branches
          ProjectViewUtil.expandTopLevel(project)
        }
      },
    )
  }
}

internal object ProjectViewUtil {
  fun expandTopLevel(project: Project) {
    val projectView = ProjectView.getInstance(project)

    // this visitor will traverse first the main project root, which has 2 elements in its path,
    // therefore, we stop right after it finishes visiting this project root
    val rootNoteExpandVisitor =
      TreeVisitor { treePath ->
        if (treePath.pathCount == 1) {
          TreeVisitor.Action.CONTINUE
        } else {
          TreeVisitor.Action.INTERRUPT
        }
      }
    val pane = projectView.currentProjectViewPane ?: return
    TreeUtil.expand(pane.tree, rootNoteExpandVisitor) {}
  }
}

@RequiresReadLock
private fun calculateBazelConfigFiles(project: Project): Set<String> {
  val rootDir = project.rootDir
  val searchScope = GlobalSearchScope.projectScope(project)
  val projectView = project.projectViewFile
  val globalFiles = GLOBAL_CONFIG_FILES.map { rootDir.findChild(it) }

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
