package org.jetbrains.bazel.workspace

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.bazel.utils.isSourceFile
import org.jetbrains.bazel.workspace.fileEvents.BazelFileEventListener
import org.jetbrains.bazel.workspace.fileEvents.SimplifiedFileEvent

/**
 * Simple helper that lets external code enqueue a collection of source files for incremental Bazel processing.
 * Files are submitted through the same queue used by [BazelFileEventListener] for normal VFS events.
 */
class AssignFileToModuleFileEventSubmitter {
  fun submitFiles(project: Project, files: Collection<VirtualFile>) {
    val events = files
      .filter { !it.isDirectory && it.isSourceFile() }
      .map { SimplifiedFileEvent.ExternalCreate(it) }
    if (events.isNotEmpty()) {
      BazelFileEventListener.enqueueExternalEvents(project, events)
    }
  }

  companion object {
    @JvmStatic
    fun submit(project: Project, files: Collection<VirtualFile>) {
      AssignFileToModuleFileEventSubmitter().submitFiles(project, files)
    }
  }
}
