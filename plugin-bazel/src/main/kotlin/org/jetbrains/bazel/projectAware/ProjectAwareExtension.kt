package org.jetbrains.bazel.projectAware

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectId
import com.intellij.openapi.vfs.VirtualFile

interface ProjectAwareExtension {
  fun getProjectId(projectPath: VirtualFile): ExternalSystemProjectId

  val eligibleConfigFileNames: List<String>
  val eligibleConfigFileExtensions: List<String>

  companion object {
    val ep: ExtensionPointName<ProjectAwareExtension> = ExtensionPointName.create("org.jetbrains.bazel.projectAwareExtension")
  }
}
