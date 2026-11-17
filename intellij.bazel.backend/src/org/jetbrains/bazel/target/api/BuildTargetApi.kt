package org.jetbrains.bazel.target.api

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.sync.workspace.snapshot.workspaceSnapshotOrNull

/**
 * Place to expose things about targets to other plugins which depend on the Bazel plugin to avoid API changes after refactorings.
 */
@Service(Service.Level.PROJECT)
class BuildTargetApi(private val project: Project) {
  @Suppress("unused")
  fun getSyncedTargets(): List<BazelTargetDescriptor> =
    project.workspaceSnapshotOrNull?.targets?.allTargetInfos()?.toList().orEmpty()

  companion object {
    @JvmStatic
    fun getInstance(project: Project): BuildTargetApi = project.service()
  }
}
