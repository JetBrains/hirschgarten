package org.jetbrains.bazel.clion.workspace

import com.intellij.openapi.project.Project
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import org.jetbrains.bazel.buildTask.TargetsToBuildProvider
import org.jetbrains.bazel.clion.sync.CcBuildTarget
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.snapshot.hasBuildData
import org.jetbrains.bazel.sync.workspace.snapshot.workspaceSnapshotOrNull

/** Builds the C/C++ targets that got a resolve configuration, because the C/C++ import has one module for all of them. */
internal class CcTargetsToBuildProvider : TargetsToBuildProvider {
  override fun getTargetsToBuild(project: Project, moduleEntity: ModuleEntity): List<Label> {
    if (moduleEntity.name != CC_WORKSPACE_MODULE_NAME) return emptyList()
    val snapshot = project.workspaceSnapshotOrNull ?: return emptyList()
    return snapshot.importedTargets()
      .filter { it.hasBuildData<CcBuildTarget>() }
      .map { it.label }
      .distinct()
      .toList()
  }
}
