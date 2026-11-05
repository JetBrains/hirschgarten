package org.jetbrains.bazel.golang.workspace

import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.workspaceModel
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.storage.entities
import org.jetbrains.bazel.buildTask.TargetsToBuildProvider
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.workspacemodel.entities.BazelGoTargetEntity
import org.jetbrains.bazel.workspacemodel.entities.targetKey

internal class GoTargetsToBuildProvider : TargetsToBuildProvider {
  override fun getTargetsToBuild(project: Project, moduleEntity: ModuleEntity): List<Label> {
    if (moduleEntity.name != GO_WORKSPACE_MODULE_NAME) return emptyList()
    return project.workspaceModel.currentSnapshot.entities<BazelGoTargetEntity>().map { it.targetKey.label }.toList()
  }
}
