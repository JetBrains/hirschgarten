package org.jetbrains.bazel.flow.exclude

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.symlinks.BazelSymlinksCalculator
import org.jetbrains.bazel.config.bazelProjectName
import org.jetbrains.bazel.config.isBazelProject
import java.nio.file.Path

@ApiStatus.Internal
class BazelSymlinkExcludeFileListener : BulkFileListener {
  override fun before(events: List<VFileEvent>) {
    events
      .asSequence()
      .filterIsInstance<VFileCreateEvent>()
      .filter { it.attributes?.isSymLink ?: false }
      .flatMap { it.toListOfProjectAndPath() }
      .filter { BazelSymlinksCalculator.isBazelSymlink(it.project.bazelProjectName, it.path) }
      .groupBy({ it.project }, { it.path })
      .forEach { (project, paths) ->
        BazelSymlinkExcludeService.getInstance(project).addBazelSymlinksToExclude(paths.toSet())
      }
  }

  private fun VFileCreateEvent.toListOfProjectAndPath(): List<ProjectAndPath> {
    val nioPath = path.toNioPathOrNull() ?: return emptyList()
    val projects = findProjectsByFilePath(nioPath)
    return projects.map { ProjectAndPath(it, nioPath) }
  }

  private fun findProjectsByFilePath(path: Path): List<Project> =
    ProjectManager.getInstance().openProjects
      .filter { project ->
        if (project.isDisposed || !project.isBazelProject) {
          return@filter false
        }
        // we can't use content roots, etc. because from the observations it turns out that
        // an event can be dispatched when a project doesn't have any modules or content roots.
        val projectBasePath = project.basePath?.toNioPathOrNull() ?: return@filter false
        path.startsWith(projectBasePath)
      }

  private data class ProjectAndPath(val project: Project, val path: Path)
}
