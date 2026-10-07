package org.jetbrains.bazel.clion.workspace

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.cidr.lang.workspace.OCFollowSymlinkRootsProvider
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.sync.workspace.mapper.normal.HARDLINKS_DIR_NAME
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceSnapshotService
import java.nio.file.Path

internal class CcFollowSymlinkRootsProvider : OCFollowSymlinkRootsProvider {
  override fun getRoots(project: Project): Collection<Path> {
    if (!project.isBazelProject) return emptyList()
    val snapshot = project.service<WorkspaceSnapshotService>().snapshot.value
    val outputBase = snapshot.bazelInfo.outputBase

    return listOf(outputBase.resolve(HARDLINKS_DIR_NAME))
  }
}
