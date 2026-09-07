package org.jetbrains.bazel.clion.workspace

import com.intellij.openapi.project.Project
import com.intellij.platform.workspace.jps.entities.ModuleId
import com.jetbrains.cidr.CidrWorkspaceSettings
import com.jetbrains.cidr.lang.workspace.model.OCWorkspaceStorageCustomizer

internal class CcWorkspaceStorageCustomizer : OCWorkspaceStorageCustomizer {

  override fun getModuleId(project: Project): ModuleId? = findCcWorkspaceModuleId(project)

  override fun isResolveConfigurationEntityEnabled(project: Project): Boolean {
    return CidrWorkspaceSettings.isOCResolveConfigurationEntityEnabled()
  }
}
