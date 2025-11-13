package org.jetbrains.bazel.workspace

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * Simple helper that lets external code enqueue a collection of source files for incremental Bazel processing.
 */
class AssignFileToModuleFileEventSubmitter {
  fun submitFiles(project: Project, files: Collection<VirtualFile>) {
    files.forEach { AssignFileToModuleListener.enqueueExternalFile(project, it) }
  }

  companion object {
    @JvmStatic
    fun submit(project: Project, files: Collection<VirtualFile>) {
      AssignFileToModuleFileEventSubmitter().submitFiles(project, files)
    }
  }
}
