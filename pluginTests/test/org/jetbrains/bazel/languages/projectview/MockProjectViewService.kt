package org.jetbrains.bazel.languages.projectview

import com.intellij.openapi.application.runReadActionBlocking
import com.intellij.openapi.project.Project
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class MockProjectViewService(private val project: Project, private val projectViewContent: String) : ProjectViewService {
  override val allowExternalProjectViewModification: Boolean
    get() = true
  override val projectViewState: StateFlow<ProjectView>
    get() = MutableStateFlow(
      runReadActionBlocking {
        ProjectView.fromProjectViewContent(project, projectViewContent)
      },
    )

  override suspend fun forceReparseCurrentProjectViewFiles() {}
}
