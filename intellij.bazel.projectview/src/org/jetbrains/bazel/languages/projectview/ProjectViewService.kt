package org.jetbrains.bazel.languages.projectview

import com.intellij.openapi.project.Project
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
interface ProjectViewService {
  val allowExternalProjectViewModification: Boolean
  val projectViewState: StateFlow<ProjectView>

  suspend fun forceReparseCurrentProjectViewFiles()

  fun getProjectView(): ProjectView = projectViewState.value

  companion object {
    @JvmStatic
    fun getInstance(project: Project): ProjectViewService = project.getService(ProjectViewService::class.java)
  }
}
