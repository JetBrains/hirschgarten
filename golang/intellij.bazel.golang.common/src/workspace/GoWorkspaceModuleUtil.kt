package org.jetbrains.bazel.golang.workspace

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus

internal const val GO_WORKSPACE_MODULE_NAME = ".workspace"

@ApiStatus.Internal
object GoWorkspaceModuleUtil {
  fun findModule(project: Project): Module? = ModuleManager.getInstance(project).findModuleByName(GO_WORKSPACE_MODULE_NAME)
}
