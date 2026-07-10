package org.jetbrains.bazel.languages.projectview

import com.intellij.openapi.project.Project
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.annotations.ApiStatus
import java.nio.file.Path

interface ProjectViewService {
  @get:ApiStatus.Internal
  val projectViewState: StateFlow<ProjectView>

  suspend fun forceReparseCurrentProjectViewFiles()

  val projectView: ProjectView
    get() = projectViewState.value

  val projectViewPath: Path?

  companion object {
    @JvmStatic
    fun getInstance(project: Project): ProjectViewService = project.getService(ProjectViewService::class.java)
  }
}

@ApiStatus.Internal
fun Project.projectView(): ProjectView = ProjectViewService.getInstance(this).projectView
