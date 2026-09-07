package org.jetbrains.bazel.clion.workspace

import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.workspaceModel
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleId
import com.intellij.platform.workspace.jps.entities.ModuleSourceDependency
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.MutableEntityStorage

internal const val CC_WORKSPACE_MODULE_NAME: String = ".cc-workspace"

internal fun addCcWorkspaceModule(builder: MutableEntityStorage, entitySource: EntitySource) {
  builder.addEntity(
    ModuleEntity(
      name = CC_WORKSPACE_MODULE_NAME,
      dependencies = listOf(ModuleSourceDependency),
      entitySource = entitySource,
    )
  )
}

/** `null` before the sync applies the project model, and for a project that Bazel did not import. */
internal fun findCcWorkspaceModuleId(project: Project): ModuleId? {
  val id = ModuleId(CC_WORKSPACE_MODULE_NAME)
  return if (project.workspaceModel.currentSnapshot.resolve(id) != null) id else null
}
