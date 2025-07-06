package org.jetbrains.bazel.sync.projectStructure.legacy

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleId

internal const val WORKSPACE_MODULE_NAME = ".workspace"

/**
 * This umbrella module is inspired from the OG's, this supports language plugins that work effectively only under one module
 */
internal object WorkspaceModuleUtils {
  fun findModuleEntity(project: Project): ModuleEntity? =
    WorkspaceModel.getInstance(project).currentSnapshot.resolve(ModuleId(WORKSPACE_MODULE_NAME))

  fun findModule(project: Project): Module? = ModuleManager.getInstance(project).findModuleByName(WORKSPACE_MODULE_NAME)
}
