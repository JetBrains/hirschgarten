package org.jetbrains.bazel.extensionPoints

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectModelExternalSource
import org.jetbrains.bazel.config.WithBuildToolId
import org.jetbrains.bazel.config.buildToolId
import org.jetbrains.bazel.config.withBuildToolId

interface BspProjectModelExternalSourceExtension :
  ProjectModelExternalSource,
  WithBuildToolId {
  companion object {
    val ep: ExtensionPointName<BspProjectModelExternalSourceExtension> =
      ExtensionPointName.create("org.jetbrains.bsp.bspProjectModelExternalSource")
  }
}

val Project.bspProjectModelExternalSource: ProjectModelExternalSource?
  get() =
    BspProjectModelExternalSourceExtension.ep.withBuildToolId(buildToolId)
