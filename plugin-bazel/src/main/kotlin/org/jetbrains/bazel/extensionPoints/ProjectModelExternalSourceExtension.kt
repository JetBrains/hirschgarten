package org.jetbrains.bazel.extensionPoints

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectModelExternalSource
import org.jetbrains.bazel.config.WithBuildToolId
import org.jetbrains.bazel.config.buildToolId
import org.jetbrains.bazel.config.withBuildToolId

interface ProjectModelExternalSourceExtension :
  ProjectModelExternalSource,
  WithBuildToolId {
  companion object {
    val ep: ExtensionPointName<ProjectModelExternalSourceExtension> =
      ExtensionPointName.create("org.jetbrains.bazel.projectModelExternalSource")
  }
}

val Project.projectModelExternalSource: ProjectModelExternalSource?
  get() =
    ProjectModelExternalSourceExtension.ep.withBuildToolId(buildToolId)
