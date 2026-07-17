package org.jetbrains.bazel.workspace.fileEvents

import com.intellij.openapi.components.service
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.newvfs.BulkFileListenerBackgroundable
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.coroutines.BazelCoroutineService

@ApiStatus.Internal
class BazelFileEventListener : BulkFileListenerBackgroundable {
  override fun after(events: MutableList<out VFileEvent>) {
    val projects =
      ProjectManager
        .getInstance()
        .openProjects // ProjectLocator::getProjectsForFile won't work, since it only recognizes files already added to content roots
        .filter { it.isBazelProject }
    for (project in projects) {
      BazelCoroutineService.getInstance(project).start {
        BazelFileEventProcessor.getInstance(project).enqueue(events)
      }
    }
  }
}
