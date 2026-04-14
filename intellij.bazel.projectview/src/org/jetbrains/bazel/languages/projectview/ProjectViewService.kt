package org.jetbrains.bazel.languages.projectview

import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
interface ProjectViewService {
  val allowExternalProjectViewModification: Boolean

  fun getProjectView(): ProjectView
  suspend fun forceReparseCurrentProjectViewFiles()

  companion object {
    @JvmStatic
    fun getInstance(project: Project): ProjectViewService = project.getService(ProjectViewService::class.java)
  }
}

@ApiStatus.Internal
fun Project.projectView(): ProjectView = ProjectViewService.getInstance(this).getProjectView()
