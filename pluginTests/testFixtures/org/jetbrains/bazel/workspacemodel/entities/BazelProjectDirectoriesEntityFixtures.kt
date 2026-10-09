package org.jetbrains.bazel.workspacemodel.entities

import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.storeAndGet
import com.intellij.platform.backend.workspace.workspaceModel
import org.jetbrains.bazel.config.rootDir

object BazelProjectDirectoriesEntityFixtures {
  fun emptyBazelDirectoryWorkspaceEntity(project: Project): BazelProjectDirectoriesEntityBuilder {
    val workspaceModel = project.workspaceModel
    return BazelProjectDirectoriesEntity(
      projectRoot = workspaceModel.getVirtualFileUrlManager().storeAndGet(project.rootDir),
      includedRoots = emptyList(),
      excludedRoots = emptyList(),
      indexPatterns = emptyList(),
      indexableRecursiveRoots = emptyList(),
      indexableNonRecursiveRoots = emptyList(),
      entitySource = BazelProjectEntitySource,
    )
  }
}
