package org.jetbrains.bazel.flow.exclude

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.openapi.vfs.newvfs.BulkFileListenerBackgroundable
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import org.jetbrains.bazel.commons.symlinks.BazelSymlinksCalculator
import org.jetbrains.bazel.config.bazelProjectName
import org.jetbrains.bazel.config.isBazelProject
import java.nio.file.Path

class BazelSymlinkExcludeFileListener : BulkFileListenerBackgroundable {
  override fun before(events: List<VFileEvent>) {
    events
      .asSequence()
      .filterIsInstance<VFileCreateEvent>()
      .filter { it.attributes?.isSymLink ?: false }
      .mapNotNull { it.toProjectAndPath() }
      .filter { BazelSymlinksCalculator.isBazelSymlink(it.project.bazelProjectName, it.path) }
      .groupBy({ it.project }, { it.path })
      .forEach { (project, paths) ->
        BazelSymlinkExcludeService.getInstance(project).addBazelSymlinksToExclude(paths.toSet())
      }
  }

  private fun VFileCreateEvent.toProjectAndPath(): ProjectAndPath? {
    val nioPath = path.toNioPathOrNull() ?: return null
    val project = findProjectByFilePath(nioPath) ?: return null
    return ProjectAndPath(project, nioPath)
  }

  private fun findProjectByFilePath(path: Path): Project? {
    return ProjectManager.getInstance().openProjects
      .filter { !it.isDisposed && it.isBazelProject }
      .firstOrNull { bazelProject ->
        // we can't use content roots, etc. because from the observations it turns out that
        // an event can be dispatched when a project doesn't have any modules or content roots.
        val projectBasePath = bazelProject.basePath?.toNioPathOrNull() ?: return@firstOrNull false
        path.startsWith(projectBasePath)
      }
  }

  private data class ProjectAndPath(val project: Project, val path: Path)
}
