package org.jetbrains.bazel.languages.projectview

import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus
import java.nio.file.Path

@ApiStatus.NonExtendable
interface ProjectViewService {

  val projectView: ProjectView

  val projectViewPath: Path?

  suspend fun forceReparseCurrentProjectViewFiles()

  companion object {
    @JvmStatic
    fun getInstance(project: Project): ProjectViewService = project.getService(ProjectViewService::class.java)
  }
}

@ApiStatus.Internal
fun Project.projectView(): ProjectView = ProjectViewService.getInstance(this).projectView

@ApiStatus.Internal
fun Project.projectViewPath(): Path? = ProjectViewService.getInstance(this).projectViewPath
