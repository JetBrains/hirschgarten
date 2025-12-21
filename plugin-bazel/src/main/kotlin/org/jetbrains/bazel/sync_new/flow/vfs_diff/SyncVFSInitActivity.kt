package org.jetbrains.bazel.sync_new.flow.vfs_diff

import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import org.jetbrains.bazel.sync_new.isNewSyncEnabled

class SyncVFSInitActivity : ProjectActivity {
  override suspend fun execute(project: Project) {
    if (!project.isNewSyncEnabled) {
      return
    }

    val service = project.serviceAsync<SyncVFSService>()
    val vfsState = service.vfsState.get()
    if (vfsState.listenState == SyncVFSListenState.LISTENING_VFS) {
      service.vfsListener.ensureAttached()
    }
  }
}
