package org.jetbrains.bazel.sync.projectStructure.legacy

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleId

internal const val MONO_MODULE_NAME = ".workspace"

internal object MonoModuleUtils {
  fun findModuleEntity(project: Project): ModuleEntity? =
    WorkspaceModel.getInstance(project).currentSnapshot.resolve(ModuleId(MONO_MODULE_NAME))

  fun findModule(project: Project): Module? = ModuleManager.getInstance(project).findModuleByName(MONO_MODULE_NAME)
}
