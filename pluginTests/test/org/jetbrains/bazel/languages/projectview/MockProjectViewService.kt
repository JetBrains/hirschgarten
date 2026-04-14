package org.jetbrains.bazel.languages.projectview

import com.intellij.openapi.application.readAction
import com.intellij.openapi.progress.runBlockingMaybeCancellable
import com.intellij.openapi.project.Project

internal class MockProjectViewService(private val project: Project, private val projectViewContent: String) : ProjectViewService {
  override val allowExternalProjectViewModification: Boolean
    get() = true
  override fun getProjectView(): ProjectView = runBlockingMaybeCancellable {
    readAction {
      ProjectView.fromProjectViewContent(project, projectViewContent)
    }
  }

  override suspend fun forceReparseCurrentProjectViewFiles() {}
}
