package org.jetbrains.bazel.workspacemodel.entities

import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.toVirtualFileUrl
import com.intellij.platform.backend.workspace.workspaceModel
import org.jetbrains.bazel.config.rootDir

object BazelProjectDirectoriesEntityFixtures {
  fun emptyBazelDirectoryWorkspaceEntity(project: Project): BazelProjectDirectoriesEntityBuilder {
    val workspaceModel = project.workspaceModel
    return BazelProjectDirectoriesEntity(
      projectRoot = project.rootDir.toVirtualFileUrl(workspaceModel.getVirtualFileUrlManager()),
      includedRoots = emptyList(),
      excludedRoots = emptyList(),
      indexAllFilesInIncludedRoots = false,
      indexAdditionalFiles = emptyList(),
      entitySource = BazelProjectEntitySource,
    )
  }
}
