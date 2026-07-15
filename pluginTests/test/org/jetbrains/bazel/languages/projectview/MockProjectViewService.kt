package org.jetbrains.bazel.languages.projectview

import com.intellij.openapi.application.runReadActionBlocking
import com.intellij.openapi.project.Project
import com.intellij.project.stateStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.bazel.flow.open.BazelProjectStoreDescriptor
import java.nio.file.Path

internal class MockProjectViewService(private val project: Project, private val projectViewContent: String) : ProjectViewService {
  override val projectViewState: StateFlow<ProjectView>
    get() = MutableStateFlow(
      runReadActionBlocking {
        ProjectViewFactory.fromProjectViewContent(project, projectViewContent)
      },
    )

  override suspend fun forceReparseCurrentProjectViewFiles() {}

  override val projectViewPath: Path?
    get() = (project.stateStore.storeDescriptor as? BazelProjectStoreDescriptor)?.projectViewFile
}
