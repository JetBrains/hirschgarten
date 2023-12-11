package org.jetbrains.plugins.bsp.config

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.util.messages.Topic

@Service(Service.Level.PROJECT)
public class BspWorkspace(public val project: Project) : Disposable {
  private var initialized = false
  private val bspWorkspaceWatcher = BspWorkspaceWatcher(project)

  @Synchronized
  public fun initialize() {
    if (!initialized) {
      BspProjectAware.initialize(this)
      bspWorkspaceWatcher.listenForConfigChanges()
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
internal class BspSyncStatusService(private val project: Project) {
  private var isCanceled = false

  @Synchronized
  fun startSync() {
    isCanceled = false
    project.messageBus.syncPublisher(BspWorkspaceListener.TOPIC).syncStarted()
  }

  @Synchronized
  fun finishSync() {
    project.messageBus.syncPublisher(BspWorkspaceListener.TOPIC).syncFinished(isCanceled)
  }

  @Synchronized
  fun cancel() {
    isCanceled = true
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project): BspSyncStatusService =
      project.getService(BspSyncStatusService::class.java)
  }
}

public class BspWorkspaceWatcher(private val project: Project) {
  public fun listenForConfigChanges() {
    project.messageBus.connect().subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
      override fun after(events: MutableList<out VFileEvent>) {
        if (shouldNotifyOnEvents(events)) BspProjectAware.notify(project)
      }
    })
  }

  private fun shouldNotifyOnEvents(events: List<VFileEvent>): Boolean {
    val projectAwareExtension = BspProjectAwareExtension.ep.extensionList.firstOrNull() ?: return false
    val projectFileIndex = ProjectFileIndex.getInstance(project)
    return events.any {
      it.file?.let { vFile ->
        projectFileIndex.isInContent(vFile) &&
          (vFile.name in projectAwareExtension.eligibleConfigFileNames ||
            projectAwareExtension.eligibleConfigFileExtensions.contains(vFile.extension))
      } ?: false
    }
  }
}

public interface BspWorkspaceListener {
  public fun syncStarted()
  public fun syncFinished(canceled: Boolean)

  public companion object {
    public val TOPIC: Topic<BspWorkspaceListener> = Topic(BspWorkspaceListener::class.java)
  }
}
