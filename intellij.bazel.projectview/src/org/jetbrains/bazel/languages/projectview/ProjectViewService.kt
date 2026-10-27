package org.jetbrains.bazel.languages.projectview

import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.projectview.checker.ProjectViewProblem
import java.nio.file.Path

@ApiStatus.NonExtendable
interface ProjectViewService {

  val projectView: ProjectView

  val projectViewPath: Path?

  suspend fun forceReparseCurrentProjectViewFiles() {
    forceReparseCurrentProjectViewFiles(onEachProblem = null)
  }

  @ApiStatus.Internal
  suspend fun forceReparseCurrentProjectViewFiles(onEachProblem: ((ProjectViewProblem) -> Unit)?)

  companion object {
    @JvmStatic
    fun getInstance(project: Project): ProjectViewService = project.getService(ProjectViewService::class.java)
  }
}

@ApiStatus.Internal
fun Project.projectView(): ProjectView = ProjectViewService.getInstance(this).projectView

@ApiStatus.Internal
fun Project.projectViewPath(): Path? = ProjectViewService.getInstance(this).projectViewPath
