package org.jetbrains.plugins.bsp.impl.projectAware

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vcs.BranchChangeListener
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileCopyEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent
import com.intellij.ui.tree.TreeVisitor
import com.intellij.util.messages.Topic
import com.intellij.util.ui.tree.TreeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.kotlin.tooling.core.Interner
import org.jetbrains.kotlin.tooling.core.WeakInterner
import org.jetbrains.plugins.bsp.buildTask.BspProjectModuleBuildTasksTracker
import org.jetbrains.plugins.bsp.coroutines.BspCoroutineService
import javax.swing.tree.TreePath

@Service(Service.Level.PROJECT)
public class BspWorkspace(public val project: Project) : Disposable {
  private var initialized = false
  public val interner: Interner = WeakInterner()

  @Synchronized
  public fun initialize() {
    if (!initialized) {
      BspProjectAware.initialize(this)
      BspProjectModuleBuildTasksTracker.initialize(this)
      BspExternalServicesSubscriber(project).subscribe()
      initialized = true
    }
  }

  public companion object {
    @JvmStatic
    public fun getInstance(project: Project): BspWorkspace = project.getService(BspWorkspace::class.java)
  }

  override fun dispose() {}
}

@Service(Service.Level.PROJECT)
public class BspSyncStatusService(private val project: Project) {
  private var isCanceled = false

  private var _isSyncInProgress: Boolean = false

  val isSyncInProgress: Boolean
    @Synchronized get() = _isSyncInProgress

  @Synchronized
  fun startSync() {
    if (_isSyncInProgress) throw SyncAlreadyInProgressException()
    isCanceled = false
    _isSyncInProgress = true
    project.messageBus.syncPublisher(BspWorkspaceListener.TOPIC).syncStarted()
  }

  @Synchronized
  fun finishSync() {
    _isSyncInProgress = false
    project.messageBus.syncPublisher(BspWorkspaceListener.TOPIC).syncFinished(isCanceled)
  }

  @Synchronized
  fun cancel() {
    isCanceled = true
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project): BspSyncStatusService = project.getService(BspSyncStatusService::class.java)
  }
}

class SyncAlreadyInProgressException : IllegalStateException()

public class BspExternalServicesSubscriber(private val project: Project) {
  public fun subscribe() {
    subscribeForConfigChanges()
    subscribeForBranchChanges()
  }

  public fun subscribeForConfigChanges() {
    project.messageBus.connect().subscribe(
      VirtualFileManager.VFS_CHANGES,
      object : BulkFileListener {
        override fun after(events: MutableList<out VFileEvent>) {
          if (shouldNotifyOnEvents(events)) BspProjectAware.notify(project)
          if (shouldRefreshProjectView(events)) {
            BspCoroutineService.getInstance(project).start {
              withContext(Dispatchers.EDT) {
                ProjectView.getInstance(project).refresh()
              }
            }
          }
        }
      },
    )
  }

  private fun shouldNotifyOnEvents(events: List<VFileEvent>): Boolean {
    val projectAwareExtension = BspProjectAwareExtension.ep.extensionList.firstOrNull() ?: return false
    val projectFileIndex = ProjectFileIndex.getInstance(project)
    return events.any {
      it.file?.let { vFile ->
        projectFileIndex.isInContent(vFile) &&
          (
            vFile.name in projectAwareExtension.eligibleConfigFileNames ||
              projectAwareExtension.eligibleConfigFileExtensions.contains(vFile.extension)
          )
      } == true
    }
  }

  private fun shouldRefreshProjectView(events: List<VFileEvent>): Boolean =
    events.any {
      it is VFileCreateEvent ||
        it is VFileCopyEvent ||
        it is VFileMoveEvent
    }

  private fun subscribeForBranchChanges() {
    val projectView = ProjectView.getInstance(project)

    // this visitor will traverse first the main project root, which has 2 elements in its path,
    // therefore, we stop right after it finishes visiting this project root
    val rootNoteExpandVisitor =
      object : TreeVisitor {
        override fun visit(treePath: TreePath): TreeVisitor.Action =
          if (treePath.pathCount == 1) {
            TreeVisitor.Action.CONTINUE
          } else {
            TreeVisitor.Action.INTERRUPT
          }
      }

    project.messageBus.connect().subscribe(
      BranchChangeListener.VCS_BRANCH_CHANGED,
      object : BranchChangeListener {
        override fun branchWillChange(branchName: String) {}

        override fun branchHasChanged(branchName: String) {
          // automatically expand the project view tree's root node on branch change event
          // TODO: remove this workaround once this bug is fixed from the platform side
          // https://youtrack.jetbrains.com/issue/IJPL-160019/Restore-workspace-when-switching-branches-collapses-root-in-project-tree-view-when-switching-between-local-branches
          val pane = projectView.currentProjectViewPane ?: return
          TreeUtil.expand(pane.tree, rootNoteExpandVisitor) {}
        }
      },
    )
  }
}

public interface BspWorkspaceListener {
  public fun syncStarted()

  public fun syncFinished(canceled: Boolean)

  public fun allTasksCancelled() {}

  public companion object {
    public val TOPIC: Topic<BspWorkspaceListener> = Topic(BspWorkspaceListener::class.java)
  }
}
