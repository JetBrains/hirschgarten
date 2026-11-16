package org.jetbrains.bazel.clion.workspace

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.cidr.lang.workspace.OCResolveConfiguration
import com.jetbrains.cidr.lang.workspace.OCResolveConfigurationChooser
import com.jetbrains.cidr.lang.workspace.OCResolveConfigurationSlice
import org.jetbrains.bazel.config.isBazelProject

/** Selects the resolve configuration that has a configuration of the build of the last debug session. */
internal class CcDebugSessionChooser : OCResolveConfigurationChooser {
  override fun selectPreselectedResolveConfiguration(
    project: Project,
    slice: OCResolveConfigurationSlice,
    file: VirtualFile?,
  ): OCResolveConfiguration? {
    if (!project.isBazelProject) return null

    val configurations = CcDebugSessionConfigurations.getInstance(project).configurations
    if (configurations.isEmpty()) return null

    val candidates = slice.resolve(project)
    return configurations.firstNotNullOfOrNull { configuration ->
      candidates.firstOrNull { it.getCcIdentifier()?.configurationId == configuration }
    }
  }
}
