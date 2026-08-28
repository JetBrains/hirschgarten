package org.jetbrains.bazel.projectAware

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.Disposable
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
import com.intellij.openapi.vcs.BranchChangeListener
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.tree.TreeVisitor
import com.intellij.util.ui.tree.TreeUtil
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.sync.ProjectSyncScope
import org.jetbrains.bazel.sync.ProjectSyncService
import org.jetbrains.bazel.sync.status.SyncStatusListener

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

  override val settingsFiles: Set<String>
    get() = emptySet()

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

  /**
   * Tells the platform to ignore every settings file event.
   * The plugin tracks the Bazel files itself. See [org.jetbrains.bazel.workspace.fileEvents.BazelFileEventProcessor].
   */
  override fun isIgnoredSettingsFileEvent(path: String, context: ExternalSystemSettingsFilesModificationContext): Boolean =
    true

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
  }
}

internal class BspExternalServicesSubscriber(private val project: Project) {
  fun subscribe(parentDisposable: Disposable) {
    subscribeForBranchChanges(parentDisposable)
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

private fun getBazelProjectId(projectPath: VirtualFile): ExternalSystemProjectId =
  ExternalSystemProjectId(BazelPluginConstants.SYSTEM_ID, projectPath.path)
