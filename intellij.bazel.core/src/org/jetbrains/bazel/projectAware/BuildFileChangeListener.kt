package org.jetbrains.bazel.projectAware

import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.config.isBazelProject

/**
 * Lightweight listener that records BUILD file changes so that [BazelProjectAware.reloadProject]
 * can determine which targets are affected and trigger a partial sync instead of a full sync.
 */
@ApiStatus.Internal
class BuildFileChangeListener : BulkFileListener {
  override fun after(events: MutableList<out VFileEvent>) {
    val buildFileEvents = events.filter { event ->
      val fileName = when (event) {
        is VFileContentChangeEvent -> event.file.name
        is VFileCreateEvent -> event.childName
        else -> event.file?.name
      }
      fileName != null && fileName in BUILD_FILE_NAMES
    }
    if (buildFileEvents.isEmpty()) return

    val projects = ProjectManager.getInstance().openProjects.filter { it.isBazelProject }
    for (project in projects) {
      val tracker = ChangedBuildFilesTracker.getInstance(project)
      for (event in buildFileEvents) {
        val path = event.file?.toNioPath() ?: continue
        tracker.addChangedFile(path)
      }
    }
  }
}

private val BUILD_FILE_NAMES: Set<String> = Constants.BUILD_FILE_NAMES.toSet()
